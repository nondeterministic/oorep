package org.multics.baueran.frep.frontend.secure.base

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import org.multics.baueran.frep.shared.Defs.deleteCookies
import org.multics.baueran.frep.shared.frontend.{serverUrl, apiPrefix}
import org.multics.baueran.frep.shared.Info
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{id, li, _}
import scalatags.JsDom.tags2.nav
import monix.execution.Scheduler.Implicits.global
import org.scalajs.dom
import dom.Event
import org.multics.baueran.frep.shared.sec_frontend.RepertoryModal

import scala.scalajs
import scala.scalajs.js
import scala.util.{Failure, Success}

object NavBar {

  private def getRepertories() = {
    HttpRequest(s"${serverUrl()}/${apiPrefix()}/available_reps")
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[Info]] match {
                case Right(infos) => {
                  infos
                    .sortBy(_.abbrev)
                    .foreach(info => {
                      dom.document
                        .getElementById("secNavBarRepertories").asInstanceOf[dom.html.Div]
                        .appendChild(a(cls:="dropdown-item", href:="#", data.toggle:="modal",
                          onclick := { (e: Event) =>
                            RepertoryModal.info() = Some(info)
                          },
                          data.target:="#repertoryInfoModal")(info.abbrev).render)
                    })
                }
                case Left(err) =>
                  println(s"ERROR: secure.NavBar: JSON decoding error: $err")
              }
            }
            case Left(err) =>
              println(s"ERROR: secure.NavBar: JSON parsing error: $err")
          }
        }
        case failure: Failure[_] =>
          println(s"ERROR: secure.NavBar: available_reps failed: ${failure.toString}")
      })
  }

  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    getRepertories()

    nav(cls:="navbar py-0 fixed-top navbar-expand-sm navbar-light", id:="public_nav_bar", style:="height:60px; line-height:55px;",
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler",
        span(cls:="navbar-toggler-icon")),
      div(id:="nav_bar_logo"),
      div(cls:="collapse navbar-collapse", id:="navbarToggler",
        div( // cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item dropdown", a(cls:="nav-link dropdown-toggle", href:="#", data.toggle:="dropdown")("Repertories"),
              div(cls:="dropdown-menu", id:="secNavBarRepertories")
            ),
            // li(cls:="navbar-item", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Repertory")),
            // li(cls:="navbar-item", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Materia Medica")),
            li(cls:="navbar-item dropdown", a(cls:="nav-link dropdown-toggle", href:="#", data.toggle:="dropdown")("File"),
              div(cls:="dropdown-menu",
                a(cls:="dropdown-item", href:="#", data.toggle:="modal", data.target:="#newFileModal")("New..."),
                a(cls:="dropdown-item", href:="#", data.toggle:="modal", data.target:="#openFileModal")("Open...")
              )
            )
          )
        ),
        div(cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item", a(cls:="nav-link", href:="#", onclick:={ () => println("pressed1") })("Settings")),
            li(cls:="navbar-item", a(cls:="nav-link", href:="#", target:="_self",
              onclick:={ (e: Event) =>
                e.stopPropagation()
                deleteCookies()
                dom.window.location.replace(serverUrl())
              })("Log-out"))
          )
        )
      )
    )
  }

}
