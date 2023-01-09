package org.multics.baueran.frep.frontend.secure.base

import scala.scalajs.js.annotation.JSExportTopLevel
import org.multics.baueran.frep.shared._
import frontend.{CaseModals, LoadingSpinner, MainView, apiPrefix, serverUrl}
import sec_frontend.{AddToFileModal, EditFileModal, FileModalCallbacks, NewFileModal, OpenFileModal, RepertoryModal, MMModal}
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse

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

  // See MainUtil trait!
  override def updateDataStructuresFromBackendData() = {

    def getMMs() = {
      HttpRequest(s"${serverUrl()}/${apiPrefix()}/available_rems_and_mms")
        .send()
        .onComplete({
          case response: Success[SimpleHttpResponse] => {
            parse(response.get.body) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[List[MMAndRemedyIds]] match {
                  case Right(mmAndRemedyIds) => {
                    mmAndRemedyIds
                      .sortBy(_.mminfo.abbrev)
                      .foreach(mmAndRemedyId =>
                        dom.document
                          .getElementById("secNavBarMMs").asInstanceOf[dom.html.Div]
                          .appendChild(a(cls := "dropdown-item", href := "#", data.toggle := "modal",
                            onclick := { (e: Event) =>
                              MMModal.info() = Some(mmAndRemedyId.mminfo)
                            },
                            data.target := "#mmInfoModal")(s"${mmAndRemedyId.mminfo.abbrev} - ${mmAndRemedyId.mminfo.displaytitle.getOrElse("")}").render)
                      )
                  }
                  case Left(err) =>
                    println(s"ERROR: secure.Main: JSON mm decoding error: $err")
                }
              }
              case Left(err) =>
                println(s"ERROR: secure.Main: JSON mm parsing error: $err")
            }
          }
          case failure: Failure[_] =>
            println(s"ERROR: secure.Main: available_rems_and_mms failed: ${failure.toString}")

        })
    }

    def getRepertories() = {
      HttpRequest(s"${serverUrl()}/${apiPrefix()}/available_rems_and_reps")
        .send()
        .onComplete({
          case response: Success[SimpleHttpResponse] => {
            parse(response.get.body) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[List[InfoExtended]] match {
                  case Right(extendedRepertoryInfos) => {
                    extendedRepertoryInfos
                      .sortBy(_.info.abbrev)
                      .foreach(extendedInfo =>
                        dom.document
                          .getElementById("secNavBarRepertories").asInstanceOf[dom.html.Div]
                          .appendChild(a(cls := "dropdown-item", href := "#", data.toggle := "modal",
                            onclick := { (e: Event) =>
                              RepertoryModal.info() = Some(extendedInfo.info)
                            },
                            data.target := "#repertoryInfoModal")(s"${extendedInfo.info.abbrev} - ${extendedInfo.info.displaytitle.getOrElse("")}").render)
                      )
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
            println(s"ERROR: secure.Main: available_rems_and_reps failed: ${failure.toString}")
        })
    }

    getRepertories()
    getMMs()
    MainView.updateDataStructuresFromBackendData()
  }

  private def authenticateAndPrepare(): Unit = {
    HttpRequest(s"${serverUrl()}/${apiPrefix()}/authenticate")
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          if (dom.document.getElementById("static_content") == null) {
            dom.document.getElementById("content").appendChild(MainView().render)
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
    if (dom.document.getElementById("temporary_content") != null)
      dom.document.body.removeChild(dom.document.getElementById("temporary_content"))

    if (dom.document.getElementById("static_content") == null) {
      val loadingSpinner = new LoadingSpinner("content")
      loadingSpinner.add()
      MainView.init(loadingSpinner)

      // Both /?show...-calls call their own render() functions via html-page embedded JS
      if (dom.window.location.toString.contains("/show")) {
        dom.document.getElementById("disclaimer_div").asInstanceOf[dom.html.Div].style.setProperty("display", "none")
      }
      // /?change_password mustn't execute the main OOREP application. So, do nothing!
      else if (dom.window.location.toString.contains("/change_password?")) {
        ;
      }
      else {
        // Static content must not also show the repertorisation view
        // dom.document.getElementById("content").appendChild(MainView().render)
        ;
      }
    }

    dom.document.body.appendChild(div(style := "width:100%;", id := "content_bottom").render)
    dom.document.body.appendChild(AddToFileModal().render)
    dom.document.body.appendChild(OpenFileModal().render)
    dom.document.body.appendChild(EditFileModal().render)

    // Not so nice but not so terrible either: we need to wait until modal exists to attach event-handler to it...
    dom.document.body.appendChild(NewFileModal().render)
    dom.document.getElementById("new_file_form").asInstanceOf[dom.html.Form].addEventListener("submit", NewFileModal.onSubmit, false)

    dom.document.body.appendChild(RepertoryModal().render)
    dom.document.body.appendChild(MMModal().render)
    dom.document.body.appendChild(CaseModals.Repertorisation().render)
    dom.document.body.appendChild(CaseModals.EditDescription().render)

    if (!dom.window.location.toString.contains("/show"))
      authenticateAndPrepare()
    else
      dom.document.getElementById("disclaimer_div").asInstanceOf[dom.html.Div].style.setProperty("display", "none")

    showNavBar()
  }

}
