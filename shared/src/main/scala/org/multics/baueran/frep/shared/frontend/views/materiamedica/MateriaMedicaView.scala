package org.multics.baueran.frep.shared.frontend.views.materiamedica

import scalatags.JsDom
import JsDom.all._
import io.circe.parser.parse
import org.scalajs.dom
import dom.html.{Div, Element}
import dom.Event
import org.scalajs.dom.{Event, html}

import scala.scalajs.js.URIUtils.encodeURI
import org.multics.baueran.frep.shared.{HitsPerRemedy, HttpRequest2, MMAllSearchResults, MMAndRemedyIds, MMSearchResult, MateriaMedicas, Paginator, Remedies, Remedy}
import org.multics.baueran.frep.shared.Defs.{HeaderFields, ResourceAccessLvl, maxLengthOfSymptoms, maxNumberOfResultsPerMMPage}
import org.multics.baueran.frep.shared.TopLevelUtilCode.getDocumentCsrfCookie
import org.multics.baueran.frep.shared.frontend.views.materiamedica.uielements._
import org.multics.baueran.frep.shared.frontend._

object MateriaMedicaView extends TabView {

  private val _prefix = "MMView"
  private var _currResultShareLink = s"${serverUrl()}"
  private var _defaultMMAbbrev: Option[String] = None
  private var _remedies: Remedies = new Remedies(List.empty)
  private var _materiaMedicas: MateriaMedicas = new MateriaMedicas(List())
  private var _collapsedMultiOccurrencesSpans = true

  private var _sectionHits: List[MMSearchResult] = List.empty
  private val _allSectionsHide = com.raquo.laminar.api.L.Var(true)

  private var _totalNumberOfResultRemedies = 0
  private var _page: Option[Int] = None
  private val _pageCache = new PageCacheMM()

  private[materiamedica] def defaultMMAbbrev(): Option[String] = _defaultMMAbbrev
  private[materiamedica] def materiaMedicas(): MateriaMedicas = _materiaMedicas

  private def resultsLink(): String = encodeURI(_currResultShareLink + s"&hideSections=${_allSectionsHide.now()}")

  private[materiamedica] def refreshRemedyDataList(): Unit = {
    val selectedAbbrev = _selectedMateriaMedicaAbbrev.getOrElse("")

    if (selectedAbbrev.length == 0)
      return

    _materiaMedicas.get(selectedAbbrev) match {
      case Some((info, remedies)) => refreshRemedyDataList(remedies)
      case None => return
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
        val multiOccurrencesSpan = span(id := s"${_prefix}_multiOccurrencesContentSpan", cls := s"${if (_collapsedMultiOccurrencesSpans) "hide" else "show"} collapse").render

        dom.document.getElementById(s"${_prefix}_multiOccurrencesDiv") match {
          case null => ;
          case multiOccDiv =>
            multiOccDiv.asInstanceOf[dom.html.Div].appendChild(multiOccurrencesSpan)
        }

        _pageCache.latest() match {
          case Some(latestCachePage) if latestCachePage.content.numberOfMatchingSectionsPerChapter.length > 1 => {
            val multiOccurrencesLinks = latestCachePage.content.numberOfMatchingSectionsPerChapter
              .sortWith((h1, h2) => _remedies.get(h1.remedyId) > _remedies.get(h2.remedyId)) // Uses the above implicit!
              .sortBy(-_.hits)
              .map {
                case HitsPerRemedy(numberOfHits, remedyId) => {
                  val remedyAbbrev = _remedies.get(remedyId) match {
                    case Some(remedy) => Some(remedy.nameAbbrev)
                    case None => None
                  }
                  span(numberOfHits.toString + "x",
                    a(href := "#", onclick := { (_: Event) => doLookup(latestCachePage.abbrev, "", None, remedyAbbrev) },
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
          case _ =>
            multiOccurrencesSpan.appendChild(span(" (No further matching chapters.)").render)
        }
      }
      // If multi-occurrences already there, even if collapse, do nothing!
      case multiOccurrencesContentSpan => ;
    }
  }

  private[materiamedica] def getPaginatorHtml(): Option[scalatags.JsDom.TypedTag[html.Element]] = {
    if (_totalNumberOfResultRemedies <= maxNumberOfResultsPerMMPage)
      return None

    val totalNumberOfPages = math.ceil(_totalNumberOfResultRemedies.toDouble / maxNumberOfResultsPerMMPage.toDouble).toInt
    val pg = new Paginator(totalNumberOfPages, _page.getOrElse(0), 5).getPagination()
    val htmlPg = new PaginatorHtml(s"${_prefix}_paginationDiv", pg)

    _pageCache.latest() match {
      case Some(latestPageCache) =>
        Some(htmlPg.toHtml(latestPageCache.abbrev, latestPageCache.symptom, latestPageCache.remedy, doLookup))
      case _ =>
        None
    }
  }

  private[materiamedica] def getResultsHtml(): JsDom.TypedTag[Div] = {
    if (_sectionHits.length > 0) {

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
        _allSectionsHide.set(true)
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
        _allSectionsHide.set(false)
      }

      val shareResultsModal = new ShareResultsModal(_prefix, resultsLink)

      // Here starts the actual HTML which is returned by this function...
      div(

        shareResultsModal(),

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
                      _collapsedMultiOccurrencesSpans = false
                    }
                    else {
                      toggleSpan.setAttribute("class", "oi oi-chevron-left")
                      toggleSpan.setAttribute("title", "Show more...")
                      _collapsedMultiOccurrencesSpans = true
                    }
                  }
                }
              },
              if (_collapsedMultiOccurrencesSpans)
                span(aria.hidden := "true", id := s"${_prefix}_collapseMultiOccurrencesButton", cls := "oi oi-chevron-left", style := "font-size: 14px;", title := "Show more...")
              else
                span(aria.hidden := "true", id := s"${_prefix}_collapseMultiOccurrencesButton", cls := "oi oi-chevron-bottom", style := "font-size: 14px;", title := "Show less")
            ),
            {
              var searchResultsTxt = ""

              (_latestSymptomString, _latestRemedyString) match {
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

              span(
                b(searchResultsTxt),
                span(raw("&nbsp;&nbsp;")),
                button(`type`:="button", cls:="btn btn-sm px-1 py-0 btn-outline-primary", data.toggle:="modal", data.dismiss:="modal", data.target:=s"#${_prefix}shareResultsModal",
                  onclick:={ (_: Event) => shareResultsModal.updateResultsLink(resultsLink()) },
                  span(cls:="oi oi-link-intact", style:="font-size: 12px;", title:="Share link...", aria.hidden:="true")),
                span(raw("&nbsp;&nbsp;&nbsp;")),
                button(`type`:="button", cls:="btn btn-sm px-1 py-0 btn-outline-primary",
                  onclick:={ (_: Event) => dom.window.open(resultsLink()) },
                  span(cls:="oi oi-external-link", style:="font-size: 12px;", title:="Open in new window...", aria.hidden:="true")),
                span(raw("&nbsp;&nbsp;"))
              )
            }
          ),

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

        _sectionHits.map(_.render(_prefix, _allSectionsHide, _latestSymptomString.getOrElse(""), _materiaMedicas, doLookup))

      )
    }
    else if (_latestSymptomString.getOrElse("").trim.length > 0 || _latestRemedyString.getOrElse("").trim.length > 0) {
      var errorMessageTxt = ""

      (_latestSymptomString, _latestRemedyString) match {
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
    dom.document.getElementById(tabLinkId()) match {
      case null =>
        println("ERROR: MateriaMedicaView.toFront() failed.")
      case element =>
        element.classList.add("show")
        element.classList.add("active")
    }

    dom.document.getElementById(tabPaneId()) match {
      case null =>
        println("ERROR: MateriaMedicaView.toFront() failed.")
      case element =>
        element.classList.add("show")
        element.classList.add("active")
    }
  }

  override def toBack(): Unit = {
    dom.document.getElementById(tabLinkId()) match {
      case null =>
        println("ERROR: MateriaMedicaView.toBack() failed.")
      case element =>
        element.classList.remove("show")
        element.classList.remove("active")
    }

    dom.document.getElementById(tabPaneId()) match {
      case null =>
        println("ERROR: MateriaMedicaView.toBack() failed.")
      case element =>
        element.classList.remove("show")
        element.classList.remove("active")
    }
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
        MMSelectionDropdown.refresh()
        renderMultiOccurrencesDiv()
      },
      "Materia Medica")
  }

  override def drawWithoutResults() = WithoutResults()

  override def drawWithResults() = WithResults()

  def jsDoLookup(abbrev: String, symptom: String, page: Int, hideSections: Boolean, remedyString: String): Unit = {
    // Hide navbar initially, while the spinner shows. (Later, the code in this file will show it again.)
    dom.document.getElementById("nav_bar").asInstanceOf[dom.html.Div].classList.add("d-none")

    _selectedMateriaMedicaAbbrev = Some(abbrev)
    _allSectionsHide.set(hideSections)

    doLookup(abbrev, symptom,
      if (page > 0) Some(page) else None,
      if (remedyString.length > 0) Some(remedyString) else None
    )
  }

  private[materiamedica] def doLookup(abbrev: String, symptom: String, page: Option[Int], remedyStringOpt: Option[String]): Unit = {
    dom.document.body.classList.add("wait")

    def updateDataAndView(searchResults: MMAllSearchResults): Unit = {
      MainView.resetContentView()
      MainView.tabToFront(this)

      _totalNumberOfResultRemedies = searchResults.numberOfMatchingSectionsPerChapter.length
      _page = page
      _sectionHits = searchResults.results

      dom.document.getElementById(s"${_prefix}_result_div") match {
        case null =>
          println("ERROR: updateDataAndView() cannot access results-div")
        case element =>
          element.innerHTML = ""
          element.appendChild(getResultsHtml().render)
          element.appendChild(div(cls := "container-fluid", id := s"${_prefix}_paginationDiv").render)
      }

      renderMultiOccurrencesDiv()

      MMSelectionDropdown.refresh()

      getPaginatorHtml() match {
        case None => ;
        case Some(resultDiv) => dom.document.getElementById(s"${_prefix}_paginationDiv").appendChild(resultDiv.render)
      }

      if (advancedSearchOptionsVisible())
        AdvancedSearchOptionsButton.onShowAdvancedSearchOptions()

      dom.document.body.classList.remove("wait")

      // When we do a /?show=... lookup, we need to make sure the disclaimer is made visible again. For other cases, it doesn't matter, because
      // the disclaimer is already visible at this point.
      dom.document.getElementById("disclaimer_div").asInstanceOf[dom.html.Div].style.setProperty("display", "block")

      // Jump to bottom nav bar, if there is one
      if (page != None && page.get > 0) {
        dom.document.getElementById(s"${_prefix}_paginationDiv") match {
          case null => ;
          case navDiv => navDiv.scrollIntoView(true)
        }
      }

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

      _currResultShareLink =
        s"${serverUrl()}/show_mm?materiaMedica=${abbrev}&symptom=${symptom}&page=${_page.getOrElse(1).toString}&remedyString=${remedyStringOpt.getOrElse("")}"
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
        // The null case fires, when we use /show_mm... link, because then the loading screen is empty and doesn't have the HTML element in question...
        case null =>
          val errorMessage = s"ERROR: Lookup failed. Perhaps URL malformed, materia medica does not exist or no results for given symptoms. " +
            s"SUGGESTED SOLUTION: Go directly to ${serverUrl()} instead and try again!"
          dom.document.location.replace(s"${serverUrl()}/${apiPrefix()}/display_error_page?message=${encodeURI(errorMessage)}")
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
        _latestSymptomString = Some(symptom)
        _latestRemedyString = remedyStringOpt
        _selectedMateriaMedicaAbbrev = Some(abbrev)
        updateDataAndView(cachePage.content)
      }
      case None => {
        HttpRequest2("lookup_mm")
          .withQueryParameters(
            ("mmAbbrev", abbrev),
            ("symptom", symptom),
            ("page", page.getOrElse(0).toString),
            ("remedyString", remedyStringOpt.getOrElse(""))
          )
          .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
          .onSuccess((response: String) =>
            parse(response) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[MMAllSearchResults] match {
                  case Right(MMAllSearchResults(results, numberOfMatchingSectionsPerChapter)) =>
                    if (results.length > 0) {
                      _latestSymptomString = Some(symptom)
                      _latestRemedyString = remedyStringOpt

                      _pageCache.addPage(CachePageMM(abbrev, symptom, remedyStringOpt, page.getOrElse(0), MMAllSearchResults(results, numberOfMatchingSectionsPerChapter)))
                      _selectedMateriaMedicaAbbrev = Some(abbrev)
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
          )
          .send()
      }
    }
  }

  override def onResultsDrawn(): Unit = {
    if (advancedSearchOptionsVisible())
      AdvancedSearchOptionsButton.onShowAdvancedSearchOptions()
  }

  override def containsAnyResults(): Boolean =
    _sectionHits.length > 0 || _latestRemedyString != None

  override def containsUnsavedResults(): Boolean = false

  override def updateDataStructures(remedies: List[Remedy]): Unit = {
    // See also RepertoryView.scala for a similar after-update-handler!
    def runAfterUpdate(): Unit = {
      if (containsAnyResults()) {
        MainView.resetContentView()
        MainView.tabToFront(this)
        MMSelectionDropdown.refresh()
        renderMultiOccurrencesDiv()
      }
    }

    def updateAvailableMMsAndRemedies(remedies: List[Remedy], runAfterUpdate: (() => Unit)): Unit = {
      _remedies = new Remedies(remedies)

      if (_materiaMedicas.size() == 0) {
        HttpRequest2("available_rems_and_mms")
          .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
          .onSuccess((response: String) =>
            parse(response) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[List[MMAndRemedyIds]] match {
                  case Right(mmsAndRemedies) => {
                    // Fill dropdown menu by triggering the corresponding RX...
                    _materiaMedicas =
                      MateriaMedicas(mmsAndRemedies.map {
                        case MMAndRemedyIds(mminfo, remedyIds) => (mminfo, remedyIds.map(_remedies.get(_)).flatten)
                      })
                    MMSelectionDropdown.Menu.refresh()

                    // Determine default materia medica...
                    if (_selectedMateriaMedicaAbbrev == None) {
                      mmsAndRemedies.filter(_.mminfo.access == ResourceAccessLvl.Default.toString) match {
                        case defaultMateriaMedica :: _ =>
                          _selectedMateriaMedicaAbbrev = Some(defaultMateriaMedica.mminfo.abbrev)
                          MMSelectionDropdown.Button.getNode() match {
                            case None => ;
                            case Some(button) => button.textContent = s"M. Medica: ${defaultMateriaMedica.mminfo.abbrev}"
                          }
                          _defaultMMAbbrev = Some(defaultMateriaMedica.mminfo.abbrev)
                        case Nil => ;
                      }
                    }

                    runAfterUpdate()
                  }
                  case Left(error) => println(s"Materia medica decoding failed: $error")
                }
              }
              case Left(error) => println(s"Materia medica parsing failed: $error")
            }
          )
          .send()
      }
      else
        MMSelectionDropdown.refresh()

      renderMultiOccurrencesDiv()
    }

    updateAvailableMMsAndRemedies(remedies, runAfterUpdate)
  }
}
