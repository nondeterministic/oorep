package org.multics.baueran.frep.frontend.secure.base

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import org.multics.baueran.frep.shared.Defs.serverUrl
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
import scala.util.Success

object NavBar {

  private def getRepertories() = {
    HttpRequest(serverUrl() + "/availableReps")
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[Info]] match {
                case Right(infos) => {
                  infos.foreach(info => {
                    dom.document
                      .getElementById("secNavBarRepertories").asInstanceOf[dom.html.Div]
                      .appendChild(a(cls:="dropdown-item", href:="#", data.toggle:="modal",
                        onclick := { (e: Event) =>
                          RepertoryModal.info() = Some(info)
                        },
                        data.target:="#repertoryInfoModal")(info.abbrev).render)
                  })
                }
              }
            }
          }
        }
      })
  }

  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    getRepertories()

    nav(cls:="navbar py-0 fixed-top navbar-expand-sm navbar-light", id:="public_nav_bar", style:="height:60px; line-height:55px;",
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler",
        span(cls:="navbar-toggler-icon")),
      div(id:="nav_bar_logo"),// a(cls := "navbar-brand py-0", href := serverUrl(), "OOREP")),
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
                val cookieNames = List("oorep_member_email", "oorep_member_password", "oorep_member_id")
                dom.document.cookie = "PLAY_SESSION=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT"
                cookieNames.foreach(cookieName =>
                  dom.document.cookie = s"${cookieName}=; path=/; expires='Thu, 01 Jan 1970 00:00:01 GMT"
                )
                dom.window.location.replace(serverUrl())
              })("Log-out"))
          )
        )
      )
    )
  }

}
