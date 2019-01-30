package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json
import play.api.libs.json.Json
import org.multics.baueran.frep._
import shared.Defs._
import backend.dao.{CazeDao, MemberDao}
import backend.db.db.DBContext
import shared.Caze

class Post @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) {
  cazes = new CazeDao(dbContext)

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

              // Does not work because JSON contains invalid Cookie values. :-(
              // Cookie("oorep_member_id", Member.memberEncoder(member).toString(), httpOnly=false)
            )
      }
  }

  def saveCaze() = Action { request: Request[AnyContent] =>
    val cazeString =  request.body.asText.get
    println("Caze received: " + cazeString)

    io.circe.parser.parse(cazeString) match {
      case Right(cazeJson) =>
        val cursor = cazeJson.hcursor
        cursor.as[Caze] match {
          case Right(caze) => cazes.insert(caze); Ok
          case Left(error) => println("Failed to decode case-JSON: " + error); BadRequest("Failed to decode case-JSON: " + error)
        }
      case Left(error) => println("Failed to decode case-JSON: " + error); BadRequest("Failed to parse case-JSON: " + error)
    }
  }


//  def saveCaze() = Action { request: Request[AnyContent] =>
//    val cazeString =  request.body.asJson
//    println("Caze received: " + cazeString)
//
//    io.circe.parser.parse(cazeString) match {
//      case Right(cazeJson) =>
//        val cursor = cazeJson.hcursor
//        cursor.as[Caze] match {
//          case Right(caze) => cazes.insert(caze); Ok
//          case Left(error) => BadRequest("Failed to decode case-JSON: " + error)
//        }
//      case Left(error) => BadRequest("Failed to parse case-JSON: " + error)
//    }
//  }

}
