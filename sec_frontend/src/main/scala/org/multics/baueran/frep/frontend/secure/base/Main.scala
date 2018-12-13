package org.multics.baueran.frep.frontend.secure.base

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.scalajs.dom.XMLHttpRequest
import monix.execution.Scheduler.Implicits.global
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw._

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation._
import org.querki.jquery._
import scalatags.JsDom.all._

import scala.util.{Failure, Success}
import org.multics.baueran.frep.shared.frontend.Repertorise

@JSExportTopLevel("MainSecure")
object Main {
  val hspace = div(cls:="col-xs-12", style:="height:50px;")

  def main(args: Array[String]): Unit = {
    // No access without valid cookies!
    HttpRequest("http://localhost:9000/authenticate")
      .withCrossDomainCookies(true)
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          $("#nav_bar").empty()
          $("#nav_bar").append(NavBar.apply().render)
          $("#content").append(Repertorise.apply().render)
          $("#content_bottom").append(Disclaimer.toHTML().render)
        }
        case error: Failure[SimpleHttpResponse] => {
          $("#content").append(p("Not authorized.").render)
        }
      })
  }
}
