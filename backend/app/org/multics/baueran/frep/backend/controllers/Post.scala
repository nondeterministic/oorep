package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json
import play.api.libs.json.Json
import org.multics.baueran.frep._
import shared.Defs._
import backend.dao.{CazeDao, FileDao, MemberDao}
import backend.db.db.DBContext
import shared.{Caze, FIle}

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

              // Does not work because JSON contains invalid Cookie values. :-(
              // Cookie("oorep_member_id", Member.memberEncoder(member).toString(), httpOnly=false)
            )
      }
  }

  def saveCaze() = Action { request: Request[AnyContent] =>
    Caze.decode(request.body.asText.get) match {
      case Some(caze) => cazeDao.replace(caze); Ok
      case None => BadRequest("Saving of caze failed. Json wrong?")
    }
  }

  def saveFile() = Action { request: Request[AnyContent] =>
    FIle.decode(request.body.asText.get) match {
      case Some(file) => fileDao.insert(file); Ok
      case None => BadRequest("Saving of file failed. Json wrong?")
    }
  }

}
