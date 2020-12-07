package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._
import org.multics.baueran.frep._
import shared.Defs._
import shared.{CaseRubric, Caze, FIle, MyDate}
import backend.db.db.DBContext

class Post @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {

  private val Logger = play.api.Logger(this.getClass)

  def login() = Action { implicit request =>
    val inputEmail: String = request.body.asFormUrlEncoded.get("inputEmail").head
    val inputPassword: String = request.body.asFormUrlEncoded.get("inputPassword").head
    val member = getRegisteredMemberForPasswordAndEmail(inputPassword, inputEmail)
    val errorMessage =
      s"User login failed. Go back to ${serverUrl(request)} and try again! " +
        s"But after 3 failed attempts your IP is going to be temporarily blocked."

    // TODO: Remove me!
    println("some random hash: " + getRandomHash(inputPassword))

    member match {
      case Some(m) =>
        m.hash.split(":") match {
          case Array(_, salt, _) =>
            val cookieCreationDate = new MyDate()
            memberDao.setLastSeen(m.member_id, cookieCreationDate)
            memberDao.increaseLoginCounter(m.member_id)

            Redirect(s"${serverUrl(request)}/assets/html/private/index.html")
              .withCookies(
                Cookie(CookieFields.salt.toString, salt, httpOnly = false),
                Cookie(CookieFields.id.toString, m.member_id.toString, httpOnly = false),
                Cookie(CookieFields.email.toString, m.email, httpOnly = false),
                Cookie(CookieFields.creationDate.toString, cookieCreationDate.toString(), httpOnly = false)
              )
          case _ =>
            BadRequest(errorMessage)
            // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, errorMessage))
        }
      case None =>
        BadRequest(views.html.defaultpages.badRequest("POST", request.uri, errorMessage))
    }
  }

  /**
    * @return Ok(case ID) of stored case as it is in the DB,
    *         or BadRequest(error string) if something went wrong.
    */

  def saveCaze() = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts
        (requestData("fileId"), requestData("case").toList) match {
          case (Seq(fileId), cazeJson :: Nil) => {
            Caze.decode(cazeJson.toString) match {
              case Some(caze) =>
                var newCaseId = caze.id

                isUserAuthorized(request, caze.member_id) match {
                  case Left(err) =>
                    Logger.error(s"Post: saveCaze() failed: not authorised: $err")
                    Unauthorized // (views.html.defaultpages.unauthorized())
                  case Right(_) =>
                    if (newCaseId < 0)
                      newCaseId = cazeDao.insert(caze)

                    if (fileDao.addCaseIdToFile(newCaseId, fileId.toInt))
                      Ok(newCaseId.toString)
                    else
                      BadRequest(<p>Hello!</p>)
                      // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, s"saveCaze() failed: failed to add case with new ID ${newCaseId} (old case id: ${caze.id}) to file with ID ${fileId}."))
                }
              case None =>
                BadRequest(<p>Hello!</p>)
                // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "saveCaze() failed: decoding of caze failed. Json wrong? " + cazeJson))
            }
          }
          case _ =>
            BadRequest(<p>Hello!</p>)
            // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "saveCaze() failed: no data received in request: " + requestData))
        }
      }
      case Left(err) =>
        BadRequest(<p>Hello!</p>)
        // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "saveCaze() failed: not authorised: " + err))
    }
  }

  def addCaseRubricsToCaze() = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("memberID"), requestData("caseID"), requestData("caserubrics")) match {
          case (Seq(memberIdStr), Seq(cazeIDStr), Seq(caserubricsJson)) if (cazeIDStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            (memberIdStr.toInt, cazeIDStr.toInt, CaseRubric.decodeList(caserubricsJson)) match {
              case (memberId, caseID, Some(caseRubrics)) =>
                isUserAuthorized(request, memberId) match {
                  case Left(err) =>
                    Logger.error(s"Post: addRubricsToCaze() failed: not authorised: $err")
                    Unauthorized // (views.html.defaultpages.unauthorized())
                  case Right(_) =>
                    if (cazeDao.addCaseRubrics(caseID, caseRubrics).length > 0) {
                      Logger.debug(s"Post: addCaseRubricsToCaze(): success")
                      Ok
                    }
                    else {
                      Logger.error(s"Post: addCaseRubricsToCaze() failed")
                      BadRequest(<p>Hello!</p>)
                      //BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "addCaseRubrics() failed"))
                    }
                }
              case _ =>
                Logger.error(s"Post: addCaseRubricsToCaze() failed: type conversion error which should never have happened")
                BadRequest(<p>Hello!</p>)
                // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "addCaseRubricsToCaze() failed: type conversion error which should never have happened"))
            }
          case _ => {
            Logger.error(s"Post: addCaseRubricsToCaze() failed: no or the wrong form data received.")
            BadRequest(<p>Hello!</p>)
            // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "addCaseRubricsToCaze() failed: no or the wrong form data received."))
          }
        }
      }
      case Left(err) =>
        BadRequest(<p>Hello!</p>)
        // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "addCaseRubrics() failed: " + err))
    }
  }

  def saveFile() = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Right(_) => {
        FIle.decode(request.body.asText.get) match {
          case Some(file) =>
            isUserAuthorized(request, file.member_id) match {
              case Left(err) =>
                Logger.error(s"Post: saveFile() failed: not authorised: $err")
                Unauthorized // (views.html.defaultpages.unauthorized())
              case Right(_) =>
                Ok(fileDao.insert(file).toString())
            }
          case None =>
            BadRequest(<p>Hello!</p>)
            // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "saveFile() failed: saving of file failed. Json wrong? " + request.body.asText.get))
        }
      }
      case Left(err) =>
        BadRequest(<p>Hello!</p>)
        // BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "saveFile() failed: " + err))
    }
  }

}
