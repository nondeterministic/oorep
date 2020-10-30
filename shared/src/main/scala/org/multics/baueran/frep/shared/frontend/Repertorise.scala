package org.multics.baueran.frep.shared.frontend

import scalatags.JsDom.all._
import org.scalajs.dom
import dom.Event
import dom.raw.{HTMLButtonElement, HTMLInputElement, Node}
import io.circe.parser.parse
import rx.Var

import scala.collection.mutable
import scala.util.{Failure, Success}
import fr.hmil.roshttp.{BackendConfig, HttpRequest}
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.shared.Defs.{CookieFields, maxLengthOfSymptoms, maxNumberOfResults, maxNumberOfSymptoms}
import org.multics.baueran.frep.shared.sec_frontend.FileModalCallbacks.updateMemberFiles

object Repertorise {

  private val _pageCache = new PageCache()
  private val _repertoryRemedyMap = mutable.HashMap[String,List[String]]()
  private val _selectedRepertory = Var("")
  private var _showMaxSearchResultsAlert = true
  private var _showMultiOccurrences = false
  private val _remedyFormat = Var(RemedyFormat.Formatted)
  val _repertorisationResults: Var[Option[ResultsCaseRubrics]] = Var(None)
  private val _resultRemedyStats: Var[List[ResultsRemedyStats]] = Var(List())

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
            "(Multi-occurrences of remedies in search: ",
            multiRemedies
              .take(multiRemedies.size - 1)
              .map { case ResultsRemedyStats(nameabbrev, count, cumulativeWeight) => {
                span(
                  (s"${count}x"),
                  a(href := "#", onclick := { (event: Event) =>
                    doLookup(_pageCache.latest.abbrev, _pageCache.latest.symptom, None, Some(nameabbrev), 1)
                  }, nameabbrev),
                  ("(" + cumulativeWeight + "), ")
                )
              }
              },
            span(
              (s"${multiRemedies.last.count}x"),
              a(href := "#", onclick := { (event: Event) =>
                doLookup(_pageCache.latest.abbrev, _pageCache.latest.symptom, None, Some(multiRemedies.last.nameabbrev), 1)
              }, multiRemedies.last.nameabbrev),
              ("(" + multiRemedies.last.cumulativeWeight + ")")
            ),
            ")").render
        )
      }
      else {
        multiOccurrenceDiv.appendChild(
          span(id := "collapseMultiOccurrences", cls := s"collapse ${if (_showMultiOccurrences) "show" else "hide"}",
            "(Multi-occurrences of remedies in search: None)").render)
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

      for (remedyAbbrev <- _repertoryRemedyMap.get(_selectedRepertory.now).getOrElse(List.empty).sorted)
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

      val remedies =
        if (_remedyFormat.now == RemedyFormat.NotFormatted)
          result.getRawRemedies()
        else
          result.getFormattedRemedies()

      if (remedies.size > 0)
        tr(
          td(result.rubric.fullPath, style:="min-width:400px;"),
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
          td(result.rubric.fullPath, style := "min-width:400px;"),
          td(),
          td()
        )
    }

    resetContentView()

    _repertorisationResults.now match {
      case Some(ResultsCaseRubrics(totalNumberOfResults, totalNumberOfPages, currPage, results)) if (results.size > 0) => {
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
                searchStatsString += s"'${_pageCache.latest.remedy}'"

              if (_pageCache.latest.minWeight > 1)
                searchStatsString += s" with min. weight >= ${_pageCache.latest.minWeight}"

              searchStatsString += s". Showing page ${currPage + 1} of ${totalNumberOfPages}. "
              searchStatsString
            })
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
      case Some(ResultsCaseRubrics(totalNumberOfResults, totalNumberOfPages, _, results))
        if (_showMaxSearchResultsAlert &&
          (results.size >= maxNumberOfResults || totalNumberOfPages > 1)) =>
      {
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
              button(`type`:="button", cls:="close", data.dismiss:="alert",  onclick := { (_: Event) => _showMaxSearchResultsAlert = false },
                span(aria.hidden:="true", raw("&times;"))),
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
      case _ => ;
    }

    _repertorisationResults.now match {
      case Some(ResultsCaseRubrics(_, _, _, results)) =>
        results.foreach(result => dom.document.getElementById("resultsTBody").appendChild(resultRow(result).render))
      case _ => ;
    }

    Case.updateCaseHeaderView()
  }

  // ------------------------------------------------------------------------------------------------------------------
  def toggleRemedyFormat() = {
    if (_remedyFormat.now == RemedyFormat.NotFormatted)
      _remedyFormat() = RemedyFormat.Formatted
    else
      _remedyFormat() = RemedyFormat.NotFormatted

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
    doLookup(_selectedRepertory.now, symptom, None, None, 1)
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
      case null => 1
      case element => element.asInstanceOf[HTMLButtonElement].textContent.trim match {
        case "" => 1
        case otherwise => otherwise.toInt
      }
    }
    val symptom = dom.document.getElementById("inputField").asInstanceOf[HTMLInputElement].value

    doLookup(_selectedRepertory.now, symptom, None, remedyQuery, remedyMinWeight)
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSymptomListRedoPressed() = {
    dom.document.getElementById("inputField").asInstanceOf[dom.html.Input].value = _pageCache.latest.symptom

    if (!advancedSearchOptionsVisible()) {
      _pageCache.latest.remedy match {
        case Some(_) => onShowAdvancedSearchOptionsMainView(true, false)
        case _ => {
          if (_pageCache.latest.minWeight > 1)
            onShowAdvancedSearchOptionsMainView(true, false)
        }
      }
    }

    dom.document.getElementById("inputField").asInstanceOf[HTMLInputElement].focus()
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onShowAdvancedSearchOptionsMainView(restorePreviousValues: Boolean, landingPageIsCurrentView: Boolean): Node = {
    val parentDiv = dom.document.getElementById("advancedSearchControlsDiv").asInstanceOf[dom.html.Div]
    val weightDropDownsDiv = div(id:="weightDropDownsDiv", cls:="dropdown-menu")

    parentDiv.appendChild(
      div(cls:="row", style:="margin-top:15px;",
        div(cls:="col-md-auto my-auto",
          "Remedy:"),
        div(cls:="col",
          input(cls:="form-control", `id`:="inputRemedy", list:="remedyDataList",
            onkeydown := { (event: dom.KeyboardEvent) =>
              if (event.keyCode == 13) {
                event.stopPropagation()
                onSymptomEntered()
              }
            }, `placeholder` := "Enter a remedy (for example: Sil.)"),
          datalist(`id`:="remedyDataList")
        ),
        div(cls:="col-md-auto my-auto",
          "Min. weight:"),
        div(cls:="col-sm-2",
          div(id:="weightDropDowns", cls:="dropdown show",
            button(id:="minWeightDropdown", cls:="btn dropdown-toggle", `type`:="button", data.toggle:="dropdown", if (restorePreviousValues) _pageCache.latest.minWeight.toString else "1"),
            weightDropDownsDiv
          )
        )
      ).render)

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

    for (remedyAbbrev <- _repertoryRemedyMap.get(_selectedRepertory.now).getOrElse(List.empty).sorted)
      optionsDiv.appendChild(option(value:=s"$remedyAbbrev").render)

    // Add possible weights for search (4 is only in Hering's)
    for (i <- 1 to 4) {
      dom.document.getElementById("weightDropDownsDiv").asInstanceOf[dom.html.Div].appendChild(
        a(cls:="dropdown-item", href:="#",
          onclick := { (_: Event) =>
            dom.document.getElementById("minWeightDropdown").asInstanceOf[dom.html.Button].textContent = s"$i" },
          i).render
      )
    }

    // Add the find, advanced find options etc. buttons
    val advancedButton = dom.document.getElementById("buttonMainViewAdvancedSearch").asInstanceOf[dom.html.Button]
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
    buttonDiv.replaceChild(basicButton, advancedButton)
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

  // ------------------------------------------------------------------------------------------------------------------
  private def doLookup(abbrev: String, symptom: String, pageOpt: Option[Int], remedyStringOpt: Option[String], minWeight: Int): Unit = {
    val page = math.max(pageOpt.getOrElse(0), 0)
    val remedyString = remedyStringOpt.getOrElse("")

    def showRepertorisationResults(results: ResultsCaseRubrics, remedyStats: List[ResultsRemedyStats]): Unit = {
      _pageCache.addPage(CachePage(abbrev, symptom, remedyStringOpt, minWeight, results, remedyStats))

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

              if (minWeight > 1)
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
            showRepertorisationResultsError(symptom, remedyString, minWeight)
        })
    }

    val resultStatusDiv = dom.document.getElementById("resultStatus")

    if (abbrev.length == 0) {
      resultStatusDiv.innerHTML = ""
      resultStatusDiv.appendChild(
        div(cls:="alert alert-danger", role:="alert",
          b("No results. Make sure a repertory is selected.")).render)
      return
    }

    if (symptom.trim.replaceAll("[^A-Za-z0-9äüöÄÜÖß]", "").length == 0 && remedyString.length == 0) {
      resultStatusDiv.innerHTML = ""
      resultStatusDiv.appendChild(
        div(cls:="alert alert-danger", role:="alert",
          b("No results. Enter some symptoms and/or a remedy.")).render)
      return
    }

    if (symptom.trim.replaceAll("[^A-Za-z0-9äüöÄÜÖß]", "").length <= 3 && remedyString.length == 0) {
      resultStatusDiv.innerHTML = ""
      resultStatusDiv.appendChild(
        div(cls:="alert alert-danger", role:="alert",
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
      println("Updating repertory information from backend")
      HttpRequest(s"${serverUrl()}/${apiPrefix()}/available_remedies")
        .send()
        .onComplete({
          case response: Success[SimpleHttpResponse] => {
            parse(response.get.body) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[List[(String, String, String)]] match {
                  case Right(repertoryRemedies) => {
                    // Update available repertories
                    repertoryRemedies
                      .map { case (repAbbrev, repAccess, _) => (repAbbrev, repAccess) }
                      .distinct
                      .sortBy(_._1)
                      .foreach { case (repAbbrev, repAccess) => {
                        dom.document.getElementById("repSelectionDropDown")
                          .appendChild(a(cls := "dropdown-item", href := "#", data.value := repAbbrev,
                            onclick := { (event: Event) =>
                              _selectedRepertory() = repAbbrev
                              dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
                            }, repAbbrev).render)

                        if (_selectedRepertory.now.length > 0)
                          dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
                        else if (repAccess == RepAccess.Default.toString) {
                          _selectedRepertory() = repAbbrev
                          dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
                        }
                      }
                      }

                    // Update available remedies
                    val repertoryNames = repertoryRemedies.map { case (repAbbrev, _, _) => (repAbbrev) }.distinct
                    for (repertoryAbbrev <- repertoryNames) {
                      _repertoryRemedyMap.addOne((repertoryAbbrev,
                        repertoryRemedies
                          .filter { case (repAbbrev, _, _) => repAbbrev == repertoryAbbrev }
                          .map { case (_, _, remedyAbbrev) => remedyAbbrev }
                      ))
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
      println("Updating repertory information from RAM")
      for ((repAbbrev, repAccess) <- _repertoryRemedyMap.toList.sortBy(_._1)) {
        dom.document.getElementById("repSelectionDropDown")
          .appendChild(a(cls := "dropdown-item", href := "#", data.value := repAbbrev,
            onclick := { (event: Event) =>
              _selectedRepertory() = repAbbrev
              dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
            }, repAbbrev).render)

        if (_selectedRepertory.now.length > 0)
          dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
        else if (repAccess == RepAccess.Default.toString) {
          _selectedRepertory() = repAbbrev
          dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[HTMLButtonElement].textContent = "Repertory: " + _selectedRepertory.now
        }
      }
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  def apply() = {
    val ulRepertorySelection =
      div(cls:="dropdown col-sm-2",
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
            div(cls:="col-sm-12 text-center", img(src:="img/logo_small.png")),
            div(cls := "row", style:="margin-top:20px;",
              div(cls := "col-sm-1"),
              div(cls := "row col-sm-10",
                ulRepertorySelection,
                div(cls := "col-sm-9",
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
          div(cls := "container-fluid text-center",
            div(cls := "row", style := "margin-top:20px;",
              div(cls := "col-sm-1"),
              div(cls := "row col-sm-10",
                ulRepertorySelection,
                div(cls := "col-sm-7",
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
                span(id:="mainViewSearchButtons",
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
              ),
              div(cls := "col-sm-1")
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
          .appendChild(a(cls := "navbar-brand py-0", href := serverUrl(), h5(cls:="freetext", "OOREP")).render)
      }
      div(style:="margin-top:100px;", myHTML)
    }
  }
}
