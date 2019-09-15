package org.multics.baueran.frep.shared.sec_frontend

import monix.execution.Scheduler.Implicits.global
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.multics.baueran.frep.shared.FIle
import org.scalajs.dom
import io.circe.parser.parse
import org.multics.baueran.frep.shared.frontend.serverUrl
import scalatags.JsDom.all._

import scala.util.{Failure, Success}

import scala.util.Try

package object FileModalCallbacks {

  // ----------------------------------------------------------------------------------------------------------------------------------------
  // Sending requests to server to retrieve a member's files, then callback for updating the modals which contain a file selection.
  // ----------------------------------------------------------------------------------------------------------------------------------------

  def updateMemberFiles(memberId: Int): Unit = {

    /**
      * The function's argument is a list of file IDs and headers as stored in the DB.
      */
    def updateModals(filesIdHeaderTuples: List[(Int,String)]) = {
      if (filesIdHeaderTuples.size > 0) {
        AddToFileModal.enableButtons()

        OpenFileModal.empty()
        AddToFileModal.empty()
      }
      else if (filesIdHeaderTuples == 0 && !AddToFileModal.submitIsDisabled())  {
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

    def updateMemberFiles(response: Try[SimpleHttpResponse]) = {
      response match {
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              // List[(FileID, FileHeader)]
              cursor.as[List[(Int,String)]] match {
                case Right(fileIdHeaderTuples) => updateModals(fileIdHeaderTuples)
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
