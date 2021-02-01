package org.multics.baueran.frep.shared.frontend

import scalatags.JsDom.all._
import org.scalajs.dom
import dom.Event
import dom.raw.{HTMLButtonElement, HTMLInputElement, Node}
import io.circe.parser.parse
import rx.{Rx, Var}
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._

import scala.collection.mutable
import scala.util.{Failure, Success}
import fr.hmil.roshttp.{BackendConfig, HttpRequest}
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.shared.Defs.{CookieFields, maxLengthOfSymptoms, maxNumberOfResults, maxNumberOfSymptoms}
import org.multics.baueran.frep.shared.sec_frontend.FileModalCallbacks.updateMemberFiles

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation._
import scala.scalajs.js.URIUtils._

@JSExportTopLevel("Repertorise")
object Repertorise {

  private def shareResultsModal = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="shareResultsModal",
      div(cls:="modal-dialog", role:="document",
        div(cls:="modal-content",
          div(cls:="modal-body",
            form(
              div(cls:="form-group",
                button(`type`:="button", cls:="close", data.dismiss:="modal", aria.label:="Close", span(aria.hidden:="true", "\u00d7")),
                label(`for`:="shareResultsModalLink", "To share the search results, copy and paste this link:"),
                input(cls:="form-control", id:="shareResultsModalLink", readonly:=true, value:=Rx(_currResultShareLink()))
              )
            )
          ),
          div(cls:="modal-footer",
            button(`type`:="button", cls:="btn btn-primary",
              onclick:= { (event: Event) =>
                dom.document.getElementById("shareResultsModalLink").asInstanceOf[dom.html.Input].select()
                dom.document.execCommand("copy")
              }, aria.label:="Copy to clipboard", "Copy to clipboard")
          )
        )
      )
    )
  }
  private val _currResultShareLink = Var(s"${serverUrl()}")
  private val _pageCache = new PageCache()
  private val _repertories = mutable.ArrayBuffer[Info]()
  private val _repertoryRemedyMap = mutable.HashMap[String,List[(String, String)]]()
  private val _selectedRepertory = Var("")
  private var _defaultRepertory = ""
  private var _showMaxSearchResultsAlert = true
  private var _showMultiOccurrences = false
  private val _remedyFormat = Var(RemedyFormat.Abbreviated)
  val _repertorisationResults: Var[Option[ResultsCaseRubrics]] = Var(None)
  private val _resultRemedyStats: Var[List[ResultsRemedyStats]] = Var(List())
  private var _loadingSpinner: Option[LoadingSpinner] = None
  private var _disclaimer: Option[Disclaimer] = None
  private val _cookiePopup = new CookiePopup("content")

  // ------------------------------------------------------------------------------------------------------------------
  def init(loadingSpinner: LoadingSpinner, disclaimer: Disclaimer) = {
    _loadingSpinner = Some(loadingSpinner)
    _disclaimer = Some(disclaimer)
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def redrawMultiOccurringRemedies(): Unit = {
    val multiRemedies = _resultRemedyStats.now.sortBy(-_.count)
    val multiOccurrenceDiv = dom.document.getElementById("multiOccurrenceDiv").asInstanceOf[dom.html.Element]

    if (multiOccurrenceDiv != null) {
      val collapseMultiOccurrences = dom.document.getElementById("collapseMultiOccurrences").asInstanceOf[dom.html.Element]
      if (collapseMultiOccurrences != null)
        multiOccurrenceDiv.removeChild(collapseMultiOccurrences)

      if (multiRemedies.length > 1) {
        multiOccurrenceDiv.appendChild(
          span(id := "collapseMultiOccurrences", cls := s"collapse ${if (_showMultiOccurrences) "show" else "hide"}",
            "(Multi-occurrences of remedies in results: ",
            multiRemedies
              .take(multiRemedies.size - 1)
              .map { case ResultsRemedyStats(nameabbrev, count, cumulativeWeight) => {
                span(
                  (s"${count}x"),
                  a(href := "#", onclick := { (event: Event) =>
                    doLookup(_pageCache.latest.abbrev, _pageCache.latest.symptom, None, Some(nameabbrev), 0)
                  }, nameabbrev),
                  ("(" + cumulativeWeight + "), ")
                )
              }
              },
            span(
              (s"${multiRemedies.last.count}x"),
              a(href := "#", onclick := { (event: Event) =>
                doLookup(_pageCache.latest.abbrev, _pageCache.latest.symptom, None, Some(multiRemedies.last.nameabbrev), 0)
              }, multiRemedies.last.nameabbrev),
              ("(" + multiRemedies.last.cumulativeweight + ")")
            ),
            ")").render
        )
      }
      else {
        multiOccurrenceDiv.appendChild(
          span(id := "collapseMultiOccurrences", cls := s"collapse ${if (_showMultiOccurrences) "show" else "hide"}",
            "(Multi-occurrences of remedies in results: None)").render)
      }
    }
  }

  _resultRemedyStats.triggerLater {
    redrawMultiOccurringRemedies()
  }

  _selectedRepertory.triggerLater {
    if (dom.document.getElementById("remedyDataList") != null) {
      val optionsDiv = dom.document.getElementById("remedyDataList").asInstanceOf[dom.html.Div]

      while (optionsDiv.childNodes.length > 0)
        optionsDiv.removeChild(optionsDiv.firstChild)

      for (remedyAbbrev <- _repertoryRemedyMap.get(_selectedRepertory.now).getOrElse(List.empty).map { case (shortname, _) => shortname }.sorted)
        optionsDiv.appendChild(option(value := s"$remedyAbbrev").render)
    }
  }

  _repertorisationResults.triggerLater(_repertorisationResults.now match {
    case Some(_) => showResults()
    case None => ;
  })
  _remedyFormat.triggerLater(_repertorisationResults.now match {
    case Some(_) => showResults(); redrawMultiOccurringRemedies()
    case None => ;
  })

  // ------------------------------------------------------------------------------------------------------------------
  // Render HTML for the results of a repertory lookup directly to page.
  def showResults(): Unit = {

    def resetContentView() = {
      val contentDiv = dom.document.getElementById("content").asInstanceOf[dom.html.Element]
      contentDiv.innerHTML = ""
      contentDiv.appendChild(apply().render)

      getCookieData(dom.document.cookie, CookieFields.id.toString) match {
        case Some(id) => updateMemberFiles(id.toInt)
        case None => ;
      }

      updateAvailableRepertoriesAndRemedies()
    }

    def resultRow(result: CaseRubric) = {
      implicit def crToCR(cr: CaseRubric) = new BetterCaseRubric(cr)

      val remedies = result.getFormattedRemedyNames(_remedyFormat.now)

      if (remedies.size > 0)
        tr(
          td(result.rubric.fullPath, style:="width:35%;"),
          td(remedies.take(remedies.size - 1).map(l => span(l, ", ")) ::: List(remedies.last)),
          td(cls := "text-right",
            button(cls := "btn btn-sm", `type` := "button", id := ("button_" + result.repertoryAbbrev + "_" + result.rubric.id),
              style := "vertical-align: middle; display: inline-block",
              (if (Case.cRubrics.filter(_.equalsIgnoreWeight(result)).size > 0) attr("disabled") := "disabled" else ""),
              onclick := { (event: Event) => {
                event.stopPropagation()
                Case.addRepertoryLookup(result)
                Case.updateCaseViewAndDataStructures()
                dom.document.getElementById("button_" + result.repertoryAbbrev + "_" + result.rubric.id).asInstanceOf[HTMLButtonElement].setAttribute("disabled", "1")
                showCase()
              }
              }, "Add")
          )
        )
      else
        tr(
          td(result.rubric.fullPath, style := "width:35%;"),
          td(),
          td()
        )
    }

    resetContentView()

    _repertorisationResults.now match {
      case Some(ResultsCaseRubrics(_, totalNumberOfResults, totalNumberOfPages, currPage, results)) if (results.size > 0) => {
        dom.document.getElementById("resultStatus").innerHTML = ""
        dom.document.getElementById("resultStatus").appendChild(
          div(scalatags.JsDom.attrs.id := "multiOccurrenceDiv", cls := "alert alert-secondary", role := "alert",
            button(`type`:="button", cls:="close", data.toggle:="collapse", data.target:="#collapseMultiOccurrences",
              onclick := { (_: Event) =>
                val toggleButton =
                  dom.document.getElementById("collapseMultiOccurrencesButton").asInstanceOf[dom.html.Span]
                if (toggleButton.getAttribute("class") == "oi oi-chevron-left") {
                  toggleButton.setAttribute("class", "oi oi-chevron-bottom")
                  toggleButton.setAttribute("title", "Show less")
                  _showMultiOccurrences = true
                } else {
                  toggleButton.setAttribute("class", "oi oi-chevron-left")
                  toggleButton.setAttribute("title", "Show more...")
                  _showMultiOccurrences = false
                }
              },
              if (!_showMultiOccurrences)
                span(aria.hidden:="true", id:="collapseMultiOccurrencesButton", cls:="oi oi-chevron-left", style:="font-size: 14px;", title:="Show more...")
              else
                span(aria.hidden:="true", id:="collapseMultiOccurrencesButton", cls:="oi oi-chevron-bottom", style:="font-size: 14px;", title:="Show less")
            ),
            b({
              var searchStatsString = s"${totalNumberOfResults} rubrics for "

              if (_pageCache.latest.symptom.trim.length > 0) {
                searchStatsString += s"'${_pageCache.latest.symptom}' "

                _pageCache.latest.remedy match {
                  case None => ;
                  case Some(remedy) =>
                    searchStatsString += s"containing '${remedy}'"
                }
              }
              else
                searchStatsString += s"'${_pageCache.latest.remedy.getOrElse("")}'"

              if (_pageCache.latest.minWeight > 0)
                searchStatsString += s" with min. weight >= ${_pageCache.latest.minWeight}"

              searchStatsString += s". Showing page ${currPage + 1} of ${totalNumberOfPages} ("
              searchStatsString
            }),
            a(href:="#", data.toggle:="modal", data.dismiss:="modal", data.target:="#shareResultsModal", b("share")),
            b("). ")
            ).render
        )

        dom.document.getElementById("resultDiv").innerHTML = ""
        dom.document.getElementById("resultDiv").appendChild(
          div(cls := "table-responsive",
            table(cls := "table table-striped table-sm table-bordered",
              thead(cls := "thead-dark", scalatags.JsDom.attrs.id := "resultsTHead",
                th(attr("scope") := "col", "Rubric"),
                th(attr("scope") := "col",
                  a(scalatags.JsDom.attrs.id := "remediesFormatButton",
                    cls := "underline", href := "#", style := "color:white;",
                    onclick := ((event: Event) => toggleRemedyFormat()),
                    "Remedies")
                ),
                th(attr("scope") := "col", " ")
              ),
              tbody(scalatags.JsDom.attrs.id := "resultsTBody")
            )
          ).render
        )

        dom.document.getElementById("paginationDiv").innerHTML = ""
        if (totalNumberOfPages > 1) {
          val pg = new Paginator(totalNumberOfPages, currPage + 1, 5).getPagination()
          val htmlPg = new PaginatorHtml(pg)

          dom.document.getElementById("paginationDiv")
            .appendChild(
              htmlPg.toHtml(_pageCache.latest.abbrev, _pageCache.latest.symptom, currPage, _pageCache.latest.remedy, _pageCache.latest.minWeight, doLookup)
                .render)
        }
      }
      case _ => ;
    }

    // Display potentially useful hint, when max. number of search results was returned.
    _repertorisationResults.now match {
      case Some(ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, _, results)) => {
        // If the total number of results matches the total number of available rubrics in a repertory, the user either entered "*"
        // or, in fact, the repertory is a so called small repertory, which means, we show everything...
        if (_showMaxSearchResultsAlert && totalNumberOfRepertoryRubrics == totalNumberOfResults) {
          dom.document.getElementById("resultStatus").appendChild(
            div(cls := "alert alert-success", role := "alert",
              button(`type` := "button", cls := "close", data.dismiss := "alert", onclick := { (_: Event) => _showMaxSearchResultsAlert = false },
                span(aria.hidden := "true", raw("&times;"))),
                b(s"Showing ALL available rubrics, because this repertory only has ${totalNumberOfRepertoryRubrics.toString} rubrics in total.")
            ).render)
        }
        else if (_showMaxSearchResultsAlert && (results.size >= maxNumberOfResults || totalNumberOfPages > 1)) {
          val fullPathWords = results.map(cr => cr.rubric.fullPath.split("[, ]+").filter(_.length > 0).map(_.trim())).flatten
          val pathWords = results.map(cr => cr.rubric.path.getOrElse("").split("[, ]+").filter(_.length > 0).map(_.trim())).flatten
          val textWords = results.map(cr => cr.rubric.textt.getOrElse("").split("[, ]+").filter(_.length > 0).map(_.trim())).flatten

          // Yields a sequence like [ ("pain", 130), ("abdomen", 50), ... ]
          val sortedResultOccurrences =
            (pathWords ::: textWords ::: fullPathWords).map(_.replaceAll("[^A-Za-z0-9äÄöÖüÜ]", "")).sortWith(_ > _)
              .groupBy(identity).mapValues(_.size)
              .toSeq.sortWith(_._2 > _._2)

          // Filter out all those results, which were actually desired via positive search terms entered by the user
          val searchTerms = new SearchTerms(_pageCache.latest.symptom)
          val posTerms = searchTerms.positive
          val filteredSortedResultOccurrences =
            sortedResultOccurrences
              .filter { case (t, _) =>
                t.length() > 3 && !(posTerms.exists(pt => searchTerms.isWordInX(pt, Some(t))))
              }

          // If there are some left after filtering, suggest to user to exclude top-most result from a next search.
          if (filteredSortedResultOccurrences.length > 2) {
            val newSearchTerms = searchTerms.positive.mkString(", ") + {
              if (searchTerms.negative.length > 0)
                ", " + searchTerms.negative.map("-" + _).mkString(", ")
              else ""
            } + s", -${filteredSortedResultOccurrences.head._1.toLowerCase.take(6)}*"

            dom.document.getElementById("resultStatus").appendChild(
              div(cls := "alert alert-warning", role := "alert",
                button(`type` := "button", cls := "close", data.dismiss := "alert", onclick := { (_: Event) => _showMaxSearchResultsAlert = false },
                  span(aria.hidden := "true", raw("&times;"))),
                if (posTerms.length > 0)
                  b(s"High number of results. Maybe try narrowing your search using '-', like '",
                    a(href := "#", onclick := { (_: Event) => onSymptomLinkClicked(newSearchTerms) }, newSearchTerms),
                    "'."
                  )
                else
                  b(s"High number of results. Maybe try narrowing your search by also entering some symptoms.")
              ).render)
          }
        }
      }
      case _ => ;
    }

    _repertorisationResults.now match {
      case Some(ResultsCaseRubrics(_, _, _, _, results)) =>
        results.foreach(result => dom.document.getElementById("resultsTBody").appendChild(resultRow(result).render))
      case _ => ;
    }

    Case.updateCaseHeaderView()
  }

  // ------------------------------------------------------------------------------------------------------------------
  def toggleRemedyFormat() = {
    if (_remedyFormat.now == RemedyFormat.Fullname)
      _remedyFormat() = RemedyFormat.Abbreviated
    else
      _remedyFormat() = RemedyFormat.Fullname

    if (Case.size() > 0)
      showCase()
    else
      println("Repertorise: toggleRemedyFormat: Case.size() == 0.")
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def showCase() = {
    Case.rmCaseDiv()
    dom.document.getElementById("caseDiv").appendChild(Case.toHTML(_remedyFormat.now).render)
    Case.updateCaseViewAndDataStructures()
    Case.updateCaseHeaderView()
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def advancedSearchOptionsVisible() = {
    dom.document.getElementById("advancedSearchControlsDiv").childNodes.length > 0
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSymptomLinkClicked(symptom: String) = {
    doLookup(_selectedRepertory.now, symptom, None, None, 0)
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSymptomEntered(): Unit = {
    val remedyQuery = dom.document.getElementById("inputRemedy") match {
      case null => None
      case element => element.asInstanceOf[HTMLInputElement].value.trim match {
        case "" => None
        case otherwise => Some(otherwise)
      }
    }
    val remedyMinWeight = dom.document.getElementById("minWeightDropdown") match {
      case null => 0
      case element => element.asInstanceOf[HTMLButtonElement].textContent.trim match {
        case "" => 0
        case otherwise => otherwise.toInt
      }
    }
    val symptom = dom.document.getElementById("inputField").asInstanceOf[HTMLInputElement].value

    doLookup(_selectedRepertory.now, symptom, None, remedyQuery, remedyMinWeight)
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSymptomListRedoPressed() = {
    dom.document.getElementById("inputField").asInstanceOf[dom.html.Input].value = _pageCache.latest.symptom

    _pageCache.latest.remedy match {
      case Some(_) => onShowAdvancedSearchOptionsMainView(true, false)
      case _ => {
        if (_pageCache.latest.minWeight > 0)
          onShowAdvancedSearchOptionsMainView(true, false)
      }
    }

    dom.document.getElementById("inputField").asInstanceOf[HTMLInputElement].focus()
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onShowAdvancedSearchOptionsMainView(restorePreviousValues: Boolean, landingPageIsCurrentView: Boolean): Unit = {
    val parentDiv = dom.document.getElementById("advancedSearchControlsDiv").asInstanceOf[dom.html.Div]
    val weightDropDownsDiv = div(id:="weightDropDownsDiv", cls:="dropdown-menu")

    dom.document.getElementById("advancedSearchControlsContent") match {
      case null =>
        parentDiv.appendChild(
          div(id := "advancedSearchControlsContent", cls := "row", style := "margin-top:15px;",
            div(cls := "col-md-auto my-auto",
              "Remedy:"),
            div(cls := "col",
              input(cls := "form-control", `id` := "inputRemedy", list := "remedyDataList",
                onkeydown := { (event: dom.KeyboardEvent) =>
                  if (event.keyCode == 13) {
                    event.stopPropagation()
                    onSymptomEntered()
                  }
                }, `placeholder` := "Enter a remedy (for example: Sil.)"),
              datalist(`id` := "remedyDataList")
            ),
            div(cls := "col-md-auto my-auto",
              "Min. weight:"),
            div(cls := "col-sm-2",
              div(id := "weightDropDowns", cls := "dropdown show",
                button(id := "minWeightDropdown", cls := "btn dropdown-toggle", `type` := "button", data.toggle := "dropdown", if (restorePreviousValues) _pageCache.latest.minWeight.toString else "0"),
                weightDropDownsDiv
              )
            )
          ).render)
      case _ => ;
    }

    // Set remedy input string, if one was previously submitted, and redo-button was pressed
    if (restorePreviousValues) {
      _pageCache.latest.remedy match {
        case Some(remedyAbbrev) =>
          dom.document.getElementById("inputRemedy").asInstanceOf[dom.html.Input].value = remedyAbbrev
        case None => ;
      }
    }

    // Add currently selected repertorys' remedies for search
    val optionsDiv = dom.document.getElementById("remedyDataList").asInstanceOf[dom.html.Div]

    while (optionsDiv.childNodes.length > 0)
      optionsDiv.removeChild(optionsDiv.firstChild)

    for (remedyAbbrev <- _repertoryRemedyMap.get(_selectedRepertory.now).getOrElse(List.empty).map { case (shortname, _) => shortname }.sorted)
      optionsDiv.appendChild(option(value:=s"$remedyAbbrev").render)

    // Add possible weights for search (4 is only in Hering's)
    for (i <- 0 to 4) {
      dom.document.getElementById("weightDropDownsDiv").asInstanceOf[dom.html.Div].appendChild(
        a(cls:="dropdown-item", href:="#",
          onclick := { (_: Event) =>
            dom.document.getElementById("minWeightDropdown").asInstanceOf[dom.html.Button].textContent = s"$i" },
          i).render
      )
    }

    dom.document.getElementById("buttonMainViewAdvancedSearch") match {
      case null => ;
      case advancedButton =>
        val basicButton = {
          if (landingPageIsCurrentView)
            button(id:="buttonMainViewBasicSearch", cls:="btn", style:="width: 120px;", `type`:="button",
              onclick := { (event: Event) =>
                onHideAdvancedSearchOptionsMainView(event, landingPageIsCurrentView)
              }, "Basic"
            ).render
          else
            button(id:="buttonMainViewBasicSearch", cls:="btn", style := "width: 70px;margin-right:5px;", `type`:="button",
              onclick := { (event: Event) =>
                onHideAdvancedSearchOptionsMainView(event, landingPageIsCurrentView)
              },
              span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true")
            ).render
        }
        val buttonDiv = dom.document.getElementById("mainViewSearchButtons").asInstanceOf[dom.html.Div]
        buttonDiv.replaceChild(basicButton, advancedButton.asInstanceOf[dom.html.Button])
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onHideAdvancedSearchOptionsMainView(event: Event, landingPageView: Boolean): Node = {
    event.stopPropagation()
    val theDiv = dom.document.getElementById("advancedSearchControlsDiv").asInstanceOf[dom.html.Div]

    while (theDiv.childNodes.length > 0)
      theDiv.removeChild(theDiv.firstChild)

    val basicButton = dom.document.getElementById("buttonMainViewBasicSearch").asInstanceOf[dom.html.Button]
    val advancedButton = {
      if (landingPageView)
        button(id:="buttonMainViewAdvancedSearch", cls:="btn", style:="width: 120px;", `type`:="button",
          onclick := { (event: Event) =>
            event.stopPropagation()
            onShowAdvancedSearchOptionsMainView(false, landingPageView)
          }, "Advanced...").render
      else
        button(`id`:="buttonMainViewAdvancedSearch", cls := "btn", style := "width: 70px;margin-right:5px;", `type` := "button",
          onclick := { (event: Event) =>
            event.stopPropagation()
            onShowAdvancedSearchOptionsMainView(false, landingPageView)
          },
          span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true")).render
    }
    val buttonDiv = dom.document.getElementById("mainViewSearchButtons").asInstanceOf[dom.html.Div]
    buttonDiv.replaceChild(advancedButton, basicButton)
  }

  @JSExport("doLookup")
  def jsDoLookup(abbrev: String, symptom: String, page: Int, remedyString: String, minWeight: Int) = {
    _selectedRepertory() = abbrev

    doLookup(abbrev,
      symptom,
      if (page > 0) Some(page) else None,
      if (remedyString.length > 0) Some(remedyString) else None,
      if (minWeight > 0) minWeight else 0)
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def doLookup(abbrev: String, symptom: String, pageOpt: Option[Int], remedyStringOpt: Option[String], minWeight: Int): Unit = {
    val page = math.max(pageOpt.getOrElse(0), 0)
    val remedyString = remedyStringOpt.getOrElse("")

    def showRepertorisationResults(results: ResultsCaseRubrics, remedyStats: List[ResultsRemedyStats]): Unit = {
      _pageCache.addPage(CachePage(abbrev, symptom, remedyStringOpt, minWeight, results, remedyStats))

      _currResultShareLink() = encodeURI(
        s"${serverUrl()}/show?repertory=${abbrev}&symptom=${symptom}&page=${(pageOpt.getOrElse(0) + 1).toString}&remedyString=${remedyStringOpt.getOrElse("")}&minWeight=${minWeight.toString}"
      )

      _repertorisationResults() = None
      _repertorisationResults.recalc()
      _repertorisationResults() = Some(results)

      _resultRemedyStats() = List.empty
      _resultRemedyStats.recalc()
      _resultRemedyStats() = remedyStats

      if (Case.size() > 0)
        showCase()

      dom.document.body.style.cursor = "default"
    }

    def showRepertorisationResultsError(symptom: String, remedyString: String, minWeight: Int): Unit = {
      val searchTerms = new SearchTerms(symptom)
      val longPosTerms = searchTerms.positive.map(_.replace("*", "")).filter(_.length() > 6)
      val longNegTerms = searchTerms.negative
      val resultStatusDiv = dom.document.getElementById("resultStatus")

      dom.document.body.style.cursor = "default"
      resultStatusDiv.innerHTML = ""

      if (searchTerms.negative.length + searchTerms.positive.length >= maxNumberOfSymptoms) {
        resultStatusDiv.appendChild(
          div(cls:="alert alert-danger", role:="alert",
            b(s"You cannot enter more than ${maxNumberOfSymptoms} symptoms.")).render)
        return
      }

      if (searchTerms.positive.length == 0 && searchTerms.negative.length == 0 && remedyString.length > 0) {
        resultStatusDiv.appendChild(
          div(cls:="alert alert-danger", role:="alert",
            b(s"Remedy '${remedyString}' does not exist in this repertory (or min. weight too high).")).render)
        return
      }

      if (searchTerms.positive.length == 0) {
        resultStatusDiv.appendChild(
          div(cls:="alert alert-danger", role:="alert",
            b(s"No results. You must enter some symptoms to search for.")).render)
        return
      }

      val tmpErrorMessage1 = s"Perhaps try a different repertory; or use wild-card search, like '"
      val tmpErrorMessage2 = {
        if (longPosTerms.length > 0)
          longPosTerms.map(t => t.take(5) + "*").mkString(", ")
        else
          searchTerms.positive.map(_.replace("*", "")).map(_ + "*").mkString(", ")
      } + {
        if (longNegTerms.length > 0)
          ", " + longNegTerms.map("-" + _).mkString(", ")
        else
          ""
      }

      resultStatusDiv.appendChild(
        div(cls:="alert alert-danger", role:="alert",
          b(
            {
              var fullErrorMessage = "No results returned for "
              if (symptom.trim.length > 0) {
                fullErrorMessage += s"'${symptom}' "

                remedyString match {
                  case "" => ;
                  case remedy =>
                    fullErrorMessage += s"containing '${remedy}'"
                }
              }
              else
                fullErrorMessage += s"'${remedyString}'"

              if (minWeight > 0)
                fullErrorMessage += s" with min. weight >= ${minWeight}"

              if (searchTerms.positive.length > 0)
                fullErrorMessage += s". $tmpErrorMessage1"

              fullErrorMessage
            },
            a(href:="#", onclick:={ (_: Event) => onSymptomLinkClicked(tmpErrorMessage2) }, tmpErrorMessage2), "'."
          )
        ).render)
    }

    def getPageFromBackend(abbrev: String, symptom: String, remedyString: String, minWeight: Int, page: Int) = {
      val cachedRemedies = _pageCache.getRemedies(abbrev, symptom, if (remedyString.length == 0) None else Some(remedyString), minWeight)
      val getRemedies = if (cachedRemedies.length == 0) "1" else "0"

      val req = HttpRequest(s"${serverUrl()}/${apiPrefix()}/lookup")
      val serverResponseFuture = req
        .withQueryParameters(
          ("symptom", symptom),
          ("repertory", abbrev),
          ("page", page.toString),
          ("remedyString", remedyString),
          ("minWeight", minWeight.toString),
          ("getRemedies", getRemedies)
        )
        .withBackendConfig(BackendConfig(
          // 1 MB maxChunkSize; see also build.sbt where the same is set for the Akka backend!
          // It only works, so long as server's chunks are not larger than maxChunkSize below,
          // or if the reply fits well within one chunk and no second, third, etc. is sent at
          // all.  An interesting test case is "schmerz*" in kent-de as this is the longest
          // reply, I have found from the backend yet.  So, it used to crash on "schmerz*" in
          // kent-de, if the chunk size was too small.
          maxChunkSize = 1048576
        ))
        .send()
        .onComplete({
          case response: Success[SimpleHttpResponse] => {
            parse(response.get.body) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[(ResultsCaseRubrics, List[ResultsRemedyStats])] match {
                  case Right((rcr, remedyStats)) => {
                    cachedRemedies match {
                      case Nil => {
                        if (getRemedies == "1")
                          _resultRemedyStats() = remedyStats
                        showRepertorisationResults(rcr, remedyStats)
                      }
                      case cachedRemedies =>
                        showRepertorisationResults(rcr, cachedRemedies)
                    }
                  }
                  case Left(err) =>
                    println(s"Parsing of lookup as RepertoryLookup failed: $err")
                }
              }
              case Left(err) =>
                println(s"Parsing of lookup failed (is it JSON?): $err")
            }
          }
          case _: Failure[SimpleHttpResponse] =>
            // If no results were found, either tell the user in the input screen,
            // or, if search was invoked via static /show-link (i.e., there is no input screen), then display
            // clean error page.
            _loadingSpinner match {
              case Some(spinner) if (spinner.isVisible()) =>
                val errorMessage = s"ERROR: Lookup failed. Perhaps URL malformed, repertory does not exist or no results for given symptoms. " +
                  s"SUGGESTED SOLUTION: Go directly to https://www.oorep.com/ instead and try again!"
                dom.document.location.replace(s"${serverUrl()}/${apiPrefix()}/displayErrorPage?message=${encodeURI(errorMessage)}")
              case _ =>
                showRepertorisationResultsError(symptom, remedyString, minWeight)
            }
        })
    }

    // TODO: Right now, we do additional error- and sanity checking only when the app wasn't called via /show direct link.
    // That is, if the user uses a shared-link where the page is not yet built (i.e., where resultStatusDiv == null, we
    // can't sensibly display the additional error messages. We rely on the backend not allow too wild imputs and fail2ban,
    // of course.  (If we simply don't check for existence of said div here, then the app simply crashes upon wrong
    // direct-/shared-links, which isn't nice.)

    dom.document.getElementById("resultStatus") match {
      case null => ;
      case resultStatusDiv =>
        if (abbrev.length == 0) {
          resultStatusDiv.innerHTML = ""
          resultStatusDiv.appendChild(
            div(cls := "alert alert-danger", role := "alert",
              b("No results. Make sure a repertory is selected.")).render)
          return
        }

        if (symptom.trim.replaceAll("[^A-Za-z0-9äüöÄÜÖß]", "").length == 0 && remedyString.length == 0) {
          resultStatusDiv.innerHTML = ""
          resultStatusDiv.appendChild(
            div(cls := "alert alert-danger", role := "alert",
              b("No results. Enter some symptoms and/or a remedy.")).render)
          return
        }

        if (symptom.trim.replaceAll("[^A-Za-z0-9äüöÄÜÖß]", "").length <= 3 && remedyString.length == 0) {
          resultStatusDiv.innerHTML = ""
          resultStatusDiv.appendChild(
            div(cls := "alert alert-danger", role := "alert",
              b("No results returned for '" + symptom + "'. " +
                "Make sure the symptom input is not too short or to enter a remedy.")).render)
          return
        }

        if (symptom.length() >= maxLengthOfSymptoms) {
          resultStatusDiv.innerHTML = ""
          resultStatusDiv.appendChild(
            div(cls := "alert alert-danger", role := "alert",
              b(s"Input must not exceed $maxLengthOfSymptoms characters in length.")).render)
          return
        }
    }

    dom.document.body.style.cursor = "wait"

    _pageCache.getPage(abbrev, symptom, if (remedyString.length == 0) None else Some(remedyString), minWeight, page) match {
      case Some(cachedPage) =>
        showRepertorisationResults(cachedPage.content, cachedPage.remedies)
      case None =>
        getPageFromBackend(abbrev, symptom, remedyString, minWeight, page)
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def updateAvailableRepertoriesAndRemedies() = {
    if (_repertoryRemedyMap.size == 0) {
      println("INFO: Updating remedy and repertory information from backend")
      HttpRequest(s"${serverUrl()}/${apiPrefix()}/available_rems_and_reps")
        .send()
        .onComplete({
          case response: Success[SimpleHttpResponse] => {
            parse(response.get.body) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[(List[RemedyAndItsRepertories], List[Info])] match {
                  case Right((repertoryRemedies, repertoryInfo)) => {

                    // Update list of available repertories
                    _repertories.addAll(repertoryInfo)

                    // Update the default repertory, if one was transmitted from backend (which should ALWAYS be the case)
                    _repertories.toList.filter(_.access == RepAccess.Default) match {
                      case rep :: Nil => _defaultRepertory = rep.abbrev
                      case _ => println("WARNING: Not setting default repertory; none was transmitted")
                    }

                    // A huge map like [ ("bogsk" -> [ ("Caust.", "Causticum"), ("Ars.", "Arsenicum Album"), ... ]), ("publicum" -> ...), ... ]
                    // TODO: This could be sped up further by iterating ONLY ONCE, and then using if-conditions to see which remedy needs to be added to which map element.
                    // TODO: This should be used to display long remedy names instead of transmitting them for every new lookup of a symptom. Will be MUCH faster, I suspect.
                    for (currRep <- _repertories) {
                      val remedies = repertoryRemedies.collect {
                        case RemedyAndItsRepertories(remedyAbbrev, remedyLong, repertoryAbbrevs) if repertoryAbbrevs.split(",").contains(currRep.abbrev) =>
                          (remedyAbbrev, remedyLong)
                      }
                      _repertoryRemedyMap.put(currRep.abbrev, remedies)
                    }

                    // Update available repertories in repertory dropdown button
                    if (dom.document.getElementById("repSelectionDropDown").childNodes.length == 0) {
                      _repertories
                        .sortBy(_.abbrev)
                        .foreach {
                          case currRep => {
                            dom.document.getElementById("repSelectionDropDown")
                              .appendChild(a(cls := "dropdown-item", href := "#", data.value := currRep.abbrev,
                                onclick := { (event: Event) =>
                                  _selectedRepertory() = currRep.abbrev
                                  dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
                                }, s"${currRep.abbrev} - ${currRep.displaytitle.getOrElse("")}").render)

                            if (_selectedRepertory.now.length > 0)
                              dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
                            else {
                              _selectedRepertory() = _defaultRepertory
                              dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
                            }
                          }
                        }
                    }
                  }
                  case Left(t) => println("Parsing of available repertories failed: " + t)
                }
              }
              case Left(_) => println("Parsing of available repertories failed (is it JSON?).")
            }
          }
          case error: Failure[SimpleHttpResponse] => println("Available repertories failed: " + error.toString)
        })
    }
    else {
      println("INFO: Updating repertory information from RAM")
      for (currRep <- _repertories.toList.sortBy(_.abbrev)) {
        dom.document.getElementById("repSelectionDropDown")
          .appendChild(a(cls := "dropdown-item", href := "#", data.value := currRep.abbrev,
            onclick := { (event: Event) =>
              _selectedRepertory() = currRep.abbrev
              dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
            }, s"${currRep.abbrev} - ${currRep.displaytitle.getOrElse("")}").render)

        if (_selectedRepertory.now.length > 0)
          dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
        else {
          _selectedRepertory() = _defaultRepertory
          dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
        }
      }
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  def apply() = {
    // Make sure, the navbar is visible at this stage
    dom.document.getElementById("nav_bar") match {
      case null => ;
      case navBar => navBar.classList.remove("d-none")
    }

    // Make sure, the loading animation is gone at this stage
    _loadingSpinner match {
      case None => ;
      case Some(spinner) => spinner.remove()
    }

    // Make sure, the disclaimer is shown at this stage
    _disclaimer match {
      case None => ;
      case Some(disclaimer) => disclaimer.show()
    }

    // Request user acceptance for cookies at this stage, if it hasn't indicated acceptance, yet
    _cookiePopup.add()

    // From here downwards is the actual repertorisation view...
    val ulRepertorySelection =
      div(cls:="dropdown col-md-2", style := "margin-top:20px;",
        button(`type`:="button",
          style:="overflow: hidden;",
          cls:="btn btn-block dropdown-toggle",
          data.toggle:="dropdown",
          `id`:="repSelectionDropDownButton",
          "Repertories"),
        div(cls:="dropdown-menu", `id`:="repSelectionDropDown")
      )

    val myHTML =
      // Fresh page...
      if (_repertorisationResults.now == None && Case.size() == 0) {
        div(cls := "container-fluid",
          div(cls := "container-fluid text-center",
            // The h1-tag here is apparently needed for SEO, cf.
            // https://stackoverflow.com/questions/665037/replacing-h1-text-with-a-logo-image-best-method-for-seo-and-accessibility
            h1(cls:="col-sm-12 text-center", img(src:=s"${serverUrl()}/assets/html/img/logo_small.png", alt:="OOREP - open online repertory of homeopathy")),
            div(cls := "row",
              div(cls := "col-sm-1"),
              div(cls := "row col-sm-10",
                ulRepertorySelection,
                div(cls := "col", style:="margin-top:20px;",
                  input(cls := "form-control", `id` := "inputField",
                    onkeydown := { (event: dom.KeyboardEvent) =>
                      if (event.keyCode == 13) {
                        event.stopPropagation()
                        onSymptomEntered()
                      }
                    }, `placeholder` := "Enter a symptom (for example: head, pain, left)")
                ),
              ),
              div(cls := "col-sm-1")
            ),
            // We copy above div's, because this is as wide as the search bar. It will be (de-) populated on demand.
            div(cls := "row",
              div(cls := "col-sm-1"),
              div(cls := "row col-sm-10",
                div(cls:="col-sm-2"),
                div(cls := "col-sm-9", id:="advancedSearchControlsDiv"),
                div(cls:="col-sm-1")
              )
            ),
            div(id:="mainViewSearchButtons", cls:="col-sm-12 text-center", style:="margin-top:20px;",
              button(cls := "btn btn-primary", style := "width: 120px; margin-right:5px;", `type` := "button",
                onclick := { (event: Event) =>
                  event.stopPropagation()
                  onSymptomEntered()
                }, "Find"),
              button(id:="buttonMainViewAdvancedSearch", cls := "btn", style := "width: 120px;", `type` := "button",
                onclick := { (event: Event) =>
                  event.stopPropagation()
                  onShowAdvancedSearchOptionsMainView(false, true)
                }, "Advanced...")
            )
          ),
          div(cls := "container-fluid", style := "margin-top: 23px;", id := "resultStatus"),
          div(cls := "container-fluid", id := "resultDiv"),
          div(cls := "container-fluid", id := "paginationDiv"),
          div(cls := "span12", id := "caseDiv", {
            if (Case.size() > 0)
              Case.toHTML(_remedyFormat.now)
            else ""
          })
        )
      }
      // Page with some results after search...
      else {
        div(cls := "container-fluid",
          shareResultsModal,
          div(cls := "container-fluid",
            div(cls := "row justify-content-md-center",
              div(cls := "row col-lg-11 justify-content-md-center",
                ulRepertorySelection,
                div(cls := "col-md-7", style := "margin-top:20px;",
                  input(cls := "form-control", `id` := "inputField",
                    onkeydown := { (event: dom.KeyboardEvent) =>
                      if (event.keyCode == 13) {
                        event.stopPropagation()
                        onSymptomEntered()
                      }
                    },
                    `placeholder` := "Enter a symptom (for example: head, pain, left)"
                  ),
                  // It will be (de-) populated on demand.
                  div(cls := "container-fluid", id:="advancedSearchControlsDiv")
                ),
                div(id:="mainViewSearchButtons", cls:="col-md-auto text-center center-block", style:="margin-top:20px;",
                  button(cls := "btn btn-primary", style:="width: 80px; margin-right:5px;", `type` := "button",
                    onclick := { (event: Event) =>
                      event.stopPropagation()
                      onSymptomEntered()
                    },
                    span(cls := "oi oi-magnifying-glass", title := "Find", aria.hidden := "true")),
                  button(`id`:="buttonMainViewAdvancedSearch", cls := "btn", style := "width: 70px;margin-right:5px;", `type` := "button",
                    onclick := { (event: Event) =>
                      event.stopPropagation()
                      onShowAdvancedSearchOptionsMainView(false, false)
                    },
                    span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true")),
                  button(cls := "btn", style := "width: 70px;", `type` := "button",
                    onclick := { (event: Event) =>
                      event.stopPropagation()
                      onSymptomListRedoPressed()
                    },
                    span(cls := "oi oi-action-redo", title := "Find again", aria.hidden := "true"))
                )
              )
            )
          ),
          div(cls := "container-fluid", style := "margin-top: 23px;", id := "resultStatus"),
          div(cls := "container-fluid", id := "resultDiv"),
          div(cls := "container-fluid", id := "paginationDiv"),
          div(cls := "span12", id := "caseDiv", {
            if (Case.size() > 0)
              Case.toHTML(_remedyFormat.now)
            else ""
          })
        )
      }

    // Initially, update Repertory-Drop-Down and available remedies for each repertory
    //
    // TODO: This is a classical race condition on program start-up: updateAvailableRepertories()
    // accesses dom-elements that don't exist until apply() returned! It works, however, because
    // by the time those results come back from the backend, those dom-elements do exist.  It is,
    // however, not nice and should be fixed one day!
    if (_repertoryRemedyMap.size == 0)
      updateAvailableRepertoriesAndRemedies()

    // If initial page, then vertically center search form
    if (_repertorisationResults.now == None && Case.size() == 0) {
      div(cls := "introduction", div(cls := "vertical-align", myHTML))
    }
    // If there are already some search results, do without center and fix nav bar prior to rendering
    else {
      if (dom.document.getElementById("nav_bar_logo").innerHTML.length() == 0) {
        val navbar = dom.document.getElementById("public_nav_bar").asInstanceOf[dom.html.Element]

        navbar.className = navbar.className + " bg-dark navbar-dark shadow p-3 mb-5"

        dom.document.getElementById("nav_bar_logo")
          .appendChild(a(cls := "navbar-brand py-0", style:="margin-top:8px;", href := serverUrl(), h5(cls:="freetext", "OOREP")).render)
      }
      div(style:="margin-top:80px;", myHTML)
    }
  }
}
