package org.multics.baueran.frep.shared

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.multics.baueran.frep.shared.Defs.{serverUrl, availableFiles}
import org.querki.jquery.$
import org.scalajs.dom
import scalatags.JsDom.all.p
import io.circe.parser.parse
import monix.execution.Scheduler.Implicits.global
import scalatags.JsDom.all.{id, input, _}
import org.scalajs.dom

import scala.util.{Failure, Success}

package object frontend {

  def getCookieData(cookie: String, elementName: String): Option[String] = {
    cookie.split(";").map(_.trim).foreach({ c: String =>
      c.split("=").map(_.trim).toList match {
        case name :: argument :: Nil => if (name == elementName) return Some(argument)
        case _ => ; // Do nothing
      }
    })
    None
  }

  def updateAvailableFiles(memberId: Int) = {
    HttpRequest(serverUrl() + "/availableFiles")
      .withQueryParameter("memberId", memberId.toString)
      .withCrossDomainCookies(true)
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[FIle]] match {
                case Right(files) => {
                  $("#availableFilesList").empty()
                  println("BOO!")
                  files.map(file => {
                    val listItem = a(cls := "list-group-item list-group-item-action", id := "list-profile-list", data.toggle := "list", href := "#list-profile", role := "tab", file.header)
                    println("BOO: " + file.header)
                    $("#availableFilesList").append(listItem.render)
                  })
                }
                case Left(t) => println("Parsing of available files failed: " + t)
              }
            }
            case Left(_) => println("Parsing of available files failed (is it JSON?).")
          }
        }
        case error: Failure[SimpleHttpResponse] => println("ERROR!")
      })
  }

}
