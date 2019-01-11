package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json
import play.api.libs.json.Json
import org.multics.baueran.frep.shared.Defs._

class Post @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def login() = Action {
    request: Request[AnyContent] =>
      val inputEmail: String = request.body.asFormUrlEncoded.get("inputEmail").head
      val inputPassword: String = request.body.asFormUrlEncoded.get("inputPassword").head

      // TODO: Password is still ignored!!
      members.getFromEmail(inputEmail) match {
        case Nil => BadRequest("Not authorized: user not in DB")
        case member :: _ =>
          Redirect(serverUrl() + "/assets/html/private/index.html")
            .withCookies(Cookie("oorep_member_email", inputEmail, httpOnly = false),
              Cookie("oorep_member_password", inputPassword, httpOnly = false),
              Cookie("oorep_member_id", member.member_id.toString, httpOnly = false)
            )
      }
  }

}
