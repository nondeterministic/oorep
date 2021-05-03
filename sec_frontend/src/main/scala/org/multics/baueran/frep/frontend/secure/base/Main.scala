package org.multics.baueran.frep.frontend.secure.base

import scala.scalajs.js.annotation.JSExportTopLevel
import org.multics.baueran.frep.shared._
import frontend.{Case, LoadingSpinner, Repertorise}
import sec_frontend.{AddToFileModal, EditFileModal, FileModalCallbacks, NewFileModal, OpenFileModal, RepertoryModal}
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import org.multics.baueran.frep.shared.frontend.{apiPrefix, serverUrl}
import org.multics.baueran.frep.shared.Info

import scala.util.{Failure, Success}
import scalatags.JsDom.all.{id, _}
import monix.execution.Scheduler.Implicits.global
import org.scalajs.dom
import dom.Event
import org.multics.baueran.frep.shared.TopLevelUtilCode.deleteCookies

import scala.scalajs.js.URIUtils.encodeURI

@JSExportTopLevel("MainSecure")
object Main extends MainUtil {

  private val loadingSpinner = new LoadingSpinner("content")

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
                          data.target:="#repertoryInfoModal")(s"${info.abbrev} - ${info.displaytitle.getOrElse("")}").render)
                    })
                }
                case Left(err) =>
                  println(s"ERROR: secure.Main: JSON decoding error: $err")
              }
            }
            case Left(err) =>
              println(s"ERROR: secure.Main: JSON parsing error: $err")
          }
        }
        case failure: Failure[_] =>
          println(s"ERROR: secure.Main: available_reps failed: ${failure.toString}")
      })
  }

  private def authenticateAndPrepare(): Unit = {
    HttpRequest(s"${serverUrl()}/${apiPrefix()}/authenticate")
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          if (dom.document.getElementById("static_content") == null) {
            dom.document.getElementById("content").appendChild(Repertorise().render)
          }

          try {
            val memberId = response.get.body.toInt
            FileModalCallbacks.updateMemberFiles(memberId)
          } catch {
            case exception: Throwable =>
              dom.document.location.replace(s"${serverUrl()}/${apiPrefix()}/display_error_page?message=${encodeURI("Not authenticated or cookie expired")}")
              println("Exception: could not convert member-id '" + exception + "'. Deleting the cookies now!")
              deleteCookies()
          }
        }
        case _: Failure[SimpleHttpResponse] => {
          dom.document.location.replace(s"${serverUrl()}/${apiPrefix()}/display_error_page?message=${encodeURI("Not authenticated or cookie expired")}")
          deleteCookies()
        }
      })
  }

  def main(args: Array[String]): Unit = {
    loadJavaScriptDependencies()

    // This is the static page which is shown when JS is disabled
    val tempContent = dom.document.getElementById("temporary_content")
    while (tempContent != null && tempContent.hasChildNodes())
      tempContent.removeChild(tempContent.firstChild)

    if (dom.document.getElementById("static_content") == null) {
      Repertorise.init(loadingSpinner)
      loadingSpinner.add()
    }

    dom.document.body.appendChild(div(style := "width:100%;", id := "content_bottom").render)
    dom.document.body.appendChild(AddToFileModal().render)
    dom.document.body.appendChild(OpenFileModal().render)
    dom.document.body.appendChild(EditFileModal().render)

    // Not so nice but not so terrible either: we need to wait until modal exists to attach event-handler to it...
    dom.document.body.appendChild(NewFileModal().render)
    dom.document.getElementById("new_file_form").asInstanceOf[dom.html.Form].addEventListener("submit", NewFileModal.onSubmit, false)

    dom.document.body.appendChild(RepertoryModal().render)
    dom.document.body.appendChild(Case.analysisModalDialogHTML().render)
    dom.document.body.appendChild(Case.editDescrModalDialogHTML().render)

    if (!dom.window.location.toString.contains("/show?"))
      authenticateAndPrepare()
    else
      dom.document.getElementById("disclaimer_div").asInstanceOf[dom.html.Div].style.setProperty("display", "none")

    getRepertories()
    showNavBar()
  }

}
