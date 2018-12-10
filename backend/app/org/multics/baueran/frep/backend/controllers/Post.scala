package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json
import play.api.libs.json.Json

class Post @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def login() = Action {
    request: Request[AnyContent] =>
      val inputEmail: String = request.body.asFormUrlEncoded.get("inputEmail").head
      val inputPassword: String = request.body.asFormUrlEncoded.get("inputPassword").head

      // TODO: Do some sensible authentication here...
      if (inputEmail == "ssj@ksks.com") {
        Redirect("http://localhost:9000/assets/html/private/index.html")
          .withCookies(Cookie("oorep_user_email", inputEmail), Cookie("oorep_user_password", inputPassword))
      }
      else
        BadRequest("Not authorized.")
  }

}
