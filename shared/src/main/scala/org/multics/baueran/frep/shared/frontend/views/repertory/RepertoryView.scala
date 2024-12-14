package org.multics.baueran.frep.shared.frontend.views.repertory

import scalatags.JsDom.all._
import org.scalajs.dom
import dom.Event
import io.circe.parser.parse
import scalatags.JsDom

import scala.scalajs.js.URIUtils._
import scala.language.implicitConversions
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.shared.Defs.{CookieFields, HeaderFields, ResourceAccessLvl, maxLengthOfSymptoms, maxNumberOfResultsPerPage, maxNumberOfSymptoms}
import org.multics.baueran.frep.shared.TopLevelUtilCode.getDocumentCsrfCookie
import org.multics.baueran.frep.shared.frontend._

object RepertoryView extends TabView {

  private class SelectedRepertoryRx(abbrev: String) extends Rx(abbrev) {
    override def triggerLater(): Unit = refreshRemedyDataList(_repertories.getRemedies(value))
    override def toString() = value
  }
  private val _selectedRepertory = new SelectedRepertoryRx("")

  private class ResultRemedyStatsRx(stats: List[ResultsRemedyStats]) extends Rx(stats) {
    override def triggerLater() = redrawMultiOccurringRemedies()
  }
  private val _resultRemedyStats = new ResultRemedyStatsRx(List())

  class RemedyFormatRx(format: RemedyFormat.Value) extends Rx(format) {
    override def triggerLater() = {
      _repertorisationResults.get() match {
        case Some(_) => showResults()
        case None => ;
      }
    }
  }
  val _remedyFormat = new RemedyFormatRx(RemedyFormat.Abbreviated)

  private class RepertorisationResultsRx(results: Option[ResultsCaseRubrics]) extends Rx(results) {
    override def triggerLater() = {
      value match {
        case Some(_) => showResults()
        case None => ;
      }
    }
  }
  private val _repertorisationResults = new RepertorisationResultsRx(None)

  private var _currResultShareLink = s"${serverUrl()}"
  private val _pageCache = new PageCache()
  private var _remedies = new Remedies(List())
  private val _repertories = new Repertories()
  private var _defaultRepertory = ""
  private var _showMaxSearchResultsAlert = true
  private var _showMultiOccurrences = false
  private val _prefix = "repertoryView"
  private var _advancedSearchOptionsVisible = false

  // ------------------------------------------------------------------------------------------------------------------
  private def resultsLink(): String = encodeURI(_currResultShareLink)

  // ------------------------------------------------------------------------------------------------------------------
  private def redrawMultiOccurringRemedies(): Unit = {
    val multiRemedies = _resultRemedyStats.get().filter(_.count > 2).sortBy(-_.count)
    val multiOccurrenceDiv = dom.document.getElementById("multiOccurrenceDiv").asInstanceOf[dom.html.Element]
    val collapseMultiOccurrences = dom.document.getElementById("collapseMultiOccurrences").asInstanceOf[dom.html.Element]

    if (multiOccurrenceDiv == null)
      return

    if (collapseMultiOccurrences != null)
      multiOccurrenceDiv.removeChild(collapseMultiOccurrences)

    _pageCache.latest() match {
      case Some(cachePage) if (multiRemedies.length > 0) =>
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
                      doLookup(cachePage.abbrev, cachePage.symptom, None, Some(nameabbrev), 0)
                    }, nameabbrev),
                    ("(" + cumulativeWeight + "), ")
                  )
                }
                },
              span(
                (s"${multiRemedies.last.count}x"),
                a(href := "#", onclick := { (event: Event) =>
                  doLookup(cachePage.abbrev, cachePage.symptom, None, Some(multiRemedies.last.nameabbrev), 0)
                }, multiRemedies.last.nameabbrev),
                ("(" + multiRemedies.last.cumulativeweight + ")")
              ),
              ")").render
          )
        }
        else if (multiRemedies.length == 1) {
          multiOccurrenceDiv.appendChild(
            span(id := "collapseMultiOccurrences", cls := s"collapse ${if (_showMultiOccurrences) "show" else "hide"}",
              "(Multi-occurrences of remedies in results: ",
              multiRemedies
                .map { case ResultsRemedyStats(nameabbrev, count, cumulativeWeight) => {
                  span(
                    (s"${count}x"),
                    a(href := "#", onclick := { (event: Event) =>
                      doLookup(cachePage.abbrev, cachePage.symptom, None, Some(nameabbrev), 0)
                    }, nameabbrev),
                    ("(" + cumulativeWeight + "))")
                  )
                }
                })
              .render)
        }
      case _ =>
        multiOccurrenceDiv.appendChild(
          span(id := "collapseMultiOccurrences", cls := s"collapse ${if (_showMultiOccurrences) "show" else "hide"}",
            "(Multi-occurrences of remedies in results: None)").render)
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def showCase(): Unit = {
    if (Case.size() > 0) {
      MainView.CaseDiv.empty()
      MainView.CaseDiv.append(new Case.HtmlRepresentation(_remedyFormat.get())().render)
      Case.updateCaseViewAndDataStructures()
      Case.updateCaseHeaderView()
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  // Render HTML for the results of a repertory lookup directly to page.
  def showResults(): Unit = {

    def resultRow(result: CaseRubric) = {
      implicit def crToCR(cr: CaseRubric): BetterCaseRubric = new BetterCaseRubric(cr)

      val remedies = result.getFormattedRemedyNames(_remedyFormat.get())

      if (remedies.size > 0)
        tr(
          td(result.rubric.fullPath, style:="width:35%;"),
          td(remedies.take(remedies.size - 1).map(l => span(l, ", ")) ::: List(remedies.last)),
          td(cls := "text-right", style := "white-space:nowrap;",
            button(cls := "btn btn-sm btn-secondary", `type` := "button", id := ("button_" + result.repertoryAbbrev + "_" + result.rubric.id),
              style := "vertical-align: middle; display: inline-block",
              (if (Case.cRubrics.filter(_.equalsIgnoreWeight(result)).size > 0) attr("disabled") := "disabled" else ""),
              title := "Add rubric",
              onclick := { (event: Event) => {
                event.stopPropagation()
                Case.addRepertoryLookup(result)
                Case.updateCaseViewAndDataStructures()
                dom.document.getElementById("button_" + result.repertoryAbbrev + "_" + result.rubric.id).asInstanceOf[dom.html.Button].setAttribute("disabled", "1")
                showCase()
                MainView.toggleOnBeforeUnload()
              }
              }, b(raw("&nbsp;+&nbsp;")))
          )
        )
      else
        tr(
          td(result.rubric.fullPath, style := "width:35%;"),
          td(),
          td()
        )
    }

    MainView.resetContentView()
    showCase()

    (_repertorisationResults.get(), _pageCache.latest()) match {
      case (Some(ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, currPage, results)), Some(latestCachePage)) if (results.size > 0) => {
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
              var searchStatsString =
                if (totalNumberOfResults == totalNumberOfRepertoryRubrics)
                  s"0 rubrics match '${latestCachePage.symptom}'"
                else if (latestCachePage.symptom.trim.length > 0 && latestCachePage.remedy.getOrElse("").length > 0)
                  s"${totalNumberOfResults} rubrics match '${latestCachePage.symptom}' containing '${latestCachePage.remedy.getOrElse("")}'"
                else if (latestCachePage.symptom.trim.length > 0)
                  s"${totalNumberOfResults} rubrics match '${latestCachePage.symptom}'"
                else if (latestCachePage.remedy.getOrElse("").trim.length > 0)
                  s"${totalNumberOfResults} rubrics contain '${latestCachePage.remedy.getOrElse("")}'"
                else // This case should never fire, really.
                  s"${totalNumberOfResults} rubrics found"

              if (latestCachePage.minWeight > 0)
                searchStatsString += s" with min. weight >= ${latestCachePage.minWeight}"

              searchStatsString += s". Showing page ${currPage + 1} of ${totalNumberOfPages}."
              searchStatsString
            }),
            span(raw("&nbsp;&nbsp;")),
            button(`type`:="button", cls:="btn btn-sm px-1 py-0 btn-outline-primary", data.toggle:="modal", data.dismiss:="modal", data.target:=s"#${_prefix}shareResultsModal",
              span(cls:="oi oi-link-intact", style:="font-size: 12px;", title:="Share link...", aria.hidden:="true")),
            span(raw("&nbsp;&nbsp;&nbsp;")),
            button(`type`:="button", cls:="btn btn-sm px-1 py-0 btn-outline-primary", onclick:={ (_: Event) => dom.window.open(_currResultShareLink) },
              span(cls:="oi oi-external-link", style:="font-size: 12px;", title:="Open in new window...", aria.hidden:="true")),
            span(raw("&nbsp;&nbsp;"))
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
                    cls := "underline", href := "#",
                    onclick := ((event: Event) => toggleRemedyFormat()),
                    "Remedies")
                ),
                th(attr("scope") := "col", " ")
              ),
              tbody(scalatags.JsDom.attrs.id := "resultsTBody")
            )
          ).render
        )

        dom.document.getElementById(s"${_prefix}_paginationDiv").innerHTML = ""
        if (totalNumberOfPages > 1) {
          val pg = new Paginator(totalNumberOfPages, currPage, 5).getPagination()
          val htmlPg = new PaginatorHtml(s"${_prefix}_paginationDiv", pg)

          dom.document.getElementById(s"${_prefix}_paginationDiv")
            .appendChild(
              htmlPg.toHtml(latestCachePage.abbrev, latestCachePage.symptom, latestCachePage.remedy, latestCachePage.minWeight, doLookup)
                .render)
        }
      }
      case _ => ;
    }

    // Display potentially useful hint, when max. number of search results was returned.
    (_repertorisationResults.get(), _pageCache.latest()) match {
      case (Some(ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, _, results)), Some(latestCachePage)) => {
        // If the total number of results matches the total number of available rubrics in a repertory, the user either entered "*"
        // or, in fact, the repertory is a so called small repertory, which means, we show everything...
        if (_showMaxSearchResultsAlert && totalNumberOfRepertoryRubrics == totalNumberOfResults) {
          dom.document.getElementById("resultStatus").appendChild(
            div(cls := "alert alert-success", role := "alert",
              button(`type` := "button", cls := "close", data.dismiss := "alert", onclick := { (_: Event) => _showMaxSearchResultsAlert = false },
                span(aria.hidden := "true", raw("&times;"))),
                b(s"Showing ALL available rubrics instead, because this small repertory only has ${totalNumberOfRepertoryRubrics.toString} rubrics in total.")
            ).render)
        }
        else if (_showMaxSearchResultsAlert && (results.size >= maxNumberOfResultsPerPage || totalNumberOfPages > 1)) {
          val fullPathWords = results.map(cr => cr.rubric.fullPath.split("[, ]+").filter(_.length > 0).map(_.trim())).flatten
          val pathWords = results.map(cr => cr.rubric.path.getOrElse("").split("[, ]+").filter(_.length > 0).map(_.trim())).flatten
          val textWords = results.map(cr => cr.rubric.textt.getOrElse("").split("[, ]+").filter(_.length > 0).map(_.trim())).flatten

          // Yields a sequence like [ ("pain", 130), ("abdomen", 50), ... ]
          val sortedResultOccurrences =
            (pathWords ::: textWords ::: fullPathWords).map(_.replaceAll("[^A-Za-z0-9äÄöÖüÜ]", "")).sortWith(_ > _)
              .groupBy(identity).view.mapValues(_.size)
              .toSeq.sortWith(_._2 > _._2)

          // Filter out all those results, which were actually desired via positive search terms entered by the user
          val searchTerms = new SearchTerms(latestCachePage.symptom)
          val filteredSortedResultOccurrences =
            sortedResultOccurrences
              .filter { case (t, _) =>
                t.length() > 3 &&
                  !(searchTerms.positive.exists(pt => searchTerms.isExpressionInTextPassage(pt, Some(t)))) &&
                  !(searchTerms.exactPositiveOnly.exists(pt => searchTerms.isExpressionInTextPassage(t, Some(pt))))
              }

          // If there are some left after filtering, suggest to user to exclude top-most result from a next search.
          if (filteredSortedResultOccurrences.length > 2) {
            val newSearchTerms = {
              (searchTerms.unexactPositiveOnly ::: searchTerms.exactPositiveOnly.map("\"" + _ + "\"")).mkString(", ") + {
                if (searchTerms.negative.length > 0)
                  ", " + (searchTerms.unexactNegativeOnly ::: searchTerms.exactNegativeOnly.map("\"" + _ + "\"")).map("-" + _).mkString(", ")
                else ""
              } + s", -${filteredSortedResultOccurrences.head._1.toLowerCase.take(6)}*"
            }

            dom.document.getElementById("resultStatus").appendChild(
              div(cls := "alert alert-warning", role := "alert",
                button(`type` := "button", cls := "close", data.dismiss := "alert", onclick := { (_: Event) => _showMaxSearchResultsAlert = false },
                  span(aria.hidden := "true", raw("&times;"))),
                if (searchTerms.positive.length > 0)
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

    _repertorisationResults.get() match {
      case Some(ResultsCaseRubrics(_, _, _, _, results)) =>
        results.foreach(result => dom.document.getElementById("resultsTBody").appendChild(resultRow(result).render))
      case _ => ;
    }

    // TODO: I'm not a 100% sure, this will work in all cases, but can't find a problem with it yet...
    if (_resultRemedyStats.get().size > 1 && dom.document.getElementById("collapseMultiOccurrences") == null)
      redrawMultiOccurringRemedies()

    Case.updateCaseHeaderView()
  }

  // ------------------------------------------------------------------------------------------------------------------
  def toggleRemedyFormat(): Unit = {
    if (_remedyFormat.get() == RemedyFormat.Fullname)
      _remedyFormat.set(RemedyFormat.Abbreviated)
    else
      _remedyFormat.set(RemedyFormat.Fullname)

    if (Case.size() > 0)
      showCase()
    else
      println("RepertoryView: toggleRemedyFormat: Case.size() == 0.")
  }

  // ------------------------------------------------------------------------------------------------------------------
  //  private def advancedSearchOptionsVisible(): Boolean = {
  //    dom.document.getElementById("advancedSearchControlsDiv").childNodes.length > 0
  //  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSymptomLinkClicked(symptom: String): Unit = {
    _pageCache.latest() match {
      case Some(latestCachePage) =>
        doLookup(_selectedRepertory.toString(), symptom, None, latestCachePage.remedy, latestCachePage.minWeight)
      case _ =>
        doLookup(_selectedRepertory.toString(), symptom, None, None, 0)
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSymptomEntered(): Unit = {
    val remedyQuery = dom.document.getElementById("inputRemedy") match {
      case null => None
      case element => element.asInstanceOf[dom.html.Input].value.trim match {
        case "" => None
        case otherwise => Some(otherwise)
      }
    }
    val remedyMinWeight = dom.document.getElementById("minWeightDropdown") match {
      case null => 0
      case element => element.asInstanceOf[dom.html.Button].textContent.trim match {
        case "" => 0
        case otherwise => otherwise.toInt
      }
    }
    val symptom = dom.document.getElementById("inputField").asInstanceOf[dom.html.Input].value

    doLookup(_selectedRepertory.toString(), symptom, None, remedyQuery, remedyMinWeight)
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSymptomListRedoPressed(): Unit = {
    _pageCache.latest() match {
      case Some(latestCachePage) =>
        dom.document.getElementById("inputField").asInstanceOf[dom.html.Input].value = latestCachePage.symptom

        latestCachePage.remedy match {
          case Some(_) =>
            onShowAdvancedSearchOptionsMainView(true, false)
          case _ =>
            if (latestCachePage.minWeight > 0)
              onShowAdvancedSearchOptionsMainView(true, false)
        }

        dom.document.getElementById("inputField").asInstanceOf[dom.html.Input].focus()
      case _ =>
        println("RepertoryView: onSymptomListRedoPressed failed.")
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onShowAdvancedSearchOptionsMainView(restorePreviousValues: Boolean, landingPageIsCurrentView: Boolean): Unit = {
    val parentDiv = dom.document.getElementById("advancedSearchControlsDiv")

    if (parentDiv == null)
      return

    dom.document.getElementById("advancedSearchControlsContent") match {
      case null =>
        _advancedSearchOptionsVisible = true
        parentDiv.asInstanceOf[dom.html.Div].appendChild(
          div(id := "advancedSearchControlsContent", cls := "row", style := "margin-top:15px;",
            div(cls := "col",
              div(cls := "row",
                div(cls := "col-md-auto my-auto",
                  "Remedy:"),
                div(cls := "col",
                  input(cls := "form-control", `id` := "inputRemedy", list := s"${_prefix}_remedyDataList",
                    onkeydown := { (event: dom.KeyboardEvent) =>
                      if (event.keyCode == 13) {
                        event.stopPropagation()
                        onSymptomEntered()
                      }
                    }, `placeholder` := "Enter a remedy abbreviation or fullname (for example: Sil. or Silica)"),
                  datalist(`id` := s"${_prefix}_remedyDataList")
                )
              )
            ),
            div(cls := "col-md-auto",
              div(cls := "row",
                div(cls := "col-md-auto my-auto",
                  "Min. weight:"),
                div(cls := "col-md-2",
                  div(id := "weightDropDowns", cls := "dropdown show",
                    button(id := "minWeightDropdown", cls := "btn dropdown-toggle btn-secondary", `type` := "button", data.toggle := "dropdown",
                      if (restorePreviousValues && _pageCache.size() > 0) _pageCache.latest().get.minWeight.toString else "0"
                    ),
                    div(id:="weightDropDownsDiv", cls:="dropdown-menu")
                  )
                )
              )
            )
          ).render)
      case _ => ;
    }

    // Set remedy input string, if one was previously submitted, and redo-button was pressed
    if (restorePreviousValues && _pageCache.size() > 0) {
      for {
        latestCachePage <- _pageCache.latest()
      } yield for {
        remedyAbbrev <- latestCachePage.remedy
      } yield dom.document.getElementById("inputRemedy").asInstanceOf[dom.html.Input].value = remedyAbbrev
    }

    refreshRemedyDataList(_repertories.getRemedies(_selectedRepertory.toString()))

    // Add possible weights for search (4 is only in hering, 0 is only in bogboen)
    val weightDropDownsDiv = dom.document.getElementById("weightDropDownsDiv").asInstanceOf[dom.html.Div]

    while (weightDropDownsDiv.childNodes.length > 0)
      weightDropDownsDiv.removeChild(weightDropDownsDiv.firstChild)

    for (i <- 0 to 4) {
      weightDropDownsDiv.appendChild(
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
            button(id:="buttonMainViewBasicSearch", cls:="btn btn-secondary text-nowrap", style:="width: 140px; margin: 5px;", `type`:="button",
              onclick := { (event: Event) =>
                onHideAdvancedSearchOptionsMainView(event, landingPageIsCurrentView)
              }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"), " Basic"
            ).render
          else
            button(id:="buttonMainViewBasicSearch", cls:="btn btn-secondary text-nowrap", style := "width: 70px;margin-right:5px;", `type`:="button",
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
  private def onHideAdvancedSearchOptionsMainView(event: Event, landingPageView: Boolean): Unit = {
    event.stopPropagation()
    val theDiv = dom.document.getElementById("advancedSearchControlsDiv").asInstanceOf[dom.html.Div]

    while (theDiv.childNodes.length > 0)
      theDiv.removeChild(theDiv.firstChild)

    val basicButton = dom.document.getElementById("buttonMainViewBasicSearch").asInstanceOf[dom.html.Button]
    val advancedButton = {
      if (landingPageView)
        button(id:="buttonMainViewAdvancedSearch", cls:="btn btn-secondary text-nowrap", style:="width: 140px; margin: 5px;", `type`:="button",
          onclick := { (event: Event) =>
            event.stopPropagation()
            onShowAdvancedSearchOptionsMainView(false, landingPageView)
          }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"), " Advanced...").render
      else
        button(`id`:="buttonMainViewAdvancedSearch", cls := "btn btn-secondary text-nowrap", style := "width: 70px;margin-right:5px;", `type` := "button",
          onclick := { (event: Event) =>
            event.stopPropagation()
            onShowAdvancedSearchOptionsMainView(false, landingPageView)
          },
          span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true")).render
    }

    _advancedSearchOptionsVisible = false

    val buttonDiv = dom.document.getElementById("mainViewSearchButtons").asInstanceOf[dom.html.Div]
    buttonDiv.replaceChild(advancedButton, basicButton)
  }

  def jsDoLookup(abbrev: String, symptom: String, page: Int, remedyString: String, minWeight: Int) = {
    // Hide navbar initially, while the spinner shows. (Later, the code in this file will show it again.)
    dom.document.getElementById("nav_bar").asInstanceOf[dom.html.Div].classList.add("d-none")

    _selectedRepertory.set(abbrev)

    doLookup(abbrev,
      symptom,
      if (page > 0) Some(page) else None,
      if (remedyString.length > 0) Some(remedyString) else None,
      if (minWeight > 0) minWeight else 0)
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def doLookup(abbrev: String, symptom: String, pageOpt: Option[Int], remedyStringOpt: Option[String], minWeight: Int): Unit = {
    val page = math.max(pageOpt.getOrElse(0), 0)

    val abbrevForRemedyEntered = {
      if (remedyStringOpt.getOrElse("").trim.length > 0) {
        _remedies.getRemedyEntered(remedyStringOpt.getOrElse("")) match {
          case RemedyEntered(Some(remedyId), _) if (_remedies.get(remedyId) != None) =>
            Some(_remedies.get(remedyId).get.nameAbbrev)
          case _ =>
            Some(remedyStringOpt.get.trim) // Remedy not found but entered, so we pass it on to generate an error
        }
      }
      else
        None // No remedy was entered
    }

    def showRepertorisationResults(results: ResultsCaseRubrics, remedyStats: List[ResultsRemedyStats]): Unit = {
      _pageCache.addPage(CachePage(abbrev, symptom, abbrevForRemedyEntered, minWeight, results, remedyStats))

      _currResultShareLink =
        s"${serverUrl()}/show?repertory=${abbrev}&symptom=${symptom}&page=${(pageOpt.getOrElse(0) + 1).toString}&remedyString=${abbrevForRemedyEntered.getOrElse("")}&minWeight=${minWeight.toString}"

      _repertorisationResults.set(Some(results))

      _resultRemedyStats.set(List())
      _resultRemedyStats.set(remedyStats)

      if (Case.size() > 0)
        showCase()

      dom.document.body.classList.remove("wait")

      // When we do a /?show=... lookup, we need to make sure the disclaimer is made visible again. For other cases, it doesn't matter, because
      // the disclaimer is already visible at this point.
      dom.document.getElementById("disclaimer_div").asInstanceOf[dom.html.Div].style.setProperty("display", "block")

      // Reset window title (just in case we came from index_lookup), so that a window title which contained
      // symptoms before, which now won't match with the latest search, are replaced with a generic and less
      // confusing title.
      if (symptom.length > 0 && remedyStringOpt != None)
        dom.document.title = s"OOREP - ${remedyStringOpt.getOrElse("")}: ${symptom} (${abbrev})"
      else if (symptom.length > 0)
        dom.document.title = s"OOREP - ${symptom} (${abbrev})"
      else if (remedyStringOpt != None)
        dom.document.title = s"OOREP - ${remedyStringOpt.getOrElse("")} (${abbrev})"
      else
        dom.document.title = "OOREP - open online homeopathic repertory"
    }

    def showRepertorisationResultsError(symptom: String, remedyString: String, minWeight: Int): Unit = {
      val searchTerms = new SearchTerms(symptom)
      val longPosTerms = searchTerms.positive.map(_.replace("*", "")).filter(_.length() > 6)
      val longNegTerms = searchTerms.negative
      val resultStatusDiv = dom.document.getElementById("resultStatus")

      dom.document.body.classList.remove("wait")

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

      val tmpErrorMessage1 = s"Perhaps try a different repertory/remedy; or use wild-card search, like '"
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

    def getPageFromBackend(abbrev: String, symptom: String, remedyString: String, minWeight: Int, page: Int): Unit = {
      val cachedRemedies = _pageCache.getRemedies(abbrev, symptom, if (remedyString.length == 0) None else Some(remedyString), minWeight)
      val getRemedies = if (cachedRemedies.length == 0) "1" else "0"

      // If no results were found, either tell the user in the input screen,
      // or, if search was invoked via static /show-link (i.e., there is no input screen), then display
      // clean error page.
      def handleNoResultsFound() = MainView.loadingSpinner() match {
        case Some(spinner) if (spinner.isVisible()) =>
          val errorMessage = s"ERROR: Lookup failed. Perhaps URL malformed, repertory does not exist or no results for given symptoms. " +
            s"SUGGESTED SOLUTION: Go directly to ${serverUrl()} instead and try again!"
          dom.document.location.replace(s"${serverUrl()}/${apiPrefix()}/display_error_page?message=${encodeURI(errorMessage)}")
        case _ =>
          showRepertorisationResultsError(symptom, remedyString, minWeight)
      }

      HttpRequest2("lookup_rep")
        .withQueryParameters(
          ("symptom", symptom),
          ("repertory", abbrev),
          ("page", page.toString),
          ("remedyString", remedyString),
          ("minWeight", minWeight.toString),
          ("getRemedies", getRemedies)
        )
        .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
        .onSuccess((response: String) =>
          parse(response) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[(ResultsCaseRubrics, List[ResultsRemedyStats])] match {
                case Right((rcr, remedyStats)) => {
                  cachedRemedies match {
                    case Nil => {
                      if (getRemedies == "1")
                        _resultRemedyStats.set(remedyStats)
                      showRepertorisationResults(rcr, remedyStats)
                    }
                    case cachedRemedies =>
                      showRepertorisationResults(rcr, cachedRemedies)
                  }
                }
                case Left(err) =>
                  println(s"Parsing of lookup as RepertoryLookup failed: $err")
                  handleNoResultsFound()
              }
            }
            case Left(err) =>
              println(s"Parsing of lookup failed (is it JSON?): $err")
              handleNoResultsFound()
          }
        )
        .onFailure((_) =>
          handleNoResultsFound()
        )
        .send()
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

        if (symptom.trim.replaceAll("[^A-Za-z0-9äüöÄÜÖß]", "").length == 0 && abbrevForRemedyEntered == None) {
          resultStatusDiv.innerHTML = ""
          resultStatusDiv.appendChild(
            div(cls := "alert alert-danger", role := "alert",
              b("No results. Enter some symptoms and/or a remedy.")).render)
          return
        }

        if (symptom.trim.replaceAll("[^A-Za-z0-9äüöÄÜÖß]", "").length <= 3 && abbrevForRemedyEntered == None) {
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

    dom.document.body.classList.add("wait")

    _pageCache.getPage(abbrev, symptom, abbrevForRemedyEntered, minWeight, page) match {
      case Some(cachedPage) =>
        showRepertorisationResults(cachedPage.content, cachedPage.remedies)
      case None =>
        getPageFromBackend(abbrev, symptom, abbrevForRemedyEntered.getOrElse(""), minWeight, page)
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def updateAvailableRepertoriesAndRemedies(remedies: List[Remedy], runAfterUpdate: (() => Unit)): Unit = {
    if (_remedies.isEmpty()) {
      _remedies = new Remedies(remedies)

      HttpRequest2("available_rems_and_reps")
        .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
        .onSuccess((response: String) => {
          parse(response) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[InfoExtended]] match {
                case Right(extendedRepertoryInfos) => {
                  // Update the default repertory, if one was transmitted from backend (which should ALWAYS be the case)
                  extendedRepertoryInfos.toList.filter(_.info.access == ResourceAccessLvl.Default) match {
                    case rep :: Nil => _defaultRepertory = rep.info.abbrev
                    case _ => println("WARNING: Not setting default repertory; none was transmitted")
                  }

                  for (rep <- extendedRepertoryInfos) {
                    val repRemedies = rep.remedyIds.map(_remedies.get(_)).flatten
                    _repertories.put(rep, new Remedies(repRemedies))
                  }

                  // Update available repertories in repertory dropdown button
                  if (dom.document.getElementById("repSelectionDropDown") != null && dom.document.getElementById("repSelectionDropDown").childNodes.length == 0) {
                    extendedRepertoryInfos.map(_.info)
                      .sortBy(_.abbrev)
                      .foreach {
                        case currRep => {
                          dom.document.getElementById("repSelectionDropDown")
                            .appendChild(a(cls := "dropdown-item", href := "#", data.value := currRep.abbrev,
                              onclick := { (event: Event) =>
                                _selectedRepertory.set(currRep.abbrev)
                                dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[dom.html.Button].textContent = "Repertory: " + _selectedRepertory
                              }, s"${currRep.abbrev} - ${currRep.displaytitle.getOrElse("")}").render)

                          if (_selectedRepertory.toString().length > 0)
                            dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[dom.html.Button].textContent = "Repertory: " + _selectedRepertory
                          else {
                            _selectedRepertory.set(_defaultRepertory)
                            dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[dom.html.Button].textContent = "Repertory: " + _selectedRepertory
                          }
                        }
                      }
                  }

                  runAfterUpdate()
                }
                case Left(t) => println("Parsing of available repertories failed: " + t)
              }
            }
            case Left(_) => println("Parsing of available repertories failed (is it JSON?).")
          }
        })
        .send()
    }
    // This branch is executed, for example, for redrawing drop down when search results are being shown
    // TODO: Is this still true? Is this branch EVER executed? I doubt it.
    else {
      println("INFO: Updating repertory information from RAM")
      if (dom.document.getElementById("repSelectionDropDown").childNodes.length == 0) {
        for (currRep <- _repertories.getInfos().map(_.info).sortBy(_.abbrev)) {
          dom.document.getElementById("repSelectionDropDown")
            .appendChild(a(cls := "dropdown-item", href := "#", data.value := currRep.abbrev,
              onclick := { (event: Event) =>
                _selectedRepertory.set(currRep.abbrev)
                dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[dom.html.Button].textContent = "Repertory: " + _selectedRepertory
              }, s"${currRep.abbrev} - ${currRep.displaytitle.getOrElse("")}").render)
        }
      }

      if (_selectedRepertory.toString().length == 0)
        _selectedRepertory.set(_defaultRepertory)
      dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[dom.html.Button].textContent = "Repertory: " + _selectedRepertory
    }
  }

  override def getPrefix(): String = _prefix

  override def tabLinkId(): String = "tab_repertory_link"

  override def tabPaneId(): String = "tab_repertory"

  override def toFront(): Unit = {
    dom.document.getElementById(tabLinkId()).classList.add("show")
    dom.document.getElementById(tabLinkId()).classList.add("active")
    dom.document.getElementById(tabPaneId()).classList.add("show")
    dom.document.getElementById(tabPaneId()).classList.add("active")
  }

  override def toBack(): Unit = {
    dom.document.getElementById(tabLinkId()).classList.remove("show")
    dom.document.getElementById(tabLinkId()).classList.remove("active")
    dom.document.getElementById(tabPaneId()).classList.remove("show")
    dom.document.getElementById(tabPaneId()).classList.remove("active")
  }

  override def tabLink() = {
    a(cls:="nav-link active", href:=s"#${tabPaneId()}", `id`:=s"${tabLinkId()}", data.toggle:="tab",
      // TODO: This is a workaround for the fact that showResults() uses a lot of getElementById()
      // and that requires the dom to be already rendered to the browser's page, and withResults()
      // doesn't render, it only returns the HTML.  Since showResults() is so HUGE, we can't simply
      // refactor it. So, for now, we just call showResults() whenever a redraw event occurs, cause
      // then the dom certainly exists.
      onshow := {( (event: Event) =>
        if (_repertorisationResults.get() != None || Case.size() > 0)
          showResults()
        )
      },
      "Repertory")
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def createView(myHTML: JsDom.TypedTag[dom.html.Div] => JsDom.TypedTag[dom.html.Div]) = {
    // Make sure, the navbar is visible
    dom.document.getElementById("nav_bar") match {
      case null => ;
      case navBar => navBar.classList.remove("d-none")
    }

    // Make sure, the loading animation is gone
    MainView.loadingSpinner() match {
      case None => ;
      case Some(spinner) => spinner.remove()
    }

    val repSelectionDropDownDiv =
      div(cls:="dropdown-menu", `id`:="repSelectionDropDown").render

    val repSelectionDropDownButton =
      button(`type`:="button",
        style:="min-width: 195px;",
        cls:="text-nowrap btn btn-block dropdown-toggle btn-secondary",
        data.toggle:="dropdown",
        `id`:="repSelectionDropDownButton",
        "Repertories").render

    // The main repertory selection div and button...
    val ulRepertorySelection =
      div(cls:="dropdown col-md-2", style:="min-width:200px; margin-top:20px;", repSelectionDropDownButton, repSelectionDropDownDiv)

    // Now fill them with data...
    if (!_repertories.isEmpty() && repSelectionDropDownDiv.childNodes.length == 0) {
      _repertories.getInfos().map(_.info)
        .sortBy(_.abbrev)
        .foreach {
          case currRep => {
            repSelectionDropDownDiv
              .appendChild(a(cls := "dropdown-item", href := "#", data.value := currRep.abbrev,
                onclick := { (event: Event) =>
                  _selectedRepertory.set(currRep.abbrev)
                  dom.document.getElementById("repSelectionDropDownButton").asInstanceOf[dom.html.Button].textContent = "Repertory: " + _selectedRepertory
                }, s"${currRep.abbrev} - ${currRep.displaytitle.getOrElse("")}").render)
          }
        }
    }

    if (_selectedRepertory.toString().length > 0)
      repSelectionDropDownButton.textContent = "Repertory: " + _selectedRepertory
    else
      repSelectionDropDownButton.textContent = "Repertory: " + _defaultRepertory

    myHTML(ulRepertorySelection) // .asInstanceOf[dom.html.Html]
  }

  // ------------------------------------------------------------------------------------------------------------------
  // override def drawWithoutResults(): JsDom.TypedTag[dom.html.Div] = {
  override def drawWithoutResults() = {
    def myHTML(ulRepertorySelection: JsDom.TypedTag[dom.html.Div]) =
      div(cls := "container-fluid text-center",
        div(cls := "row",
          div(cls := "col-sm-12",
            div(cls := "row",
              ulRepertorySelection,
              div(cls := "col-sm", style:="margin-top:20px;",
                input(cls := "form-control", `id` := "inputField",
                  onkeydown := { (event: dom.KeyboardEvent) =>
                    if (event.keyCode == 13) {
                      event.stopPropagation()
                      onSymptomEntered()
                    }
                  }, `placeholder` := "Enter a symptom (for example: head, pain, left)")
              ),
            ),
          )
        ),
        // We copy above div's, because this is as wide as the search bar. It will be (de-) populated on demand.
        div(cls := "row",
          div(cls := "col-sm-12",
            div(cls := "row justify-content-center",
              div(cls := "col-sm-10", id:="advancedSearchControlsDiv")
            )
          )
        ),
        div(id:="mainViewSearchButtons", cls:="col-sm-12 text-center", style:="margin-top:20px;",
          button(cls := "btn btn-primary text-nowrap", style := "width: 140px; margin: 5px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              onSymptomEntered()
            }, span(cls := "oi oi-magnifying-glass", title := "Find", aria.hidden := "true"), " Find"),
          button(id:="buttonMainViewAdvancedSearch", cls := "btn btn-secondary text-nowrap", style := "width: 140px; margin: 5px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              onShowAdvancedSearchOptionsMainView(false, true)
            }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"), " Advanced...")
        ),
        div(cls := "container-fluid", style := "margin-top: 23px;", id := "resultStatus"),
        div(cls := "container-fluid", id := "resultDiv"),
        div(cls := "container-fluid", id := s"${_prefix}_paginationDiv"),
      )
    createView(myHTML)
  }

  // ------------------------------------------------------------------------------------------------------------------
  override def drawWithResults() = {
    def myHTML(ulRepertorySelection: JsDom.TypedTag[dom.html.Div]): JsDom.TypedTag[dom.html.Div]  =
      div(cls := "container-fluid",
        new ShareResultsModal(_prefix, resultsLink).apply(),
        div(cls := "container-fluid text-center",
          div(cls := "row justify-content-center",
            div(cls := "col-md-12 justify-content-center",
              div(cls := "row justify-content-center",
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
                  )
                ),
                div(id:="mainViewSearchButtons", cls:="col-md-auto text-center center-block", style:="margin-top:20px;",
                  button(cls := "btn btn-primary text-nowrap", style:="width: 80px; margin-right:5px;", `type` := "button",
                    onclick := { (event: Event) =>
                      event.stopPropagation()
                      onSymptomEntered()
                    },
                    span(cls := "oi oi-magnifying-glass", title := "Find", aria.hidden := "true")),
                  button(`id`:="buttonMainViewAdvancedSearch", cls := "btn btn-secondary text-nowrap", style := "width: 70px;margin-right:5px;", `type` := "button",
                    onclick := { (event: Event) =>
                      event.stopPropagation()
                      onShowAdvancedSearchOptionsMainView(false, false)
                    },
                    span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true")),
                  button(cls := "btn btn-secondary text-nowrap", style := "width: 70px;", `type` := "button",
                    onclick := { (event: Event) =>
                      event.stopPropagation()
                      onSymptomListRedoPressed()
                    },
                    span(cls := "oi oi-action-redo", title := "Find again", aria.hidden := "true"))
                )
              )
            ),
            // It will be (de-) populated on demand.
            div(cls:="col-md-10", id:="advancedSearchControlsDiv")
          )
        ),
        div(cls := "container-fluid", style := "margin-top: 23px;", id := "resultStatus"),
        div(cls := "container-fluid", id := "resultDiv"),
        div(cls := "container-fluid", id := s"${_prefix}_paginationDiv"),
      )

    createView(myHTML) // .asInstanceOf[dom.html.Html]
  }

  override def onResultsDrawn() = {
    if (_advancedSearchOptionsVisible == true)
      onShowAdvancedSearchOptionsMainView(true, false)
  }

  override def containsAnyResults(): Boolean = {
    if (_repertorisationResults.get() != None)
      _repertorisationResults.get().size > 0
    else
      false
  }

  override def containsUnsavedResults(): Boolean = {
    Case.containsUnsavedResults()
  }

  override def updateDataStructures(remedies: List[Remedy]): Unit = {
    // See also MateriaMedicaView.scala for a similar after-update-handler!
    def runAfterUpdate(): Unit = {
      if (_selectedRepertory.toString() == "")
        _selectedRepertory.set(_defaultRepertory)

      // The following code would work as in MateriaMedicaView.scala, but we don't need to draw twice, unless we have to:
      //
      //      if (containsAnyResults()) {
      //        MainView.resetContentView()
      //        MainView.tabToFront(this)
      //        showResults()
      //      }
    }

    updateAvailableRepertoriesAndRemedies(remedies, runAfterUpdate)
  }

}
