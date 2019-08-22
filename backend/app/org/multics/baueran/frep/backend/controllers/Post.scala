package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._
import org.multics.baueran.frep._
import shared.Defs._
import backend.dao.{CazeDao, FileDao}
import backend.db.db.DBContext
import shared.{Caze, FIle}

class Post @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) {
  cazeDao = new CazeDao(dbContext)
  fileDao = new FileDao(dbContext)

  def login() = Action {
    request: Request[AnyContent] =>
      val inputEmail: String = request.body.asFormUrlEncoded.get("inputEmail").head
      val inputPassword: String = request.body.asFormUrlEncoded.get("inputPassword").head

      val hashedPass = getHash(inputPassword)
      println("TODO: Remove me: " + hashedPass) // TODO

      memberDao.getFromEmail(inputEmail) match {
        case member :: _ if (hashedPass == member.hash) =>
          Redirect(serverUrl() + "/assets/html/private/index.html")
            .withCookies(
              Cookie(CookieFields.email.toString, inputEmail, httpOnly = false),
              Cookie(CookieFields.hash.toString, member.hash, httpOnly = false),
              Cookie(CookieFields.id.toString, member.member_id.toString, httpOnly = false)
            )
        case _ => BadRequest("login() failed: user not authorized to login.")
      }
  }

  /**
    * @return Ok(case ID) of stored case as it is in the DB (and not that 0 stuff, a newly created case gets!),
    *         or BadRequest(error string) if something went wrong.
    */

  def saveCaze() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts
        (requestData("fileId"), requestData("case").toList) match {
          case (_, Nil) | (Nil, _) => BadRequest("Post: saveCaze() failed. No data received.")
          case (Seq(fileId), cazeJson :: Nil) => {
            Caze.decode(cazeJson.toString) match {
              case Some(caze) => {
                fileDao.addCaseToFile(caze, fileId.toInt) match {
                  case Right(newId) => Ok(s"${newId}")
                  case Left(err) => BadRequest("Post: saveCaze() failed: " + err)
                }
              }
              case None => BadRequest("Post: saveCaze() failed: decoding of caze failed. Json wrong?")
            }
          }
          case _ => BadRequest("Post: saveCaze() failed: no data received.")
        }
      }
      case Left(err) => BadRequest("Post: saveCaze() failed: " + err)
    }
  }

 /**
   * Updates case on disk, if it already exists, otherwise, does nothing.
   */

  def updateCaze() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("case"), requestData("memberId")) match {
          case (Seq(cazeJson), Seq(memberIdStr)) =>
            Caze.decode(cazeJson.toString) match {
              case Some(c) =>
                cazeDao.replaceIfExists(c)
                Ok
              case None => BadRequest("updateCaze() failed: decoding of caze failed. Json wrong?")
            }
          case _ => BadRequest("updateCaze() failed: no data received")
        }
      }
      case Left(err) => BadRequest("updateCaze() failed: " + err)
    }
  }

  def delCaze() = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("caseId"), requestData("memberId")) match {
          case (Seq(caseIdStr), Seq(memberIdStr)) =>
            doesUserHaveAuthorizedCookie(request) match {
              case Right(true) => {
                cazeDao.delete(caseIdStr.toInt)
                Ok
              }
              case Left(err) =>
                BadRequest("delCaze() failed: " + err)
            }
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
          case Some(file) => {
            fileDao.insert(file) match {
              case Right(_) => Ok
              case Left(err) => BadRequest(err)
            }
          }
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

  def updateFileDescription() = Action { request: Request[AnyContent] =>
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
