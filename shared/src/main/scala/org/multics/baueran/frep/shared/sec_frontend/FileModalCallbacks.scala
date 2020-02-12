package org.multics.baueran.frep.shared.sec_frontend

import java.io.IOException

import monix.execution.Scheduler.Implicits.global
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.exceptions.HttpException
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.scalajs.dom
import io.circe.parser.parse
import org.multics.baueran.frep.shared.frontend.{apiPrefix, serverUrl}
import scalatags.JsDom.all._

import scala.util.{Failure, Success}

package object FileModalCallbacks {

  // ----------------------------------------------------------------------------------------------------------------------------------------
  // Sending requests to server to retrieve a member's files, then callback for updating the modals which contain a file selection.
  // ----------------------------------------------------------------------------------------------------------------------------------------

  def updateMemberFiles(memberId: Int): Unit = {

    /**
      * The function's argument is a list of file IDs and headers as stored in the DB.
      */
    def fileHeadersAddToModals(filesIdHeaderTuples: List[(Int, String)]) = {
      if (filesIdHeaderTuples.size > 0) {
        AddToFileModal.enableButtons()

        OpenFileModal.empty()
        AddToFileModal.empty()
      }
      else if (filesIdHeaderTuples == 0 && !AddToFileModal.submitIsDisabled()) {
        AddToFileModal.disableButtons()
        OpenFileModal.disableButtons()
      }

      filesIdHeaderTuples.map(fileIdHeaderTuple => {
        val listItemAddToFile =
          a(cls := "list-group-item list-group-item-action", data.toggle := "list", href := "#list-profile", role := "tab",
            onclick := { (event: dom.Event) =>
              AddToFileModal.selected_file_id() = Some(fileIdHeaderTuple._1)
              AddToFileModal.selected_file_header() = Some(fileIdHeaderTuple._2)
            },
            data.`fileId` := s"${fileIdHeaderTuple._1}",
            fileIdHeaderTuple._2)
        AddToFileModal.appendItem(listItemAddToFile.render)

        val listItemOpenFile =
          a(cls := "list-group-item list-group-item-action", data.toggle := "list", href := "#list-profile", role := "tab",
            onclick := { (event: dom.Event) =>
              OpenFileModal.selected_file_id() = Some(fileIdHeaderTuple._1)
              OpenFileModal.selected_file_header() = Some(fileIdHeaderTuple._2)
              OpenFileModal.enableButtons()
            },
            fileIdHeaderTuple._2)
        OpenFileModal.appendItem(listItemOpenFile.render)
      })
    }

    def updateMemberFiles(response: Any): Unit = {
      response match {
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[(Int, String)]] match {
                case Right(fileIdHeaderTuples) => fileHeadersAddToModals(fileIdHeaderTuples)
                case Left(t) => println(s"ERROR: updateMemberFiles: decoding of available files failed: $t")
              }
            }
            case Left(_) => println("ERROR: updateMemberFiles: parsing of available files failed (is it JSON?).")
          }
        }
        case _: Any => println("ERROR: updateMemberFiles: received Failure response likely due a stale cookie, which has now been deleted.")
      }
    }

    // When an exception occurs, we're erring on the safe side (as it is likely due to a stale cookie)
    // and reload the main page, which checks if a valid cookie is present.  And if it isn't, you'll
    // have to relogin.
    HttpRequest(s"${serverUrl()}/${apiPrefix()}/available_files")
      .withQueryParameter("memberId", memberId.toString)
      .send()
      .recover {
        case HttpException(e: SimpleHttpResponse) =>
          dom.window.location.replace(serverUrl())
          println(s"ERROR: updateMemberFiles: HttpException occurred: ${e.statusCode}")
        case e: IOException =>
          dom.window.location.replace(serverUrl())
          println(s"ERROR: updateMemberFiles: IOException occurred: There was a network issue, perhaps try again: ${e.getMessage}")
      }
      .onComplete((r: Any) => updateMemberFiles(r))

  }

}
