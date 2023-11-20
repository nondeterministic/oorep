package org.multics.baueran.frep.shared.sec_frontend

import org.scalajs.dom
import io.circe.parser.parse
import org.multics.baueran.frep.shared.{HttpRequest2, dbFile}

object FileModalCallbacks {

  // ----------------------------------------------------------------------------------------------------------------------------------------
  // Sending requests to server to retrieve a member's files, then callback for updating the modals which contain a file selection.
  // ----------------------------------------------------------------------------------------------------------------------------------------

  def updateMemberFiles(memberId: Int): Unit = {

    def fileHeadersAddToModals(dbFiles: List[dbFile]): Unit = {
      if (dbFiles.length == 0 && !AddToFileModal.SubmitButton.isDisabled()) {
        AddToFileModal.SubmitButton.disable()
        OpenFileModal.disableButtons()
      }

      // First remove all entries...
      OpenFileModal.empty()
      AddToFileModal.empty()

      // ...then re-add them.  By default, we sort by file header, ascending...
      dbFiles.sortBy(_.header).reverse.map(fileIdHeaderTuple => {

        AddToFileModal.appendItem(fileIdHeaderTuple, (event: dom.Event) => {
          AddToFileModal.selected_file_id = Some(fileIdHeaderTuple.id)
          AddToFileModal.selected_file_header = Some(fileIdHeaderTuple.header)
          AddToFileModal.SubmitButton.enable()
        })

        OpenFileModal.appendItem(fileIdHeaderTuple, (event: dom.Event) => {
          OpenFileModal.selected_file_id = Some(fileIdHeaderTuple.id)
          OpenFileModal.selected_file_header = Some(fileIdHeaderTuple.header)
          OpenFileModal.enableButtons()
        })

      })
    }

    def updateMemberFiles(response: String): Unit = {
      parse(response) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[List[dbFile]] match {
            case Right(dbFiles) => fileHeadersAddToModals(dbFiles)
            case Left(t) => println(s"ERROR: updateMemberFiles: decoding of available files failed: $t")
          }
        }
        case Left(_) => println("ERROR: updateMemberFiles: parsing of available files failed (is it JSON?).")
      }
    }

    HttpRequest2("sec/available_files")
      .withQueryParameters("memberId" -> memberId.toString)
      .onSuccess((response: String) => updateMemberFiles(response))
      .send()
  }

}
