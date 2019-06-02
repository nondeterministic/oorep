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

// TODO: Like in Get.scala, check cookies for permission first!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

class Post @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) {
  cazeDao = new CazeDao(dbContext)
  fileDao = new FileDao(dbContext)

  def login() = Action {
    request: Request[AnyContent] =>
      val inputEmail: String = request.body.asFormUrlEncoded.get("inputEmail").head
      val inputPassword: String = request.body.asFormUrlEncoded.get("inputPassword").head

      // TODO: Password is still ignored!!
      memberDao.getFromEmail(inputEmail) match {
        case Nil => BadRequest("Not authorized: user not in DB")
        case member :: _ =>
          Redirect(serverUrl() + "/assets/html/private/index.html")
            .withCookies(Cookie("oorep_member_email", inputEmail, httpOnly = false),
              Cookie("oorep_member_password", inputPassword, httpOnly = false),
              Cookie("oorep_member_id", member.member_id.toString, httpOnly = false)

              // Does not work because JSON contains (or rather is a) invalid Cookie values. :-(
              // Cookie("oorep_member_id", Member.memberEncoder(member).toString(), httpOnly=false)
            )
      }
  }

  def saveCaze() = Action { request: Request[AnyContent] =>
    val requestData = request.body.asMultipartFormData.get.dataParts
    (requestData("fileheader"), requestData("case").toList) match {
      case (_, Nil) | (Nil, _) => BadRequest("saveCaze() failed. No data received.")
      case (Seq(fileheader), cazeJson::Nil) => {
        Caze.decode(cazeJson.toString) match {
          case Some(caze) => {
            fileDao.addCaseToFile(caze, fileheader)
            Ok
          }
          case None => BadRequest("Decoding of caze failed. Json wrong?")
        }
      }
      case _ => BadRequest("saveCaze() failed. No data received.")
    }
  }

  /*
   * Updates case on disk, if it already exists, otherwise, does nothing.
   */

  def updateCaze() = Action { request: Request[AnyContent] =>
    io.circe.parser.parse(request.body.asText.get) match {
      case Right(json) =>
        val cursor = json.hcursor
        cursor.as[Caze] match {
          case Right(c) => cazeDao.replaceIfExists(c); Ok
          case _ => BadRequest("Decoding of caze failed. Json wrong?")
        }
      case Left(err) => BadRequest("updateCaze() failed. No data received.")
    }
  }

  def saveFile() = Action { request: Request[AnyContent] =>
    FIle.decode(request.body.asText.get) match {
      case Some(file) => fileDao.insert(file); println(request.body.asText.get); Ok
      case None => BadRequest("Saving of file failed. Json wrong? " + request.body.asText.get)
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
    println("Received: " + request.body.asText.get)
    Ok
  }

}
