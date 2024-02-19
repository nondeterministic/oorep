package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import dom.Event
import scalatags.JsDom
import scalatags.JsDom.all._
import io.circe.syntax._

import scala.scalajs.js
import scala.collection.mutable
import mutable.ListBuffer
import rx.Var
import rx.Ctx.Owner.Unsafe._
import org.multics.baueran.frep.shared
import org.multics.baueran.frep.shared.TopLevelUtilCode.getDocumentCsrfCookie
import org.multics.baueran.frep.shared.sec_frontend.AddToFileModal
import shared._
import shared.frontend.views.repertory.RepertoryView
import shared.Defs.{CookieFields, HeaderFields}
import shared.frontend.RemedyFormat.RemedyFormat
import shared.sec_frontend.FileModalCallbacks._

import scala.language.implicitConversions

object Case {

  var descr: Option[shared.Caze] = None
  var cRubrics: List[CaseRubric] = List()
  private val remedyScores = mutable.HashMap[String,Integer]()
  private var prevCase: Option[shared.Caze] = None

  private object SortCaseBy extends Enumeration {
    type SortCaseBy = Value
    val Weight = Value("Weight")
    val Abbrev = Value("Abbrev")
    val Label = Value("Label")
    val Path = Value("Path")
  }
  import SortCaseBy._
  private var sortCaseBy = Abbrev
  private val sortReverse = Var(false)
  sortReverse.triggerLater(updateCaseViewAndDataStructures())

  // This is not really necessary for proper functioning, but when deleting a case/file which is currently shown,
  // the user could, by pressing add or remove again, mess up the database as we're trying to write to a file/case,
  // or try to delete it, that no longer exists.
  // private var currOpenFileHeader: Option[String] = None
  private var currOpenFileId: Option[Int] = None

  object OpenNewCaseButton extends OorepHtmlButton {
    def getId() = "openNewCaseButton"

    def apply() = {
      button(cls := "btn btn-sm btn-secondary", id := getId(), `type` := "button", data.toggle := "modal", data.target := s"#${CaseModals.EditModal.getId()}",
        style := "margin-left:5px; margin-bottom: 5px;",
        onclick := { (_: Event) => {
          CaseModals.EditModal.CaseIdInput.setEditable()
          CaseModals.EditModal.CaseIdInput.setText("")
          CaseModals.EditModal.CaseDescriptionTextArea.setText("")
          CaseModals.EditModal.SubmitButton.disable()
        }
        },
        span(cls := "oi oi-document", title := "Create a new case", aria.hidden := "true"),
        " New...")
    }
  }

  object EditDescrButton extends OorepHtmlButton {
    def getId() = "editDescrButton"

    def apply() = {
      button(cls := "btn btn-sm btn-secondary", id := getId(), `type` := "button", data.toggle := "modal", data.target := s"#${CaseModals.EditModal.getId()}",
        style := "display: none; margin-left:5px; margin-bottom: 5px;",
        onclick := { (event: Event) =>
          descr match {
            case Some(descr) =>
              CaseModals.EditModal.CaseIdInput.setText(descr.header)
              CaseModals.EditModal.CaseDescriptionTextArea.setText(descr.description)
              CaseModals.EditModal.SubmitButton.disable()
            case None => ;
          }
        },
        span(cls := "oi oi-pencil", title := "Edit case description", aria.hidden := "true"),
        " Edit description..."
      )
    }
  }

  object CloseCaseButton extends OorepHtmlButton {
    def getId() = "closeCaseButton"

    def apply() = {
      button(cls := "btn btn-sm btn-secondary", id := getId(), `type` := "button", style := "display: none; margin-left:5px; margin-bottom: 5px;",
        onclick := { (_: Event) =>
          removeFromMemory()
        },
        span(cls := "oi oi-x", title := "Close case", aria.hidden := "true"),
        " Close"
      )
    }
  }

  object CloneCaseButton extends OorepHtmlButton {
    def getId() = "cloneCaseButton"

    def apply() = {
      button(cls := "btn btn-sm btn-secondary", id := getId(), `type` := "button", style := "display: none; margin-left:5px; margin-bottom: 5px;",
        onclick := { (event: Event) =>
          cloneCase()
        },
        span(cls := "oi oi-tags", title := "Clone case", aria.hidden := "true"),
        " Clone"
      )
    }
  }

  object AddToFileButton extends OorepHtmlButton {
    def getId() = "addToFileButton"

    def apply() = {
      button(cls := "btn btn-sm btn-secondary", id := getId(), `type` := "button", data.toggle := "modal", data.target := s"#${AddToFileModal.getId()}",
        disabled := true, style := "margin-left:5px; margin-bottom: 5px;",
        onclick := { (event: Event) =>
          updateCaseViewAndDataStructures()
          AddToFileModal.unselectAll()
        },
        span(cls := "oi oi-plus", title := "Add case to file", aria.hidden := "true"),
        " Add to file..."
      )
    }
  }

  object RepertoriseButton extends OorepHtmlButton {
    def getId() = "CaseRepertroiseButtonID_DoesntMatterAsItsNotUsed"

    def apply() = {
      button(cls := "btn btn-sm btn-primary", `type` := "button", data.toggle := "modal", data.target := s"#${CaseModals.RepertorisationModal.getId()}",
        style := "margin-left:5px; margin-bottom: 5px;",
        onclick := { (event: Event) => {
          updateCaseViewAndDataStructures()
        }},
        span(cls := "oi oi-grid-three-up", title := "Repertorise", aria.hidden := "true"),
        " Repertorise..."
      )
    }
  }

  object CaseHeader extends OorepHtmlElement {
    def getId() = "caseHeader"

    def setHeaderText(newHeaderText: String) = {
      getNode() match {
        case None => ;
        case Some(hdr) => hdr.textContent = newHeaderText
      }
    }

    def apply() = {
      def header = "CASE"

      getCookieData(dom.document.cookie, CookieFields.id.toString) match {
        case Some(_) =>
          if (descr != None) {
            div(
              b(id := getId(), s"$header '" + descr.get.header + "': "),
              EditDescrButton(),
              CloseCaseButton(),
              CloneCaseButton(),
              AddToFileButton(),
              RepertoriseButton()
            )
          }
          else {
            div(
              b(id := getId(), s"$header: "),
              EditDescrButton(),
              OpenNewCaseButton(),
              CloseCaseButton(),
              CloneCaseButton(),
              AddToFileButton(),
              RepertoriseButton()
            )
          }
        case None =>
          div(
            b(id := getId(), s"$header: "),
            RepertoriseButton()
          )
      }
    }
  }

  // ===== <HtmlRepresentation> =======================================================================================
  object HtmlRepresentation {
    object TableHead extends OorepHtmlElement {
      def getId() = "Case_caseSectionOfPage_34534jhdkfgfd"

      def apply() = {
        thead(cls := "thead-dark", scalatags.JsDom.attrs.id := getId(),
          th(attr("scope") := "col", "Weight"),
          th(attr("scope") := "col", "Rep."),
          th(attr("scope") := "col", "Label"),
          th(attr("scope") := "col", "Rubric"),
          th(cls := "d-none d-sm-table-cell", attr("scope") := "col",
            a(cls := "underline", href := s"#${getId()}", onclick := ((event: Event) => RepertoryView.toggleRemedyFormat()), "Remedies")
          ),
          th(attr("scope") := "col", " ")
        )
      }
    }
  }

  class HtmlRepresentation(remedyFormat: RemedyFormat) extends OorepHtmlElement {
    def getId() = "Case_HtmlRepresentation_3243jkdvjk34jkJKhk"

    class CaseRow(crub: CaseRubric) extends OorepHtmlElement {
      def getId() = "crub_" + crub.rubric.id + crub.repertoryAbbrev

      implicit def crToCR(cr: CaseRubric): BetterCaseRubric = new BetterCaseRubric(cr)

      val remedies = crub.getFormattedRemedyNames(remedyFormat)
      val weight = Var(crub.rubricWeight) // The weight label on the drop-down button, which needs to change automatically on new user choice
      weight.triggerLater {
        dom.document.getElementById(s"${getId()}_Button.Weight") match {
          case null => ;
          case button: dom.html.Button =>
            button.textContent = weight.now.toString
        }
      }

      // Same for label
      val label = Var(crub.rubricLabel)
      label.triggerLater {
        dom.document.getElementById(s"${getId()}_Button.Label") match {
          case null => ;
          case button: dom.html.Button =>
            button.textContent = label.now.getOrElse("")
        }
      }

      def apply() = {
        tr(scalatags.JsDom.attrs.id := getId(),
          td(
            button(`type` := "button", id := s"${getId()}_Button.Weight", cls := "btn dropdown-toggle btn-sm btn-secondary", style := "width:45px;", data.toggle := "dropdown", weight.now.toString),
            div(cls := "dropdown-menu",
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricWeight = 0; weight() = 0; updateCaseViewAndDataStructures() }, "0 (ignore)"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricWeight = 1; weight() = 1; updateCaseViewAndDataStructures() }, "1 (normal)"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricWeight = 2; weight() = 2; updateCaseViewAndDataStructures() }, "2 (important)"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricWeight = 3; weight() = 3; updateCaseViewAndDataStructures() }, "3 (very important)"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricWeight = 4; weight() = 4; updateCaseViewAndDataStructures() }, "4 (essential)")
            )
          ),
          td(crub.repertoryAbbrev),
          td(
            button(`type` := "button", id := s"${getId()}_Button.Label", cls := "btn dropdown-toggle btn-sm btn-secondary", style := "width:45px;", data.toggle := "dropdown", s"${label.now.getOrElse("")}"),
            div(cls := "dropdown-menu", style := "max-height:250px; overflow-y:auto;",
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = None; label() = None; updateCaseViewAndDataStructures() }, "none"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("A"); label() = Some("A"); updateCaseViewAndDataStructures() }, "A"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("B"); label() = Some("B"); updateCaseViewAndDataStructures() }, "B"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("C"); label() = Some("C"); updateCaseViewAndDataStructures() }, "C"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("D"); label() = Some("D"); updateCaseViewAndDataStructures() }, "D"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("E"); label() = Some("E"); updateCaseViewAndDataStructures() }, "E"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("F"); label() = Some("F"); updateCaseViewAndDataStructures() }, "F"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("G"); label() = Some("G"); updateCaseViewAndDataStructures() }, "G"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("H"); label() = Some("H"); updateCaseViewAndDataStructures() }, "H"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("I"); label() = Some("I"); updateCaseViewAndDataStructures() }, "I"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("J"); label() = Some("J"); updateCaseViewAndDataStructures() }, "J"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("K"); label() = Some("K"); updateCaseViewAndDataStructures() }, "K"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("L"); label() = Some("L"); updateCaseViewAndDataStructures() }, "L"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("M"); label() = Some("M"); updateCaseViewAndDataStructures() }, "M"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("N"); label() = Some("N"); updateCaseViewAndDataStructures() }, "N"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("O"); label() = Some("O"); updateCaseViewAndDataStructures() }, "O"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("P"); label() = Some("P"); updateCaseViewAndDataStructures() }, "P"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("Q"); label() = Some("Q"); updateCaseViewAndDataStructures() }, "Q"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("R"); label() = Some("R"); updateCaseViewAndDataStructures() }, "R"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("S"); label() = Some("S"); updateCaseViewAndDataStructures() }, "S"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("T"); label() = Some("T"); updateCaseViewAndDataStructures() }, "T"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("U"); label() = Some("U"); updateCaseViewAndDataStructures() }, "U"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("V"); label() = Some("V"); updateCaseViewAndDataStructures() }, "V"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("W"); label() = Some("W"); updateCaseViewAndDataStructures() }, "W"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("X"); label() = Some("X"); updateCaseViewAndDataStructures() }, "X"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("Y"); label() = Some("Y"); updateCaseViewAndDataStructures() }, "Y"),
              a(cls := "dropdown-item", href := s"#${HtmlRepresentation.TableHead.getId()}", onclick := { (event: Event) => crub.rubricLabel = Some("Z"); label() = Some("Z"); updateCaseViewAndDataStructures() }, "Z")
            )
          ),
          td(style := "width:28%;", crub.rubric.fullPath),
          td(cls := "d-none d-sm-table-cell", remedies.take(remedies.size - 1).map(l => span(l, ", ")) ::: List(remedies.last)),
          td(cls := "text-right", style := "white-space:nowrap;",
            button(cls := "btn btn-sm btn-secondary", `type` := "button",
              scalatags.JsDom.attrs.id := ("rmBut_" + crub.rubric.id + crub.repertoryAbbrev),
              style := "vertical-align: middle; display: inline-block",
              title := "Remove rubric",
              onclick := { (event: Event) => {
                event.stopPropagation()
                crub.rubricWeight = 1
                cRubrics = cRubrics.filter(_ != crub)
                dom.document.getElementById("crub_" + crub.rubric.id + crub.repertoryAbbrev) match {
                  case null => ;
                  case elem => elem.parentNode.removeChild(elem)
                }

                // Enable add-button in results, if removed symptom was in the displayed results list...
                dom.document.getElementById("button_" + crub.repertoryAbbrev + "_" + crub.rubric.id) match {
                  case null => ;
                  case elem => elem.asInstanceOf[dom.html.Button].removeAttribute("disabled")
                }

                // If this was last case-rubric, clear case div
                if (cRubrics.size == 0)
                  MainView.CaseDiv.empty()

                updateCaseViewAndDataStructures()
                MainView.toggleOnBeforeUnload()
              }
              }, b(raw("&nbsp;-&nbsp;")))
          )
        )
      }
    }

    object TableBody extends OorepHtmlElement {
      def getId() = "caseTBody"

      def apply() = {
        tbody(scalatags.JsDom.attrs.id := getId(),
          cRubrics
            .sortBy(cr => (cr.repertoryAbbrev + cr.rubric.fullPath))
            .map(crub => new CaseRow(crub)()))
      }
    }

    override def apply() = {
      updateCaseViewAndDataStructures()

      div(id:=getId(), cls := "container-fluid",
        div(CaseHeader()),
        div(cls := "table-responsive",
          table(cls := "table table-striped table-sm table-bordered",
            HtmlRepresentation.TableHead(),
            TableBody()
          )
        )
      )
    }
  }
  // ==== </HtmlRepresentation> =======================================================================================

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
  def removeFromMemory(): Unit = {
    for (crub <- cRubrics) {
      crub.rubricWeight = 1

      dom.document.getElementById("crub_" + crub.rubric.id + crub.repertoryAbbrev) match {
        case null => ;
        case elem => elem.parentNode.removeChild(elem)
      }

      // Enable add-button in results, if removed symptom was in the displayed results list...
      dom.document.getElementById("button_" + crub.repertoryAbbrev + "_" + crub.rubric.id) match {
        case null => ;
        case elem => elem.asInstanceOf[dom.html.Button].removeAttribute("disabled")
      }
    }
    cRubrics = List()
    descr = None
    MainView.CaseDiv.empty()
    MainView.toggleOnBeforeUnload()

    // TODO: Any of those also required?
    //    prevCase = None
    //    remedyScores.clear()
    //    updateCurrOpenFile(None)
    //    updateCaseViewAndDataStructures()
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def cloneCase(): Unit = {
    descr match {
      case Some(c) =>
        descr = None
        prevCase = None
        updateCurrOpenFile(None)

        MainView.CaseDiv.empty()
        MainView.toggleOnBeforeUnload()
        MainView.CaseDiv.append(new Case.HtmlRepresentation(RepertoryView._remedyFormat.now)().render)
        updateCaseViewAndDataStructures()
        updateCaseHeaderView()
      case None =>
        println("Case: cloneCase(): failed.")
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  def getCurrOpenFileId() = {
    currOpenFileId
  }

  // ------------------------------------------------------------------------------------------------------------------
  // Return the full remedy name for nameabbrev from a list of caseRubrics
  private def getFullNameFromCasRubrics(remedies: List[Remedy], nameabbrev: String) = {
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
      CaseModals.EditModal.CaseIdInput.setReadOnly()
    }
    else
      println(s"Case: updateCaseId with ID ${caseId} failed.")
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

            HttpRequest2("sec/add_caserubrics_to_case")
              .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
              .post(
                ("memberID" -> memberId.toString),
                ("caseID" -> descr.get.id.toString),
                ("caserubrics" -> diff.asJson.toString))
          }
          else if (prevCase.get.isSupersetOf(descr.get).length > 0) { // Delete the removed case rubrics in DB
            val diff = prevCase.get.isSupersetOf(descr.get)

            HttpRequest2("sec/del_caserubrics_from_case")
              .withMethod("DELETE")
              .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
              .withBody(
                ("memberID" -> memberId.toString),
                ("caseID" -> descr.get.id.toString),
                ("caserubrics" -> diff.asJson.toString))
              .send()
          }
          else if (descr.get.isEqualExceptUserDefinedValues(prevCase.get).length > 0) { // Update user defined case rubric values only in DB
            val diff = prevCase.get.isEqualExceptUserDefinedValues(descr.get) // These are the user-changed ones, which we'll need to update in the DB, too.

            HttpRequest2("sec/update_caserubrics_userdef")
              .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
              .put(
                ("memberID" -> memberId.toString),
                ("caseID" -> descr.get.id.toString),
                ("caserubrics" -> diff.asJson.toString))
          }
          else if (descr.get.description != prevCase.get.description) {
            HttpRequest2("sec/update_case_description")
              .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
              .put(
                ("memberID" -> memberId.toString),
                ("caseID" -> descr.get.id.toString),
                ("casedescription" -> descr.get.description))
          }
          else {
            println("Case: updateFileModalDataStructures(): NOT saving case, although something indicates it may have changed. " +
              "This shouldn't have happened, but previous saves should have taken care that no data-loss occurred.")
          }
        }
        else if (memberId == -1) {
          if (Notify.noAlertsVisible())
            new Notify("tempFeedbackAlert", "Saving case failed. Please log out and back in, and then try again.")
        }
      }

      // Delete not only view but entire case from DB, when user removed all of its rubrics...
      if (cRubrics.size == 0) {
        if (descr != None && descr.get.id != 0)
          HttpRequest2("sec/del_case")
            .withMethod("DELETE")
            .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
            .withBody(
              ("caseId" -> descr.get.id.toString()),
              ("memberId" -> memberId.toString()))
            .send()

        CaseModals.EditModal.CaseIdInput.setEditable()
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
    CaseModals.RepertorisationModal.TableHead.getNode() match {
      case None => println("Case: Redrawing of table header failed.")
      case Some(tableHead) =>
        CaseModals.RepertorisationModal.TableHead.rmAllChildren()
        if (caseUsesWeights)
          tableHead.appendChild(th(attr("scope") := "col", a(href := s"#${HtmlRepresentation.TableHead.getId()}", cls := "underline", onclick := { (event: Event) => sortCaseBy = Weight; sortReverse() = !sortReverse.now }, "W.")).render)
        tableHead.appendChild(th(attr("scope") := "col", a(href := s"#${HtmlRepresentation.TableHead.getId()}", cls := "underline", onclick := { (event: Event) => sortCaseBy = Abbrev; sortReverse() = !sortReverse.now }, "Rep.")).render)
        if (caseUsesLabels)
          tableHead.appendChild(th(attr("scope") := "col", a(href := s"#${HtmlRepresentation.TableHead.getId()}", cls := "underline", onclick := { (event: Event) => sortCaseBy = Label; sortReverse() = !sortReverse.now }, "L.")).render)
        tableHead.appendChild(th(attr("scope") := "col", a(href := s"#${HtmlRepresentation.TableHead.getId()}", cls := "underline", onclick := { (event: Event) => sortCaseBy = Path; sortReverse() = !sortReverse.now }, "Rubric")).render)
        val allRemediesInCase = cRubrics.map(_.weightedRemedies.map(_.remedy)).flatten.distinct
        remedyScores.toList.sortWith(_._2 > _._2).map(_._1).foreach(nameabbrev =>
          tableHead.appendChild(th(attr("scope") := "col",
            data.toggle := "tooltip", title := s"${getFullNameFromCasRubrics(allRemediesInCase, nameabbrev).getOrElse("LOOK-UP-ERROR")}",
            div(cls := "vertical-text", style := "width: 30px;",
              s"${nameabbrev} (${remedyScores.get(nameabbrev).get})")).render))
    }

    // Redraw table body
    implicit def stringToString(s: String) = new BetterString(s) // For 'shorten'.

    CaseModals.RepertorisationModal.TableBody.getNode() match {
      case None => println("Case: Redrawing of table body failed.")
      case Some(tableBody) =>
        CaseModals.RepertorisationModal.TableBody.rmAllChildren()
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
          })(if (sortReverse.now) Ordering[String].reverse else Ordering[String]))
        {
          val trId = cr.rubric.fullPath.replaceAll("[^A-Za-z0-9]", "") + "_" + cr.repertoryAbbrev

          // Construct table row entries
          val tableRowEntries = new ListBuffer[JsDom.TypedTag[dom.html.TableCell]]()
          if (caseUsesWeights)
            tableRowEntries += td(cr.rubricWeight.toString())
          tableRowEntries += td(cr.repertoryAbbrev)
          if (caseUsesLabels)
            tableRowEntries += td(cr.rubricLabel.getOrElse("").toString())
          tableRowEntries += td(style := "white-space: nowrap;", cr.rubric.fullPath.shorten)

          // Add table row
          tableBody.appendChild(
            tr(scalatags.JsDom.attrs.id := trId, tableRowEntries.toList).render)

          dom.document.getElementById(trId) match {
            case null => println("Case: Redrawing of table body failed. Can't update TRs...")
            case trr =>
              remedyScores.toList.sortWith(_._2 > _._2).map(_._1) foreach (abbrev => {
                if (cr.rubricWeight > 0 && cr.containsRemedyAbbrev(abbrev))
                  trr.appendChild(td(data.toggle := "tooltip", title := s"${cr.rubric.fullPath.shorten(40)}", "" + (cr.getRemedyWeight(abbrev) * cr.rubricWeight)).render)
                else
                  trr.appendChild(td(data.toggle := "tooltip", title := s"${cr.rubric.fullPath.shorten(40)}", " ").render)
              })
          }
        }
    }
  }

  def updateCaseHeaderView(): Unit = {
    getCookieData(dom.document.cookie, CookieFields.id.toString) match {
      // Not logged in...
      case None =>
        OpenNewCaseButton.hide()
        EditDescrButton.hide()
        CloseCaseButton.hide()
        CloneCaseButton.hide()
      // Logged in...
      case Some(_) =>
        descr match {
          // Case doesn't exist...
          case None =>
            OpenNewCaseButton.show()
            EditDescrButton.hide()
            CloseCaseButton.hide()
            CloneCaseButton.hide()
            AddToFileButton.disable()
          // Case exists...
          case Some(currCase) =>
            OpenNewCaseButton.hide()
            EditDescrButton.show()
            CloseCaseButton.show()
            CloneCaseButton.show()

            // Id is > 0, if the case has been already added to DB.  We disallow readding.
            if (currCase.id <= 0) {
              AddToFileButton.enable()
              CaseModals.EditModal.CaseIdInput.setEditable()
            }
            else {
              AddToFileButton.disable()
              CaseModals.EditModal.CaseIdInput.setReadOnly()
            }
        }
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  def containsUnsavedResults(): Boolean = {
    var unsaved = false

    if (size() > 0) {
      currOpenFileId match {
        case Some(_) => unsaved = false
        case None => unsaved = true
      }
    }

    unsaved
  }

}
