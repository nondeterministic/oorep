package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._
import org.multics.baueran.frep._
import shared.Defs._
import backend.dao.{CazeDao, FileDao}
import backend.db.db.DBContext
import play.api.Logger
import shared.{CaseRubric, Caze, FIle}

import play.filters.csrf._
import play.filters.csrf.CSRF.Token

class Post @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {
  cazeDao = new CazeDao(dbContext)
  fileDao = new FileDao(dbContext)

  def login() = Action { implicit request =>
    val inputEmail: String = request.body.asFormUrlEncoded.get("inputEmail").head
    val inputPassword: String = request.body.asFormUrlEncoded.get("inputPassword").head

    val hashedPass = getHash(inputPassword)
    println("TODO: Remove me: " + hashedPass) // TODO

    memberDao.getFromEmail(inputEmail) match {
      case member :: _ if (hashedPass == member.hash) =>
        Redirect(serverUrl(request) + "/assets/html/private/index.html")
          .withCookies(
            Cookie(CookieFields.email.toString, inputEmail, httpOnly = false),
            Cookie(CookieFields.hash.toString, member.hash, httpOnly = false),
            Cookie(CookieFields.id.toString, member.member_id.toString, httpOnly = false)
          )
      case _ => BadRequest("login() failed: user not authorized to login.")
    }
  }

  /**
    * @return Ok(case ID) of stored case as it is in the DB,
    *         or BadRequest(error string) if something went wrong.
    */

  def saveCaze() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts
        (requestData("fileId"), requestData("case").toList) match {
          case (Seq(fileId), cazeJson :: Nil) => {
            Caze.decode(cazeJson.toString) match {
              case Some(caze) =>
                var newCaseId = caze.id
                if (newCaseId < 0)
                  newCaseId = cazeDao.insert(caze)

                if (fileDao.addCaseIdToFile(newCaseId, fileId.toInt))
                  Ok(newCaseId.toString)
                else
                  BadRequest(s"Post: saveCaze() failed: failed to add case with new ID ${newCaseId} (old case id: ${caze.id}) to file with ID ${fileId}.")

              case None =>
                BadRequest("Post: saveCaze() failed: decoding of caze failed. Json wrong? " + cazeJson)
            }
          }
          case _ => BadRequest("Post: saveCaze() failed: no data received in request: " + requestData)
        }
      }
      case Left(err) => BadRequest("Post: saveCaze() failed: not authorised: " + err)
    }
  }

  def addCaseRubricsToCaze() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("caseID"), requestData("caserubrics")) match {
          case (Seq(cazeIDStr), Seq(caserubricsJson)) if (cazeIDStr.forall(_.isDigit)) =>
            (cazeIDStr.toInt, CaseRubric.decodeList(caserubricsJson)) match {
              case (caseID, Some(caseRubrics)) =>
                if (cazeDao.addCaseRubrics(caseID, caseRubrics).length > 0) {
                  Logger.debug(s"Post: addCaseRubrics(): success")
                  Ok
                }
                else {
                  Logger.error(s"Post: addCaseRubrics(): failed")
                  BadRequest("addCaseRubrics() failed")
                }
            }
        }
      }
      case Left(err) => BadRequest("addCaseRubrics() failed: " + err)
    }
  }

  def delCaseRubricsFromCaze() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("caseID"), requestData("caserubrics")) match {
          case (Seq(cazeIDStr), Seq(caserubricsJson)) if (cazeIDStr.forall(_.isDigit)) =>
            (cazeIDStr.toInt, CaseRubric.decodeList(caserubricsJson)) match {
              case (caseID, Some(caseRubrics)) =>
                if (cazeDao.delCaseRubrics(caseID, caseRubrics) > 0) {
                  Logger.debug(s"Post: delCaseRubrics(): success")
                  Ok
                }
                else {
                  Logger.error(s"Post: delCaseRubrics(): failed")
                  BadRequest("delCaseRubrics() failed")
                }
            }
        }
      }
      case Left(err) => BadRequest("delCaseRubrics() failed: " + err)
    }
  }

  def updateCaseRubricsWeights() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("caseID"), requestData("caserubrics")) match {
          case (Seq(cazeIDStr), Seq(caserubricsJson)) if (cazeIDStr.forall(_.isDigit)) =>
            (cazeIDStr.toInt, CaseRubric.decodeList(caserubricsJson)) match {
              case (caseID, Some(caseRubrics)) =>
                if (cazeDao.updateCaseRubricsWeights(caseID, caseRubrics) > 0) {
                  Logger.debug(s"Post: updateCaseRubricsWeights(): success")
                  Ok
                }
                else {
                  Logger.error(s"Post: updateCaseRubricsWeights(): failed")
                  BadRequest("updateCaseRubricsWeights() failed")
                }
            }
        }
      }
      case Left(err) => BadRequest("updateCaseRubricsWeights() failed: " + err)
    }
  }

  def updateCaseDescription() = Action { implicit request =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("caseID"), requestData("casedescription")) match {
          case (Seq(cazeIDStr), Seq(casedescription)) if (cazeIDStr.forall(_.isDigit)) =>
            if (cazeDao.updateCaseDescription(cazeIDStr.toInt, casedescription) > 0) {
              Logger.debug(s"Post: updateCaseDescription(): success")
              Ok
            }
            else {
              Logger.error(s"Post: updateCaseDescription(): failed")
              BadRequest("updateCaseDescription() failed")
            }
        }
      }
      case Left(err) => BadRequest("updateCaseDescription() failed: " + err)
    }
  }

  def delCaze() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("caseId"), requestData("memberId")) match {
          case (Seq(caseIdStr), Seq(memberIdStr)) =>
            cazeDao.delete(caseIdStr.toInt)
            Ok
          case _ =>
            BadRequest("delCaze() failed")
        }
      }
      case Left(err) => BadRequest("delCaze() failed: " + err)
    }
  }

  def saveFile() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        FIle.decode(request.body.asText.get) match {
          case Some(file) =>
            Ok(fileDao.insert(file).toString())
          case None =>
            BadRequest("saveFile() failed: saving of file failed. Json wrong? " + request.body.asText.get)
        }
      }
      case Left(err) => BadRequest("saveFile() failed: " + err)
    }
  }

  def delFileAndCases() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        request.body.asMultipartFormData.get.dataParts("fileId") match {
          case Seq(fileId) => {
            fileDao.delFileAndAllCases(fileId.toInt)
            Ok
          }
          case _ => BadRequest("delFile() failed: wrong data received.")
        }
      }
      case Left(err) => BadRequest("delFile() failed: " + err)
    }
  }

  def updateFileDescription() = Action { implicit request =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts
        (requestData("filedescr"), requestData("fileId")) match {
          case (Seq(filedescr), Seq(fileId)) if (fileId.forall(_.isDigit)) =>
            fileDao.changeDescription(fileId.toInt, filedescr)
            Ok
          case _ =>
            BadRequest("updateFileDescription() failed")
        }
      }
      case Left(err) => BadRequest("updateFileDescription() failed: " + err)
    }
  }

}
