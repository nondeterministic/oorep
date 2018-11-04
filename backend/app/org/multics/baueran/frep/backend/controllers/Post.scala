package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json
import play.api.libs.json.Json

// curl --header "Content-type: application/json" --request POST --data '{"symbol":"GOOG", "price":900.00}'  http://localhost:9000/myPostRequest 

class Post @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def myPostRequest() = Action { 
    request: Request[AnyContent] => 
      println(request.body.asText)
      request.body.asText match {
        case Some(text) => println("OK"); Ok("Got: " + text)
        case None => println("!OK"); BadRequest("Nothing received?!")
      }
//      request.body.asFormUrlEncoded.flatMap(m => m.get("inputField").flatMap(_.headOption)) match {
//        case Some(text) => println("Received: " + text); Ok("Got: " + text)
//        case None => println("Form empty?"); Ok("Form empty?") // BadRequest("Form empty?!")
//      }
  }
}
