package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import dom.Event
import fr.hmil.roshttp.{HttpRequest, Method}
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import monix.execution.Scheduler.Implicits.global

import org.scalajs.dom.raw.HTMLInputElement
import org.querki.jquery.$
import org.scalajs.dom
import scalatags.JsDom
import scalatags.JsDom.all._
import io.circe.syntax._

import scala.scalajs.js
import scala.collection.mutable
import mutable.ListBuffer
import rx.{Rx, Var}
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._

import org.multics.baueran.frep.shared
import shared.frontend.Notify
import shared._
import shared.Defs.CookieFields
import shared.frontend.RemedyFormat.RemedyFormat
import shared.sec_frontend.FileModalCallbacks._

import scala.language.implicitConversions

object Case {

  var descr: Option[shared.Caze] = None
  var cRubrics: List[CaseRubric] = List()
  private val remedyScores = mutable.HashMap[String,Integer]()
  private var prevCase: Option[shared.Caze] = None

  object SortCaseBy extends Enumeration {
    type SortCaseBy = Value
    val Weight = Value("Weight")
    val Abbrev = Value("Abbrev")
    val Label = Value("Label")
    val Path = Value("Path")
  }
  import SortCaseBy._
  private var sortCaseBy = Abbrev
  private var sortReverse = Var(false)
  sortReverse.triggerLater(updateCaseViewAndDataStructures())

  // This is not really necessary for proper functioning, but when deleting a case/file which is currently shown,
  // the user could, by pressing add or remove again, mess up the database as we're trying to write to a file/case,
  // or try to delete it, that no longer exists.
  // private var currOpenFileHeader: Option[String] = None
  private var currOpenFileId: Option[Int] = None

  // ------------------------------------------------------------------------------------------------------------------
  def size() = cRubrics.size

  // ------------------------------------------------------------------------------------------------------------------
  def addRepertoryLookup(r: CaseRubric) = {
    if (cRubrics.filter(cr => cr.rubric.id == r.rubric.id && cr.repertoryAbbrev == r.repertoryAbbrev).length == 0)
      cRubrics = r :: cRubrics
  }

  // ------------------------------------------------------------------------------------------------------------------
  def updateCurrOpenFile(fileId: Option[Int]) = {
    currOpenFileId = fileId
  }

  // ------------------------------------------------------------------------------------------------------------------
  def getCurrOpenFileId() = {
    currOpenFileId
  }

  // ------------------------------------------------------------------------------------------------------------------
  // Return the full remedy name for nameabbrev from a list of caseRubrics
  def getFullNameFromCasRubrics(remedies: List[Remedy], nameabbrev: String) = {
    remedies.find(_.nameAbbrev == nameabbrev) match {
      case Some(remedy) => Some(remedy.nameLong)
      case None => None
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  // Called from the outside.  Typically, an updateCaseViewAndDatastructures() follows such a call.
  def updateCurrOpenCaseId(caseId: Int) = {
    if (descr != None) {
      descr = Some(shared.Caze(caseId, descr.get.header, descr.get.member_id, descr.get.date, descr.get.description, cRubrics))
      dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].setAttribute("readonly", "readonly")
    }
    else
      println(s"Case: updateCaseId with ID ${caseId} failed.")
  }

  def rmCaseDiv() = {
    $("#caseDiv").empty()
  }

  // ------------------------------------------------------------------------------------------------------------------
  def updateCaseViewAndDataStructures(): Unit = {
    def updateFileModalDataStructures(): Unit = {
      val memberId = getCookieData(dom.document.cookie, CookieFields.id.toString) match {
        case Some(id) => updateMemberFiles(id.toInt); id.toInt
        case None => println("WARNING: updateFileModalDataStructures() failed. Could not get memberID from cookie."); -1
      }

      remedyScores.clear()
      cRubrics.foreach(caseRubric => {
        caseRubric.weightedRemedies.foreach { case WeightedRemedy(r, w) => {
          remedyScores.put(r.nameAbbrev, remedyScores.getOrElseUpdate(r.nameAbbrev, 0) + caseRubric.rubricWeight * w)
        }}
      })

      if (descr.isDefined) {
        descr = Some(shared.Caze(descr.get.id, descr.get.header, descr.get.member_id, descr.get.date, descr.get.description, cRubrics))

        // If user is logged in, attempt to update case in DB (if it exists; see comment in Post.scala),
        // and if previous case != current case.
        // And, it only makes sense to update, if there are any rubrics left, e.g., which may not be the
        // case after pressing "Remove" a few times...
        if ((memberId >= 0) && prevCase.isDefined && (prevCase.get.id == descr.get.id) && (cRubrics.size > 0) && (prevCase.get != descr.get)) {
          // Before we write the case to disk, we update the date to record the change.
          // We do not do this above, as the prevCase != descr check would always fail then!
          descr = Some(shared.Caze(descr.get.id, descr.get.header, descr.get.member_id, (new js.Date()).toISOString(), descr.get.description, cRubrics))

          if (descr.get.isSupersetOf(prevCase.get).length > 0) { // Add additional case rubrics to DB
            val diff = descr.get.isSupersetOf(prevCase.get)

            HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/add_caserubrics_to_case")
              .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
              .post(MultiPartBody(
                "memberID" -> PlainTextBody(memberId.toString),
                "caseID" -> PlainTextBody(descr.get.id.toString),
                "caserubrics" -> PlainTextBody(diff.asJson.toString)))
          }
          else if (prevCase.get.isSupersetOf(descr.get).length > 0) { // Delete the removed case rubrics in DB
            val diff = prevCase.get.isSupersetOf(descr.get)

            HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/del_caserubrics_from_case")
              .withMethod(Method.DELETE)
              .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
              .withBody(MultiPartBody(
                "memberID" -> PlainTextBody(memberId.toString),
                "caseID" -> PlainTextBody(descr.get.id.toString),
                "caserubrics" -> PlainTextBody(diff.asJson.toString)))
              .send()
          }
          else if (descr.get.isEqualExceptUserDefinedValues(prevCase.get).length > 0) { // Update user defined case rubric values only in DB
            val diff = prevCase.get.isEqualExceptUserDefinedValues(descr.get) // These are the user-changed ones, which we'll need to update in the DB, too.

            HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/update_caserubrics_userdef")
              .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
              .put(MultiPartBody(
                "memberID" -> PlainTextBody(memberId.toString),
                "caseID" -> PlainTextBody(descr.get.id.toString),
                "caserubrics" -> PlainTextBody(diff.asJson.toString)))
          }
          else if (descr.get.description != prevCase.get.description) {
            HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/update_case_description")
              .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
              .put(MultiPartBody(
                "memberID" -> PlainTextBody(memberId.toString),
                "caseID" -> PlainTextBody(descr.get.id.toString),
                "casedescription" -> PlainTextBody(descr.get.description)))
          }
          else {
            println("Case: updateFileModalDataStructures(): NOT saving case, although something indicates it may have changed. " +
              "This shouldn't have happened, but previous saves should have taken care that no data-loss occurred.")
          }
        }
        else if (memberId == -1) {
          if (Notify.noAlertsVisible())
            new Notify("tempFeedbackAlert", "Saving case failed. Please log out and back in, and then try again.")
        } else
          println("Case: updateFileModalDataStructures(): NOT updating case in DB as case is currently unchanged (or member not authorised).")
      }

      // Delete not only view but entire case from DB, when user removed all of its rubrics...
      if (cRubrics.size == 0) {
        if (descr != None && descr.get.id != 0)
          HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/del_case")
            .withMethod(Method.DELETE)
            .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
            .withBody(MultiPartBody(
              "caseId" -> PlainTextBody(descr.get.id.toString()),
              "memberId" -> PlainTextBody(memberId.toString())))
            .send()

        if (dom.document.getElementById("caseDescrId") != null)
          dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].removeAttribute("readonly")
        descr = None
      }
    }

    // Update data structures first
    updateFileModalDataStructures()

    // Now, put previous case to current case; a bit more verbose in order to avoid that prevCase.eq(descr) holds
    // as would be the case with prevCase = descr from what I've tried...
    // Update: this is due to the var in CaseRubric data structure. F*CK!
    if (descr.isDefined)
      prevCase = Some(descr.get.copy(results = cRubrics.map(_.copy())))
    else
      prevCase = None

    // Only draw labels and weights, if user is actually using them
    val caseUsesLabels = cRubrics.filter(_.rubricLabel != None).length > 0
    val caseUsesWeights = cRubrics.filter(_.rubricWeight != 1).length > 0

    // Redraw table header
    $("#analysisTHead").empty()
    if (caseUsesWeights)
      $("#analysisTHead").append(th(attr("scope"):="col", a(href:="#caseSectionOfPage", cls:="underline", onclick:={ (event: Event) => sortCaseBy = Weight; sortReverse() = !sortReverse.now }, "W.")).render)
    $("#analysisTHead").append(th(attr("scope"):="col", a(href:="#caseSectionOfPage", cls:="underline", onclick:={ (event: Event) => sortCaseBy = Abbrev; sortReverse() = !sortReverse.now }, "Rep.")).render)
    if (caseUsesLabels)
      $("#analysisTHead").append(th(attr("scope"):="col", a(href:="#caseSectionOfPage", cls:="underline", onclick:={ (event: Event) => sortCaseBy = Label; sortReverse() = !sortReverse.now }, "L.")).render)
    $("#analysisTHead").append(th(attr("scope"):="col", a(href:="#caseSectionOfPage", cls:="underline", onclick:={ (event: Event) => sortCaseBy = Path; sortReverse() = !sortReverse.now }, "Rubric")).render)
    val allRemediesInCase = cRubrics.map(_.weightedRemedies.map(_.remedy)).flatten.distinct
    remedyScores.toList.sortWith(_._2 > _._2).map(_._1).foreach(nameabbrev =>
      $("#analysisTHead").append(th(attr("scope") := "col",
        data.toggle:="tooltip", title:=s"${getFullNameFromCasRubrics(allRemediesInCase, nameabbrev).getOrElse("LOOK-UP-ERROR")}",
        div(cls:="vertical-text", style:="width: 30px;",
        s"${nameabbrev} (${remedyScores.get(nameabbrev).get})")).render))

    // Redraw table body
    implicit def stringToString(s: String) = new BetterString(s) // For 'shorten'.

    $("#analysisTBody").empty()
    for (cr <- cRubrics.filter(_.rubricWeight > 0)
      .sortBy(cr => {
        if (sortCaseBy == Weight)
          s"${cr.rubricWeight}${cr.rubricLabel.getOrElse("")}${cr.repertoryAbbrev}${cr.rubric.fullPath}"
        else if (sortCaseBy == Path)
          cr.rubric.fullPath
        else if (sortCaseBy == Abbrev)
          cr.repertoryAbbrev + cr.rubricLabel.getOrElse("") + cr.rubric.fullPath
        else
          cr.rubricLabel.getOrElse("") + cr.repertoryAbbrev + cr.rubric.fullPath
      })(if (sortReverse.now) Ordering[String].reverse else Ordering[String].key))
    {
      val trId = cr.rubric.fullPath.replaceAll("[^A-Za-z0-9]", "") + "_" + cr.repertoryAbbrev

      // Construct table row entries
      var tableRowEntries = new ListBuffer[JsDom.TypedTag[dom.html.TableCell]]()
      if (caseUsesWeights)
        tableRowEntries += td(cr.rubricWeight.toString())
      tableRowEntries += td(cr.repertoryAbbrev)
      if (caseUsesLabels)
        tableRowEntries += td(cr.rubricLabel.getOrElse("").toString())
      tableRowEntries += td(style:="white-space: nowrap;", cr.rubric.fullPath.shorten)

      // Add table row
      $("#analysisTBody").append(
        tr(scalatags.JsDom.attrs.id := trId, tableRowEntries.toList).render)

      remedyScores.toList.sortWith(_._2 > _._2).map(_._1) foreach (abbrev => {
        if (cr.rubricWeight > 0 && cr.containsRemedyAbbrev(abbrev))
          $("#" + trId).append(td(data.toggle:="tooltip", title:=s"${cr.rubric.fullPath.shorten(40)}", "" + (cr.getRemedyWeight(abbrev) * cr.rubricWeight)).render)
        else
          $("#" + trId).append(td(data.toggle:="tooltip", title:=s"${cr.rubric.fullPath.shorten(40)}", " ").render)
      })
    }
  }

  def updateCaseHeaderView() = {
    getCookieData(dom.document.cookie, CookieFields.id.toString) match {
      // Not logged in...
      case None =>
        $("#openNewCaseButton").hide()
        $("#editDescrButton").hide()
        $("#closeCaseButton").hide()
      // Logged in...
      case Some(_) =>
        descr match {
          // Case doesn't exist...
          case None =>
            $("#openNewCaseButton").show()
            $("#editDescrButton").hide()
            $("#closeCaseButton").hide()
            $("#addToFileButton").attr("disabled", true)
          // Case exists...
          case Some(currCase) =>
            $("#openNewCaseButton").hide()
            $("#editDescrButton").show()
            $("#closeCaseButton").show()

            // Id is != 0, if the case has been already added to DB.  We disallow readding.
            if (currCase.id == 0) {
              $("#addToFileButton").removeAttr("disabled")
              dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].removeAttribute("readonly")
            }
            else {
              $("#addToFileButton").attr("disabled", true)
              dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].setAttribute("readonly", "readonly")
            }
        }
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  // The modal-dialog HTML-code for showing the case analysis
  def analysisModalDialogHTML() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", scalatags.JsDom.attrs.id:="caseAnalysisModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        // style:="left: 10%; min-width: 80%; margin-left:0px; margin-right:0px; margin-top:20px;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Case repertorisation"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              table(cls:="table case table-striped table-hover table-sm table-bordered",
                thead(scalatags.JsDom.attrs.id:="analysisTHead",
                  th(attr("scope"):="col", "W."),
                  th(attr("scope"):="col", "Rep."),
                  th(attr("scope"):="col", "Rubric")
                ),
                tbody(scalatags.JsDom.attrs.id:="analysisTBody")
              )
            )
          )
        )
      )
    )
  }

  // ------------------------------------------------------------------------------------------------------------------
  // The modal-dialog HTML-code for editing case description
  def editDescrModalDialogHTML() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", scalatags.JsDom.attrs.id:="caseDescriptionModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Case description"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              form(
                div(cls:="form-group",
                  label(`for`:="caseDescrId", "ID"),
                  input(cls:="form-control", id:="caseDescrId", placeholder:="A simple, unique case identifier", required)
                ),
                div(cls:="form-group",
                  label(`for`:="caseDescrDescr", "Description"),
                  textarea(cls:="form-control", id:="caseDescrDescr", rows:="3", placeholder:="A more verbose description of the case")
                ),
                div(cls:="d-flex flex-row-reverse",
                  button(cls:="btn btn-primary mb-2", style:="margin-left:8px;", `type`:="button",
                    "Submit",
                    onclick:={(event: Event) =>
                      event.stopPropagation()

                      val caseIdTxt = dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].value
                      dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].setAttribute("readonly", "readonly")
                      val caseDescrTxt = dom.document.getElementById("caseDescrDescr").asInstanceOf[HTMLInputElement].value
                      val memberId = getCookieData(dom.document.cookie, CookieFields.id.toString) match {
                        case Some(id) => id.toInt
                        case None => -1 // TODO: Force user to relogin; the identification cookie has disappeared!!!!!!!!!!
                      }

                      descr = Some(shared.Caze( // WHERE CAZE IS INITIALLY CREATED: WITH ID -1!
                        (if (descr.isDefined) descr.get.id else -1),
                        caseIdTxt,
                        memberId,
                        (new js.Date()).toISOString(),
                        caseDescrTxt,
                        cRubrics))

                      dom.document.getElementById("caseHeader").textContent = s"Case '${descr.get.header}':"
                      $("#openNewCaseButton").hide()
                      $("#editDescrButton").show()
                      $("#closeCaseButton").show()
                      $("#addToFileButton").removeAttr("disabled")
                      js.eval("$('#caseDescriptionModal').modal('hide');")

                      updateCaseViewAndDataStructures()
                    }),
                  button(data.dismiss:="modal", cls:="btn mb-2 btn-secondary",
                    "Cancel",
                    onclick:={ (event: Event) =>
                      descr match {
                        case Some(descr) =>
                          $("#caseDescrId").`val`(descr.header)
                          $("#caseDescrDescr").`val`(descr.description)
                        case None =>
                          $("#caseDescrId").`val`("")
                          $("#caseDescrDescr").`val`("")
                      }
                    })
                )
              )
            )
          )
        )
      )
    )
  }

  // ------------------------------------------------------------------------------------------------------------------
  def toHTML(remedyFormat: RemedyFormat): JsDom.TypedTag[dom.html.Div] = {
    updateCaseViewAndDataStructures()

    def caseRow(crub: CaseRubric) = {
      implicit def crToCR(cr: CaseRubric) = new BetterCaseRubric(cr)

      val remedies = crub.getFormattedRemedyNames(remedyFormat)
      val weight = Var(crub.rubricWeight) // The weight label on the drop-down button, which needs to change automatically on new user choice
      val printWeight = Rx { weight().toString() }

      // Same for label
      val label = Var(crub.rubricLabel)
      val printLabel = Rx { label().getOrElse("") }

      tr(scalatags.JsDom.attrs.id := "crub_" + crub.rubric.id + crub.repertoryAbbrev,
        td(
          button(`type` := "button", cls := "btn dropdown-toggle btn-sm btn-secondary", style := "width:45px;", data.toggle := "dropdown", printWeight),
          div(cls := "dropdown-menu",
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 0; weight() = 0; updateCaseViewAndDataStructures() }, "0 (ignore)"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 1; weight() = 1; updateCaseViewAndDataStructures() }, "1 (normal)"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 2; weight() = 2; updateCaseViewAndDataStructures() }, "2 (important)"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 3; weight() = 3; updateCaseViewAndDataStructures() }, "3 (very important)"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 4; weight() = 4; updateCaseViewAndDataStructures() }, "4 (essential)")
          )
        ),
        td(crub.repertoryAbbrev),
        td(
          button(`type`:="button", cls:="btn dropdown-toggle btn-sm btn-secondary", style:="width:45px;", data.toggle:="dropdown", printLabel),
          div(cls := "dropdown-menu", style:="max-height:250px; overflow-y:auto;",
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = None; label() = None; updateCaseViewAndDataStructures() }, "none"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("A"); label() = Some("A"); updateCaseViewAndDataStructures() }, "A"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("B"); label() = Some("B"); updateCaseViewAndDataStructures() }, "B"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("C"); label() = Some("C"); updateCaseViewAndDataStructures() }, "C"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("D"); label() = Some("D"); updateCaseViewAndDataStructures() }, "D"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("E"); label() = Some("E"); updateCaseViewAndDataStructures() }, "E"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("F"); label() = Some("F"); updateCaseViewAndDataStructures() }, "F"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("G"); label() = Some("G"); updateCaseViewAndDataStructures() }, "G"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("H"); label() = Some("H"); updateCaseViewAndDataStructures() }, "H"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("I"); label() = Some("I"); updateCaseViewAndDataStructures() }, "I"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("J"); label() = Some("J"); updateCaseViewAndDataStructures() }, "J"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("K"); label() = Some("K"); updateCaseViewAndDataStructures() }, "K"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("L"); label() = Some("L"); updateCaseViewAndDataStructures() }, "L"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("M"); label() = Some("M"); updateCaseViewAndDataStructures() }, "M"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("N"); label() = Some("N"); updateCaseViewAndDataStructures() }, "N"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("O"); label() = Some("O"); updateCaseViewAndDataStructures() }, "O"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("P"); label() = Some("P"); updateCaseViewAndDataStructures() }, "P"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("Q"); label() = Some("Q"); updateCaseViewAndDataStructures() }, "Q"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("R"); label() = Some("R"); updateCaseViewAndDataStructures() }, "R"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("S"); label() = Some("S"); updateCaseViewAndDataStructures() }, "S"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("T"); label() = Some("T"); updateCaseViewAndDataStructures() }, "T"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("U"); label() = Some("U"); updateCaseViewAndDataStructures() }, "U"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("V"); label() = Some("V"); updateCaseViewAndDataStructures() }, "V"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("W"); label() = Some("W"); updateCaseViewAndDataStructures() }, "W"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("X"); label() = Some("X"); updateCaseViewAndDataStructures() }, "X"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("Y"); label() = Some("Y"); updateCaseViewAndDataStructures() }, "Y"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricLabel = Some("Z"); label() = Some("Z"); updateCaseViewAndDataStructures() }, "Z")
          )
        ),
        td(style := "width:28%;", crub.rubric.fullPath),
        td(cls:="d-none d-sm-table-cell", remedies.take(remedies.size - 1).map(l => span(l, ", ")) ::: List(remedies.last)),
        td(cls := "text-right",
          button(cls := "btn btn-sm btn-secondary", `type` := "button",
            scalatags.JsDom.attrs.id := ("rmBut_" + crub.rubric.id + crub.repertoryAbbrev),
            style := "vertical-align: middle; display: inline-block",
            onclick := { (event: Event) => {
              event.stopPropagation()
              crub.rubricWeight = 1
              cRubrics = cRubrics.filter(_ != crub) // cRubrics.remove(cRubrics.indexOf(crub))
              $("#crub_" + crub.rubric.id + crub.repertoryAbbrev).remove()

              // Enable add-button in results, if removed symptom was in the displayed results list...
              $("#button_" + crub.repertoryAbbrev + "_" + crub.rubric.id).removeAttr("disabled")

              // If this was last case-rubric, clear case div
              if (cRubrics.size == 0)
                rmCaseDiv()

              updateCaseViewAndDataStructures()
            }
            }, "Remove")
        )
      )
    }

    def header() = {
      val repertoriseButton =
        button(cls:="btn btn-sm btn-primary", `type`:="button", data.toggle:="modal", data.target:="#caseAnalysisModal", style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            updateCaseViewAndDataStructures()

            // TODO: Ugly work-around to activate tooltips as recommended by Bootstrap documentation. :-(
            // TODO: Interestingly, it seems, it works without.  In fact, turning this on and then closing a HUGE repertorisation, slows down the browser dramatically!
            // js.eval("""$('[data-toggle="tooltip"]').tooltip();""")
          }},
          "Repertorise")
      val editDescrButton =
        button(cls:="btn btn-sm btn-secondary", id:="editDescrButton", `type`:="button", data.toggle:="modal", data.target:="#caseDescriptionModal", style:="display: none; margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            descr match {
              case Some(descr) =>
                $("#caseDescrId").`val`(descr.header)
                $("#caseDescrDescr").`val`(descr.description)
              case None => ;
            }
          }
          }, "Edit case description")
      val openNewCaseButton =
        button(cls:="btn btn-sm btn-secondary", id:="openNewCaseButton", `type`:="button", data.toggle:="modal", data.target:="#caseDescriptionModal", style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].removeAttribute("readonly")
            $("#caseDescrId").`val`("")
            $("#caseDescrDescr").`val`("")
          }},
          "Open new case")
      val closeCaseButton =
        button(cls:="btn btn-sm btn-secondary", id:="closeCaseButton", `type`:="button", style:="display: none; margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            for (crub <- cRubrics) {
              crub.rubricWeight = 1
              $("#crub_" + crub.rubric.id + crub.repertoryAbbrev).remove()
              // Enable add-button in results, if removed symptom was in the displayed results list...
              $("#button_" + crub.repertoryAbbrev + "_" + crub.rubric.id).removeAttr("disabled")
            }
            cRubrics = List()
            descr = None
            rmCaseDiv()
          }
          }, "Close case")
      val addToFileButton =
        button(cls:="btn btn-sm btn-secondary", id:="addToFileButton", `type`:="button", data.toggle:="modal", data.target:="#addToFileModal", disabled:=true, style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            updateCaseViewAndDataStructures()
          }},
          "Add case to file")

      getCookieData(dom.document.cookie, CookieFields.id.toString) match {
        case Some(_) =>
          if (descr != None) {
            div(
              b(id:="caseHeader", "Case '" + descr.get.header + "':"),
              editDescrButton,
              closeCaseButton,
              addToFileButton,
              repertoriseButton
            )
          }
          else {
            div(
              b(id:="caseHeader", "Case: "),
              editDescrButton,
              openNewCaseButton,
              closeCaseButton,
              addToFileButton,
              repertoriseButton
            )
          }
        case None =>
          div(
            b(id:="caseHeader", "Case: "),
            repertoriseButton
          )
      }
    }

    div(cls:="container-fluid",
      div(header),
      div(cls:="table-responsive",
        table(cls:="table table-striped table-sm table-bordered",
          thead(cls:="thead-dark", scalatags.JsDom.attrs.id:="caseTHead",
            th(attr("scope"):="col", "Weight"),
            th(attr("scope"):="col", "Rep."),
            th(attr("scope"):="col", "Label"),
            th(attr("scope"):="col", "Rubric"),
            th(cls:="d-none d-sm-table-cell", attr("scope"):="col",
              a(scalatags.JsDom.attrs.id:="caseSectionOfPage",
                cls:="underline", href:="#caseSectionOfPage",
                onclick:=((event: Event) => Repertorise.toggleRemedyFormat()),
                "Remedies")
            ),
            th(attr("scope"):="col", " ")
          ),
          tbody(scalatags.JsDom.attrs.id:="caseTBody",
            cRubrics
              .sortBy(cr => (cr.repertoryAbbrev + cr.rubric.fullPath))
              .map(crub => caseRow(crub)))
        )
      )
    )
  }
}
