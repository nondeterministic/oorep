package org.multics.baueran.frep.shared.frontend

import scalatags.rx.all._
import scalatags.JsDom
import org.scalajs.dom.raw.HTMLInputElement
import JsDom.all._
import fr.hmil.roshttp.{BackendConfig, HttpRequest}
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import org.scalajs.dom
import dom.html.{Div, Element}
import dom.Event
import monix.execution.Scheduler.Implicits.global
import rx.{Rx, Var}
import rx.Ctx.Owner.Unsafe._

import scala.util.{Failure, Success}
import org.multics.baueran.frep.shared.{HitsPerRemedy, MMAllSearchResults, MMAndRemedyIds, MMSearchResult, MateriaMedicas, Paginator, Remedies, Remedy}
import org.multics.baueran.frep.shared.Defs.{maxLengthOfSymptoms, maxNumberOfResultsPerMMPage}
import org.multics.baueran.frep.shared.Defs.ResourceAccessLvl

object MateriaMedicaView extends TabView {

  private val _prefix = "MMView"

  // private var _showMaxSearchResultsAlert = true
  private val _collapsedMultiOccurrencesSpans = Var(true)
  private var _defaultMMAbbrev: Option[String] = None

  private val _selectedMateriaMedicaAbbrev: Var[Option[String]] = Var(None)
  private var _remedies: Remedies = _
  private val _materiaMedicas: Var[MateriaMedicas] = Var(new MateriaMedicas(List()))

  private val _latestSymptomString: Var[Option[String]] = Var(None)
  private val _latestRemedyString: Var[Option[String]] = Var(None)
  private val _sectionHits: Var[List[MMSearchResult]] = Var(List.empty)
  private var _totalNumberOfResultRemedies = 0
  private var _page: Option[Int] = None
  private val _pageCache = new PageCacheMM()
  private var _allSectionsHide = true
  private var _advancedSearchOptionsVisible = false

  private val ulMMSelection =
    div(cls := "dropdown col-md-2", style := "min-width:200px; margin-top:20px;",
      button(`type` := "button",
        style := "min-width: 195px;",
        cls := "text-nowrap btn btn-block dropdown-toggle btn-secondary",
        data.toggle := "dropdown",
        `id` := s"${_prefix}mmSelectionDropDownButton",
        "Materia Medicas"),
      div(cls := "dropdown-menu", `id` := s"${_prefix}mmSelectionDropDown",
        Rx(_materiaMedicas().getAll()
          .sortBy{ case (mminfo, remedies) => mminfo.abbrev }
          .map { case (mminfo, remedies) => {
            a(cls := "dropdown-item", href := "#",
              onclick := { (_: Event) =>
                _selectedMateriaMedicaAbbrev() = Some(mminfo.abbrev)
                dom.document.getElementById(s"${_prefix}mmSelectionDropDownButton").asInstanceOf[dom.html.Button].textContent =
                  s"M. Medica: ${mminfo.abbrev}"
                refreshRemedyDataList()
              },
              s"${mminfo.abbrev} - ${mminfo.displaytitle.getOrElse("")}"
            ).render
          }
        })
      )
    )

  _sectionHits.triggerLater {
    dom.document.getElementById(s"${_prefix}_result_div").innerHTML = ""
    dom.document.getElementById(s"${_prefix}_result_div").appendChild(getResultsHtml().render)
    dom.document.getElementById(s"${_prefix}_result_div").appendChild(div(cls:= "container-fluid", id:=s"${_prefix}_paginationDiv").render)

    refreshMMDropDownButtonLabel()

    renderMultiOccurrencesDiv()

    // TODO: If we, one day, actually want to show the case under the MM view as well, we only should have to uncomment those lines here (more or less)...
    // MainView.CaseDiv.empty()
    // MainView.CaseDiv.append(Case.toHTML(RemedyFormat.Abbreviated).render)

    getPaginatorHtml() match {
      case None => ;
      case Some(resultDiv) => dom.document.getElementById(s"${_prefix}_paginationDiv").appendChild(resultDiv.render)
    }
    ()
  }

  private def refreshRemedyDataList(): Unit = {
    val selectedAbbrev = _selectedMateriaMedicaAbbrev.now.getOrElse("")

    if (selectedAbbrev.length == 0)
      return

    _materiaMedicas.now.get(selectedAbbrev) match {
      case Some((info, remedies)) => refreshRemedyDataList(remedies)
      case None => return
    }
  }

  private def refreshMMDropDownButtonLabel(): Unit = {
    dom.document.getElementById(s"${_prefix}mmSelectionDropDownButton") match {
      case null => ;
      case mmSelectionButton =>
        if (_selectedMateriaMedicaAbbrev.now == None && _defaultMMAbbrev != None)
          mmSelectionButton.asInstanceOf[dom.html.Button].textContent = s"M. Medica: ${_defaultMMAbbrev}"
        else if (_selectedMateriaMedicaAbbrev.now != None)
          mmSelectionButton.asInstanceOf[dom.html.Button].textContent = s"M. Medica: ${_selectedMateriaMedicaAbbrev.now.getOrElse("Materia Medicas")}"
        else {
          ; // do nothing
        }
    }
  }

  private def renderMultiOccurrencesDiv(): Unit = {
    implicit class RemedyOrdered(val remedy: Option[Remedy])
      extends Ordered[RemedyOrdered] {
      def compare(other: RemedyOrdered): Int = {
        (this.remedy, other.remedy) match {
          case (Some(r1), Some(r2)) => r1.nameAbbrev.compare(r2.nameAbbrev)
          case _ => 0 // Should never fire!
          // a negative int if this < that
          // 0 if this == that
          // a positive int if this > that
        }
      }
    }

    dom.document.getElementById(s"${_prefix}_multiOccurrencesContentSpan") match {
      // If multi-occurrences aren't there, for whatever reason, paint them!
      case null => {
        val multiOccurrencesSpan = span(id := s"${_prefix}_multiOccurrencesContentSpan", cls := s"${if (_collapsedMultiOccurrencesSpans.now) "hide" else "show"} collapse").render

        dom.document.getElementById(s"${_prefix}_multiOccurrencesDiv") match {
          case null => ;
          case multiOccDiv =>
            multiOccDiv.asInstanceOf[dom.html.Div].appendChild(multiOccurrencesSpan)
        }

        if (_pageCache.length() > 0 && _pageCache.latest.content.numberOfMatchingSectionsPerChapter.length > 1) {
          val multiOccurrencesLinks = _pageCache.latest.content.numberOfMatchingSectionsPerChapter
            .sortWith((h1, h2) => _remedies.get(h1.remedyId) > _remedies.get(h2.remedyId)) // Uses the above implicit!
            .sortBy(-_.hits)
            .map {
              case HitsPerRemedy(numberOfHits, remedyId) => {
                val remedyAbbrev = _remedies.get(remedyId) match {
                  case Some(remedy) => Some(remedy.nameAbbrev)
                  case None => None
                }
                span(numberOfHits.toString + "x",
                  a(href := "#", onclick := { (_: Event) => doLookup(_pageCache.latest.abbrev, "", None, remedyAbbrev) },
                    // ### Should never be seen by the user. Could only happen if remedy data transfer from backend was incomplete.
                    remedyAbbrev.getOrElse("###").toString
                  )
                )
              }
            }
          multiOccurrencesSpan.appendChild(span(" (All matching chapters: ").render)
          multiOccurrencesLinks.take(multiOccurrencesLinks.length - 1).foreach(lnk => multiOccurrencesSpan.appendChild(span(lnk, ", ").render))
          multiOccurrencesLinks.takeRight(1).foreach(lnk => multiOccurrencesSpan.appendChild(span(lnk, ")").render))
        }
        else
          multiOccurrencesSpan.appendChild(span(" (No further matching chapters.)").render)
      }
      // If multi-occurrences already there, even if collapse, do nothing!
      case multiOccurrencesContentSpan => ;
    }
  }

  private def getPaginatorHtml(): Option[JsDom.TypedTag[Element]] = {
    if (_totalNumberOfResultRemedies <= maxNumberOfResultsPerMMPage)
      return None

    val totalNumberOfPages = math.ceil(_totalNumberOfResultRemedies.toDouble / maxNumberOfResultsPerMMPage.toDouble).toInt
    val pg = new Paginator(totalNumberOfPages, _page.getOrElse(0), 5).getPagination()
    val htmlPg = new PaginatorHtml(s"${_prefix}_paginationDiv", pg)

    Some(htmlPg.toHtml(_pageCache.latest.abbrev, _pageCache.latest.symptom, _pageCache.latest.remedy, doLookup))
  }

  private def getResultsHtml(): JsDom.TypedTag[Div] = {
    if (_sectionHits.now.length > 0) {

      // These are the onclick-handlers for unfolding or collapsing all result sections at once
      def hideLinksHandler(event: Event) = {
        val allMMSectionSpans = dom.document.querySelectorAll(s"div[type^=${_prefix}_mm_section_span]")
        for (i <- 0 to allMMSectionSpans.length - 1) {
          val currSpan = allMMSectionSpans.item(i).asInstanceOf[dom.html.Div]
          currSpan.setAttribute("class", "collapse hide")
        }

        val allMMSectionChevronSpans = dom.document.querySelectorAll(s"span[type^=${_prefix}_mm_section_chevron_span]")
        for (i <- 0 to allMMSectionChevronSpans.length - 1) {
          val currSpan = allMMSectionChevronSpans.item(i).asInstanceOf[dom.html.Div]
          currSpan.setAttribute("class", "oi oi-chevron-right")
        }
        _allSectionsHide = true
      }

      def showLinksHandler(event: Event) = {
        val allMMSectionSpans = dom.document.querySelectorAll(s"div[type^=${_prefix}_mm_section_span]")
        for (i <- 0 to allMMSectionSpans.length - 1) {
          val currSpan = allMMSectionSpans.item(i).asInstanceOf[dom.html.Div]
          currSpan.setAttribute("class", "collapse show")
        }

        val allMMSectionChevronSpans = dom.document.querySelectorAll(s"span[type^=${_prefix}_mm_section_chevron_span]")
        for (i <- 0 to allMMSectionChevronSpans.length - 1) {
          val currSpan = allMMSectionChevronSpans.item(i).asInstanceOf[dom.html.Div]
          currSpan.setAttribute("class", "oi oi-chevron-bottom")
        }
        _allSectionsHide = false
      }

      // Here starts the actual HTML which is returned by this function...
      div(

        div(id:=s"${_prefix}_resultStatusAlerts",

          div(id:=s"${_prefix}_multiOccurrencesDiv", cls:="alert alert-secondary", role:="alert",
            button(`type` := "button", cls := "close", data.toggle := "collapse", data.target := s"#${_prefix}_multiOccurrencesContentSpan",
              onclick := { (event: Event) =>
                dom.document.getElementById(s"${_prefix}_collapseMultiOccurrencesButton") match {
                  case null => ;
                  case toggleSpan => {
                    if (toggleSpan.getAttribute("class") == "oi oi-chevron-left") {
                      toggleSpan.setAttribute("class", "oi oi-chevron-bottom")
                      toggleSpan.setAttribute("title", "Show less")
                      _collapsedMultiOccurrencesSpans() = false
                    }
                    else {
                      toggleSpan.setAttribute("class", "oi oi-chevron-left")
                      toggleSpan.setAttribute("title", "Show more...")
                      _collapsedMultiOccurrencesSpans() = true
                    }
                  }
                }
              },
              if (_collapsedMultiOccurrencesSpans.now)
                span(aria.hidden := "true", id := s"${_prefix}_collapseMultiOccurrencesButton", cls := "oi oi-chevron-left", style := "font-size: 14px;", title := "Show more...")
              else
                span(aria.hidden := "true", id := s"${_prefix}_collapseMultiOccurrencesButton", cls := "oi oi-chevron-bottom", style := "font-size: 14px;", title := "Show less")
            ),
            {
              var searchResultsTxt = ""

              (_latestSymptomString.now, _latestRemedyString.now) match {
                case (Some(symptomString), Some(remedyName)) if (symptomString.trim.length > 0 && remedyName.trim.length > 0) =>
                  searchResultsTxt = s"${_totalNumberOfResultRemedies} chapter(s) match '${symptomString}' whose title(s) start with '${remedyName}'. " +
                    s"Showing page ${_page.getOrElse(0) + 1} of ${math.ceil(_totalNumberOfResultRemedies.toDouble / maxNumberOfResultsPerMMPage.toDouble).toInt}."
                case (Some(symptomString), _) if (symptomString.trim.length > 0) =>
                  searchResultsTxt = s"${_totalNumberOfResultRemedies} chapter(s) match '${symptomString}'. " +
                    s"Showing page ${_page.getOrElse(0) + 1} of ${math.ceil(_totalNumberOfResultRemedies.toDouble / maxNumberOfResultsPerMMPage.toDouble).toInt}."
                case (_, Some(remedyName)) if (remedyName.trim.length > 0) =>
                  searchResultsTxt = s"${_totalNumberOfResultRemedies} chapter title(s) start with '${remedyName}'. " +
                    s"Showing page ${_page.getOrElse(0) + 1} of ${math.ceil(_totalNumberOfResultRemedies.toDouble / maxNumberOfResultsPerMMPage.toDouble).toInt}."
                case _ =>
                  searchResultsTxt = s"Something went wrong. Please, try again."
              }

              b(searchResultsTxt)
            }
          ),

  //        // TODO: In MM, does this warning really make sense?
  //        if (_sectionHits.now.length > 2 * maxNumberOfResultsPerMMPage) {
  //          div(cls := "alert alert-warning", role := "alert",
  //            button(`type` := "button", cls := "close", data.dismiss := "alert", onclick := { (_: Event) => _showMaxSearchResultsAlert = false }, span(aria.hidden := "true", raw("&times;"))),
  //            b(s"High number of results. Maybe try narrowing your search using '-' and '*', like '-left*'.")
  //          )
  //        } else {
  //          div()
  //        },

          div(cls := "alert alert-info", role := "alert",
            button(`type` := "button", cls := "close", data.dismiss := "alert", span(aria.hidden := "true", raw("&times;"))),
            b(a(href := "#", onclick := {
              showLinksHandler(_)
            }, "Show all result sections"),
              raw(" | "),
              a(href := "#", onclick := {
                hideLinksHandler(_)
              }, "Hide all result sections")
            )
          ),
        ),

        _sectionHits.now.map(_.render(_prefix, _allSectionsHide, _latestSymptomString.now.getOrElse(""), _materiaMedicas.now, doLookup))

      )
    }
    else if (_latestSymptomString.now.getOrElse("").trim.length > 0 || _latestRemedyString.now.getOrElse("").trim.length > 0) {
      var errorMessageTxt = ""

      (_latestSymptomString.now, _latestRemedyString.now) match {
        case (Some(symptomString), Some(remedyName)) if (symptomString.trim.length > 0 && remedyName.trim.length > 0) =>
          errorMessageTxt = s"No results returned for '${symptomString}' and remedy '${remedyName}'."
        case (Some(symptomString), _) if (symptomString.trim.length > 0) =>
          errorMessageTxt = s"No results returned for '${symptomString}'."
        case (_, Some(remedyName)) if (remedyName.trim.length > 0) =>
          errorMessageTxt = s"Remedy abbreviation or fullname '${remedyName}' does not exist in this materia medica."
        case _ =>
          errorMessageTxt = s"Something went wrong. Please, try again."
      }

      div(id:=s"${_prefix}_resultStatusAlerts",
        div(cls := "alert alert-danger", role := "alert", b(errorMessageTxt))
      )
    }
    else {
      div(id:=s"${_prefix}_resultStatusAlerts")
    }
  }

  override def getPrefix(): String = _prefix

  override def tabLinkId(): String = s"${_prefix}_tab_materia_medica_link"

  override def tabPaneId(): String = s"${_prefix}_tab_materia_medica"

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
    a(cls := "nav-link", href := s"#${tabPaneId()}", id := tabLinkId(), data.toggle := "tab",
      onclick := { (_: Event) =>
        dom.document.getElementById(s"${_prefix}_result_div") match {
          case null => ;
          case resultDiv =>
            resultDiv.innerHTML = ""
            resultDiv.appendChild(
              div(getResultsHtml(),
                div(cls:= "container-fluid", id:=s"${_prefix}_paginationDiv",
                  getPaginatorHtml() match {
                    case None => div()
                    case Some(resultDiv) => resultDiv
                  }
                )
              ).render
            )
        }

        MainView.CaseDiv.empty()
        refreshMMDropDownButtonLabel()
        renderMultiOccurrencesDiv()
      },
      "Materia Medica")
  }

  private def updateAvailableMMsAndRemedies(remedies: List[Remedy]): Unit = {
    _remedies = new Remedies(remedies)

    if (_materiaMedicas.now.size() == 0) {
      HttpRequest(s"${serverUrl()}/${apiPrefix()}/available_rems_and_mms")
        .send()
        .onComplete({
          case response: Success[SimpleHttpResponse] => {
            parse(response.get.body) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[List[MMAndRemedyIds]] match {
                  case Right(mmsAndRemedies) => {
                    // Fill dropdown menu by triggering the corresponding RX...
                    _materiaMedicas() = new MateriaMedicas(
                      mmsAndRemedies.map{ case MMAndRemedyIds(mminfo, remedyIds) =>
                        (mminfo, remedyIds.map(_remedies.get(_)).flatten)
                      }
                    )

                    // Determine default materia medica...
                    if (_selectedMateriaMedicaAbbrev.now == None) {
                      mmsAndRemedies.filter(_.mminfo.access == ResourceAccessLvl.Default.toString) match {
                        case defaultMateriaMedica :: _ =>
                          _selectedMateriaMedicaAbbrev() = Some(defaultMateriaMedica.mminfo.abbrev)
                          dom.document.getElementById(s"${_prefix}mmSelectionDropDownButton") match {
                            case null => ; // Do nothing: this triggers, e.g., when user calls direct link to repertorisation results (/show?)
                            case dropDownButton =>
                              dropDownButton.asInstanceOf[dom.html.Button].textContent =
                                s"M. Medica: ${defaultMateriaMedica.mminfo.abbrev}"
                          }
                          _defaultMMAbbrev = Some(defaultMateriaMedica.mminfo.abbrev)
                        case Nil => ;
                      }
                    }
                  }
                  case Left(error) => println(s"Materia medica decoding failed: $error")
                }
              }
              case Left(error) => println(s"Materia medica parsing failed: $error")
            }
          }
          case error: Failure[SimpleHttpResponse] => println(s"Materia medica information not received from backend: ${error.toString}")
        })
    }
    else
      refreshMMDropDownButtonLabel()

    renderMultiOccurrencesDiv()
  }

  private def onShowAdvancedSearchOptions(): Unit = {
    // If for some reason the remedy input is already visible (which it shouldn't be), do nothing and return.
    if (dom.document.getElementById(s"${_prefix}_inputRemedy") != null)
      return

    val parentDiv = dom.document.getElementById(s"${_prefix}_advancedSearchControlsDiv")

    if (parentDiv == null)
      return

    parentDiv.asInstanceOf[dom.html.Div].appendChild(
      div(id := s"${_prefix}_advancedSearchControlsContent", cls := "row", style := "margin-top:15px;",
        div(cls := "col-md-auto my-auto",
          "Remedy:"),
        div(cls := "col",
          input(cls := "form-control", `id` := s"${_prefix}_inputRemedy", list := s"${_prefix}_remedyDataList",
            onkeydown := { (event: dom.KeyboardEvent) =>
              if (event.keyCode == 13) {
                event.stopPropagation()
                val symptom = dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[HTMLInputElement].value
                val abbrev = _selectedMateriaMedicaAbbrev.now.getOrElse("")
                val remedyName = dom.document.getElementById(s"${_prefix}_inputRemedy") match {
                  case null => None
                  case remedyDataList => Some(remedyDataList.asInstanceOf[dom.html.Input].value.trim)
                }
                doLookup(abbrev, symptom, None, remedyName)
              }
            }, `placeholder` := "Enter a remedy abbreviation or fullname (for example: Sil. or Silica)"),
          datalist(`id` := s"${_prefix}_remedyDataList")
        )
      ).render)

    // Add currently selected repertory's remedies for search
    refreshRemedyDataList()

    val advancedButton = dom.document.getElementById(s"${_prefix}_buttonMainViewAdvancedSearch").asInstanceOf[dom.html.Button]
    val buttonDiv = dom.document.getElementById(s"${_prefix}_mainViewSearchButtons").asInstanceOf[dom.html.Div]
    buttonDiv.replaceChild(showBasicSearchOptionsButton().render, advancedButton)

    _advancedSearchOptionsVisible = true
  }

  private def showAdvancedSearchOptionsButton(): JsDom.TypedTag[dom.html.Button] = {
    if (!MainView.someResultsHaveBeenShown())
      button(id := s"${_prefix}_buttonMainViewAdvancedSearch", cls := "btn btn-secondary text-nowrap", style := "width: 140px; margin: 5px;", `type` := "button",
        onclick := { (event: Event) =>
          event.stopPropagation()
          onShowAdvancedSearchOptions()
        }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"), " Advanced...")
    else
      button(id := s"${_prefix}_buttonMainViewAdvancedSearch", cls := "btn btn-secondary text-nowrap", style := "width: 70px; margin-right: 5px;", `type` := "button",
        onclick := { (event: Event) =>
          event.stopPropagation()
          onShowAdvancedSearchOptions()
        }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"))
  }

  private def onShowBasicSearchOptions(): Unit = {
    val parentDiv = dom.document.getElementById(s"${_prefix}_advancedSearchControlsDiv").asInstanceOf[dom.html.Div]
    parentDiv.innerHTML = ""

    val basicButton = dom.document.getElementById(s"${_prefix}_buttonMainViewBasicSearch").asInstanceOf[dom.html.Button]
    val buttonDiv = dom.document.getElementById(s"${_prefix}_mainViewSearchButtons").asInstanceOf[dom.html.Div]
    buttonDiv.replaceChild(showAdvancedSearchOptionsButton().render, basicButton)

    _advancedSearchOptionsVisible = false
  }

  private def showBasicSearchOptionsButton(): JsDom.TypedTag[dom.html.Button] = {
    if (!MainView.someResultsHaveBeenShown())
      button(id := s"${_prefix}_buttonMainViewBasicSearch", cls := "btn btn-secondary text-nowrap", style := "width: 140px; margin: 5px;", `type` := "button",
        onclick := { (event: Event) =>
          event.stopPropagation()
          onShowBasicSearchOptions()
        }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"), " Basic")
    else
      button(id := s"${_prefix}_buttonMainViewBasicSearch", cls := "btn btn-secondary text-nowrap", style := "width: 70px; margin-right: 5px;", `type` := "button",
        onclick := { (event: Event) =>
          event.stopPropagation()
          onShowBasicSearchOptions()
        }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"))
  }

  override def drawWithoutResults(): JsDom.TypedTag[Div] = {
    div(cls := "container-fluid text-center",
      div(cls := "row",
        div(cls := "row col-sm-12",
          ulMMSelection,
          div(cls := "col-sm", style := "margin-top:20px;",
            input(cls := "form-control", `id` := s"${_prefix}inputField",
              onkeydown := { (event: dom.KeyboardEvent) =>
                if (event.keyCode == 13) {
                  event.stopPropagation()
                  val symptom = dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[HTMLInputElement].value
                  val abbrev = _selectedMateriaMedicaAbbrev.now.getOrElse("")
                  val remedyName = dom.document.getElementById(s"${_prefix}_inputRemedy") match {
                    case null => None
                    case remedyDataList => Some(remedyDataList.asInstanceOf[dom.html.Input].value.trim)
                  }
                  doLookup(abbrev, symptom, None, remedyName)
                }
              }, `placeholder` := "Enter some search terms (for example: menses, night)")
          ),
        )
      ),
      div(cls := "row",
        div(cls := "row col-sm-12",
          div(cls := "col-sm-2"),
          div(cls := "col-sm-10", id := s"${_prefix}_advancedSearchControlsDiv")
        )
      ),
      div(id := s"${_prefix}_mainViewSearchButtons", cls := "col-sm-12 text-center", style := "margin-top:20px;",
        button(cls := "btn btn-primary text-nowrap", style := "width: 140px; margin:5px;", `type` := "button",
          onclick := { (event: Event) =>
            event.stopPropagation()
            val symptom = dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[HTMLInputElement].value
            val abbrev = _selectedMateriaMedicaAbbrev.now.getOrElse("")
            val remedyName = dom.document.getElementById(s"${_prefix}_inputRemedy") match {
              case null => None
              case remedyDataList => Some(remedyDataList.asInstanceOf[dom.html.Input].value.trim)
            }
            doLookup(abbrev, symptom, None, remedyName)
          }, span(cls := "oi oi-magnifying-glass", title := "Find", aria.hidden := "true"), " Find"),
        showAdvancedSearchOptionsButton
      ),
      div(id:=s"${_prefix}_resultStatusAlerts", cls:="container-fluid", style:="margin-top:23px;")
    )
  }

  override def drawWithResults(): JsDom.TypedTag[dom.html.Div] = {
    div(cls := "container-fluid", style := "padding-bottom:20px",
      div(cls := "row justify-content-md-center",
        div(cls := "row col-lg-11 justify-content-md-center",
          ulMMSelection,
          div(cls := "col-md-7", style := "margin-top:20px;",
            input(cls := "form-control", `id` := s"${_prefix}inputField",
              onkeydown := { (event: dom.KeyboardEvent) =>
                if (event.keyCode == 13) {
                  event.stopPropagation()
                  val symptom = dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[HTMLInputElement].value
                  val abbrev = _selectedMateriaMedicaAbbrev.now.getOrElse("")
                  val remedyName = dom.document.getElementById(s"${_prefix}_inputRemedy") match {
                    case null => None
                    case remedyDataList => Some(remedyDataList.asInstanceOf[dom.html.Input].value.trim)
                  }
                  doLookup(abbrev, symptom, None, remedyName)
                }
              },
              `placeholder` := "Enter some search terms (for example: menses, night)"
            ),
            // It will be (de-) populated on demand
            div(cls:="container-fluid", id:=s"${_prefix}_advancedSearchControlsDiv")
          ),
          div(id := s"${_prefix}_mainViewSearchButtons", cls := "col-md-auto text-center center-block", style := "margin-top:20px;",
            button(cls := "btn btn-primary text-nowrap", style := "width: 80px; margin-right:5px;", `type` := "button",
              onclick := { (event: Event) =>
                event.stopPropagation()
                val symptom = dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[HTMLInputElement].value
                val abbrev = _selectedMateriaMedicaAbbrev.now.getOrElse("")
                val remedyName = dom.document.getElementById(s"${_prefix}_inputRemedy") match {
                  case null => None
                  case remedyDataList => Some(remedyDataList.asInstanceOf[dom.html.Input].value.trim)
                }
                doLookup(abbrev, symptom, None, remedyName)
              },
              span(cls := "oi oi-magnifying-glass", title := "Find", aria.hidden := "true")),
            button(`id`:=s"${_prefix}_buttonMainViewAdvancedSearch", cls:="btn btn-secondary text-nowrap", style:="width: 70px;margin-right:5px;", `type`:="button",
              onclick:={ (event: Event) =>
                event.stopPropagation()
                onShowAdvancedSearchOptions()
              },
              span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true")),
            button(cls := "btn btn-secondary text-nowrap", style := "width: 70px;", `type` := "button",
              onclick := { (event: Event) =>
                event.stopPropagation()

                (_latestSymptomString.now, _latestRemedyString.now) match {
                  case (Some(symptomString), Some(remedyName)) if (symptomString.trim.length > 0 && remedyName.trim.length > 0) =>
                    onShowAdvancedSearchOptions()
                    dom.document.getElementById(s"${_prefix}_inputRemedy").asInstanceOf[dom.html.Input].value = remedyName
                    dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[dom.html.Input].value = symptomString
                    dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[dom.html.Input].focus()
                  case (Some(symptomString), _) if (symptomString.trim.length > 0) =>
                    dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[dom.html.Input].value = symptomString
                    dom.document.getElementById(s"${_prefix}inputField").asInstanceOf[dom.html.Input].focus()
                  case (_, Some(remedyName)) if (remedyName.trim.length > 0) =>
                    onShowAdvancedSearchOptions()
                    dom.document.getElementById(s"${_prefix}_inputRemedy").asInstanceOf[dom.html.Input].value = remedyName
                    dom.document.getElementById(s"${_prefix}_inputRemedy").asInstanceOf[dom.html.Input].focus()
                  case _ => ; // This case should never fire
                }

              },
              span(cls := "oi oi-action-redo", title := "Find again", aria.hidden := "true"))
          )
        )
      ),
      div(id := s"${_prefix}_result_div", cls := "container-fluid", style := "margin-top:23px;",
        div(getResultsHtml(),
          div(cls:= "container-fluid", id:=s"${_prefix}_paginationDiv",
            getPaginatorHtml() match {
              case None => div()
              case Some(resultDiv) => resultDiv
            }
          )
        )
      )
    )
  }

  private def doLookup(abbrev: String, symptom: String, page: Option[Int], remedyStringOpt: Option[String]): Unit = {

    dom.document.body.classList.add("wait")

    def updateDataAndView(searchResults: MMAllSearchResults) = {
      MainView.resetContentView()
      MainView.tabToFront(this)

      _totalNumberOfResultRemedies = searchResults.numberOfMatchingSectionsPerChapter.length
      _page = page
      _sectionHits() = List()
      _sectionHits() = searchResults.results

      refreshMMDropDownButtonLabel()

      if (_advancedSearchOptionsVisible)
        onShowAdvancedSearchOptions()

      dom.document.body.classList.remove("wait")

      // Jump to bottom nav bar, if there is one
      if (page != None && page.get > 0) {
        dom.document.getElementById(s"${_prefix}_paginationDiv") match {
          case null => ;
          case navDiv => navDiv.scrollIntoView(true)
        }
      }
    }

    def showErrorMessage(errorMessage: Option[String]): Unit = {
      var errorMessageTxt = ""

      dom.document.body.classList.remove("wait")

      // If there isn't a specific error message, we assume this function was called because a search failed.
      // The search can fail for 4 different reasons:
      if (errorMessage == None) {
        (symptom, remedyStringOpt) match {
          case (symptomString, Some(remedyName)) if (symptomString.trim.length > 0 && remedyName.trim.length > 0) =>
            errorMessageTxt = s"No results returned for '${symptomString}' and remedy '${remedyName}'."
          case (symptomString, _) if (symptomString.trim.length > 0) =>
            errorMessageTxt = s"No results returned for '${symptomString}'."
          case (_, Some(remedyName)) if (remedyName.trim.length > 0) =>
            errorMessageTxt = s"Remedy abbreviation or fullname '${remedyName}' does not exist in this materia medica."
          case _ =>
            errorMessageTxt = s"Something went wrong. Please, try again."
        }
      }
      // Show custom error message...
      else
        errorMessageTxt = errorMessage.get

      val errorMessageDiv = div(cls := "alert alert-danger", role := "alert", b(errorMessageTxt))

      dom.document.getElementById(s"${_prefix}_resultStatusAlerts") match {
        case null => ;
        case resultDiv =>
          resultDiv.innerHTML = ""
          resultDiv.appendChild(errorMessageDiv.render)
      }
    }

    if (symptom.trim.replaceAll("[^A-Za-z0-9äüöÄÜÖß]", "").length == 0 && remedyStringOpt.getOrElse("").trim.length == 0) {
      showErrorMessage(Some("No results. Enter some symptoms and/or a remedy"))
      return
    }

    if (symptom.trim.replaceAll("[^A-Za-z0-9äüöÄÜÖß]", "").length <= 3 && remedyStringOpt.getOrElse("").trim.length == 0) {
      showErrorMessage(Some("No results returned for '" + symptom.trim + "'. " +
            "Make sure the symptom input is not too short or to enter a remedy."))
      return
    }

    if (symptom.length() >= maxLengthOfSymptoms) {
      showErrorMessage(Some(s"Input must not exceed ${maxLengthOfSymptoms} characters in length."))
      return
    }

    _pageCache.getPage(abbrev, symptom, page.getOrElse(0), remedyStringOpt) match {
      case Some(cachePage) => {
        _latestSymptomString() = Some(symptom)
        _latestRemedyString() = remedyStringOpt
        _selectedMateriaMedicaAbbrev() = Some(abbrev)
        updateDataAndView(cachePage.content)
      }
      case None => {
        HttpRequest(s"${serverUrl()}/${apiPrefix()}/lookup_mm")
          .withQueryParameters(
            ("mmAbbrev", abbrev),
            ("symptom", symptom),
            ("page", page.getOrElse(0).toString),
            ("remedyString", remedyStringOpt.getOrElse(""))
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
                  cursor.as[MMAllSearchResults] match {
                    case Right(MMAllSearchResults(results, numberOfMatchingSectionsPerChapter)) =>
                      if (results.length > 0) {
                        _latestSymptomString() = Some(symptom)
                        _latestRemedyString() = remedyStringOpt

                        _pageCache.addPage(CachePageMM(abbrev, symptom, remedyStringOpt, page.getOrElse(0), MMAllSearchResults(results, numberOfMatchingSectionsPerChapter)))
                        _selectedMateriaMedicaAbbrev() = Some(abbrev)
                        updateDataAndView(MMAllSearchResults(results, numberOfMatchingSectionsPerChapter))
                      }
                      else
                        showErrorMessage(None)
                    case Left(error) =>
                      println(error) // Malformed JSON - shouldn't happen at all
                  }
                }
                case Left(_) => {
                  showErrorMessage(None) // No results returned, most likely from entering incorrect remedy name
                }
              }
            }
            // TODO: Backend did not send success response. Do we need to handle this at all?
            case _ => println("TODO")
          })
      }
    }
  }

  override def onResultsDrawn() = {
    if (_advancedSearchOptionsVisible)
      onShowAdvancedSearchOptions()
  }

  override def containesAnyResults() = {
    _sectionHits.now.length > 0 || _latestRemedyString.now != None
  }

  override def containsUnsavedResults() = { false }

  override def updateDataStructures(remedies: List[Remedy]): Unit = {
    updateAvailableMMsAndRemedies(remedies)
  }
}
