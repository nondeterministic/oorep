package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json
import play.api.libs.json.Json
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
      println("TODO: Remove me: " + hashedPass)

      memberDao.getFromEmail(inputEmail) match {
        case member :: _ if (hashedPass == member.hash) =>
          Redirect(serverUrl() + "/assets/html/private/index.html")
            .withCookies(
              Cookie(CookieFields.email.toString, inputEmail, httpOnly = false),
              Cookie(CookieFields.hash.toString, member.hash, httpOnly = false),
              Cookie(CookieFields.id.toString, member.member_id.toString, httpOnly = false)

              // Does not work because JSON contains (or rather is a) invalid Cookie values. :-(
              // Cookie("oorep_member_id", Member.memberEncoder(member).toString(), httpOnly=false)
            )
        case _ => BadRequest("User not authorized to login.")
      }
  }

  /**
    * @return Ok(case ID) of stored case as it is in the DB (and not that 0 stuff, a newly created case gets!),
    *         or BadRequest(error string) if something went wrong.
    */

  def saveCaze() = Action { request: Request[AnyContent] =>
    val requestData = request.body.asMultipartFormData.get.dataParts
    (requestData("fileheader"), requestData("case").toList) match {
      case (_, Nil) | (Nil, _) => BadRequest("saveCaze() failed. No data received.")
      case (Seq(fileheader), cazeJson::Nil) => {
        Caze.decode(cazeJson.toString) match {
          case Some(caze) => {
            fileDao.addCaseToFile(caze, fileheader) match {
              case Right(newId) => Ok(s"${newId}")
              case Left(err) => BadRequest("saveCaze(): ERROR: " + err)
            }
          }
          case None => BadRequest("Decoding of caze failed. Json wrong?")
        }
      }
      case _ => BadRequest("saveCaze() failed. No data received.")
    }
  }

 /**
   * Updates case on disk, if it already exists, otherwise, does nothing.
   */

  def updateCaze() = Action { request: Request[AnyContent] =>
    val requestData = request.body.asMultipartFormData.get.dataParts

    (requestData("case"), requestData("memberId")) match {
      case (Seq(cazeJson), Seq(memberIdStr)) =>
        Caze.decode(cazeJson.toString) match {
          case Some(c) =>
            cazeDao.replaceIfExists(c, memberIdStr.toInt)
            Ok
          case None => BadRequest("Decoding of caze failed. Json wrong?")
        }
      case _ => BadRequest("updateCaze() failed. No data received")
    }
  }

  def delCaze() = Action { request: Request[AnyContent] =>
    val requestData = request.body.asMultipartFormData.get.dataParts

    (requestData("caseId"), requestData("memberId")) match {
      case (Seq(caseIdStr), Seq(memberIdStr)) =>
        doesUserHaveCorrespondingCookie(request, memberIdStr.toInt) match {
          case Right(true) => {
            cazeDao.delete(caseIdStr.toInt, memberIdStr.toInt)
            Ok
          }
          case Left(err) =>
            BadRequest(err)
        }
      case _ =>
        BadRequest
    }
  }

  def saveFile() = Action { request: Request[AnyContent] =>
    FIle.decode(request.body.asText.get) match {
      case Some(file) => {
        fileDao.insert(file) match {
          case Right(_) => Ok
          case Left(err) => BadRequest(err)
        }
      }
      case None =>
        BadRequest("Saving of file failed. Json wrong? " + request.body.asText.get)
    }
  }

  def delFile() = Action { request: Request[AnyContent] =>
    val requestData = request.body.asMultipartFormData.get.dataParts
    (requestData("fileheader"), requestData("memberId").toList) match {
      case (_, Nil) | (Nil, _) => BadRequest("delFile() failed. No data received.")
      case (Seq(fileheader), memberId::Nil) => {
        fileDao.delFile(fileheader, memberId.toInt)
        Ok
      }
      case _ => BadRequest("delFile() failed. Wrong data received.")
    }
  }

  def updateFileDescription() = Action { request: Request[AnyContent] =>
    val requestData = request.body.asMultipartFormData.get.dataParts

    (requestData("filedescr"), requestData("fileheader"), requestData("memberId")) match {
      case (Seq(filedescr), Seq(fileheader), Seq(memberIdStr)) =>
        doesUserHaveCorrespondingCookie(request, memberIdStr.toInt) match {
          case Right(true) => {
            fileDao.changeDescription(fileheader, memberIdStr.toInt, filedescr)
            Ok
          }
          case Left(err) =>
            BadRequest(err)
        }
      case _ =>
        BadRequest
    }
  }

}
