package org.multics.baueran.frep.shared.sec_frontend

import monix.execution.Scheduler.Implicits.global
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.multics.baueran.frep.shared.FIle
import org.scalajs.dom
import io.circe.parser.parse
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.querki.jquery._
import scalatags.JsDom.all._

import scala.util.{Failure, Success}

import scala.util.Try

package object Callbacks {

  // ----------------------------------------------------------------------------------------------------------------------------------------
  // Sending requests to server to retrieve a member's files, then callback for updating the modals which contain a file selection.
  // ----------------------------------------------------------------------------------------------------------------------------------------

  def updateMemberFiles(memberId: Int): Unit = {

    def updateModals(files: List[FIle]) = {
      if (files.size > 0) {
        AddToFileModal.enableButtons()

        OpenFileModal.empty()
        AddToFileModal.empty()
      }
      else if (files == 0 && !($("#submitAddToFileModal").hasOwnProperty("disabled"))) {
        AddToFileModal.disableButtons()
        OpenFileModal.disableButtons()
      }

      files.map(file => {
        val listItemAddToFile =
          a(cls := "list-group-item list-group-item-action", data.toggle := "list", href := "#list-profile", role := "tab",
            onclick := { (event: dom.Event) => AddToFileModal.selected_file_id = file.header },
            file.header)
        AddToFileModal.appendItem(listItemAddToFile.render)

        val listItemOpenFile =
          a(cls := "list-group-item list-group-item-action", data.toggle := "list", href := "#list-profile", role := "tab",
            onclick := { (event: dom.Event) =>
              OpenFileModal.selected_file_id() = file.header
              OpenFileModal.enableButtons()
            },
            file.header)
        OpenFileModal.appendItem(listItemOpenFile.render)
      })
    }

    def updateMemberFiles(response: Try[SimpleHttpResponse]) = {
      response match {
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[FIle]] match {
                case Right(files) => updateModals(files)
                case Left(t) => println("Decoding of available files failed: " + t)
              }
            }
            case Left(_) => println("Parsing of available files failed (is it JSON?).")
          }
        }
        case error: Failure[SimpleHttpResponse] => println("ERROR: " + error.get.body)
      }
    }

    HttpRequest(serverUrl() + "/availableFiles")
      .withQueryParameter("memberId", memberId.toString)
      .withCrossDomainCookies(true)
      .send()
      .onComplete((r: Try[SimpleHttpResponse]) => updateMemberFiles(r))

  }

}
