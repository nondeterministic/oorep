package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json
import play.api.libs.json.Json

class Post @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def myPostRequest() = Action { 
    request: Request[AnyContent] => 
      println(request.body.asText)
      request.body.asText match {
        case Some(text) => println("OK"); Ok("Got: " + text)
        case None => println("!OK"); BadRequest("Nothing received?!")
      }
  }

  def login() = Action {
    request: Request[AnyContent] =>
      println("Play: Req1: " + request.headers.toString())
      println("Play: Req2: " + request.body.asFormUrlEncoded.get("inputEmail"))
      println("Play: Req2: " + request.body.asFormUrlEncoded.get("inputPassword"))
      Redirect("http://www.google.de/")
//      Ok.sendFile(new java.io.File("/home/baueran/hotelrechnung.pdf"))
  }

}
