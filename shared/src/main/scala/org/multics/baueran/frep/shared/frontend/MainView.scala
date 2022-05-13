package org.multics.baueran.frep.shared.frontend

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.Remedy
import org.multics.baueran.frep.shared.sec_frontend.FileModalCallbacks.updateMemberFiles
import scalatags.JsDom.all._
import org.scalajs.dom
import scalatags.JsDom
import monix.execution.Scheduler.Implicits.global
import org.scalajs.dom.raw.BeforeUnloadEvent

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Success

@JSExportTopLevel("MainView")
object MainView {

  object CaseDiv {
    private def caseDivId = "theCaseDiv"
    def apply() = div(cls:="span12", id:=caseDivId)
    def empty() = dom.document.getElementById(caseDivId).innerHTML = ""
    def append(element: dom.html.Element) = dom.document.getElementById(caseDivId).appendChild(element)
  }

  // Register tab views: order is relevant. First element is first tab, and so on...
  private val _tabViews = List(RepertoryView, MateriaMedicaView)
  private var _remedies: List[Remedy] = List.empty

  private def getAvailableRemedies(): List[Remedy] = {
    if (_remedies.length == 0) {
      HttpRequest(s"${serverUrl()}/${apiPrefix()}/available_remedies")
        .send()
        .onComplete({
          case response: Success[SimpleHttpResponse] => {
            parse(response.get.body) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[List[Remedy]] match {
                  case Right(allRemedies) =>
                    _remedies = allRemedies
                    _tabViews.map(_.updateDataStructures(_remedies))  // Update also the tab views' datastructures!
                  case Left(err) => println("Parsing of remedies failed: " + err)
                }
              }
              case Left(err) => println("Parsing of remedies failed: is it JSON? " + err)
            }
          }
          case _ => println("MainView didn't receive available remedies from backend.")
        })
    }

    _remedies
  }

  def updateDataStructuresFromBackendData() = {
    getAvailableRemedies()
  }

  def resetContentView(): Unit = {
    dom.document.getElementById("content") match {
      case null =>
        println("ERROR: '#content'-div is null. This shoulnd't have happened.")
      case contentDiv => {
        contentDiv.asInstanceOf[dom.html.Element].innerHTML = ""
        contentDiv.asInstanceOf[dom.html.Element].appendChild(withResults().render)

        // This is code, where the various tab views can modify the DOM after its been rendered
        _tabViews.foreach(_.onResultsDrawn())

        if (dom.document.getElementById("nav_bar_logo").innerHTML.length() == 0) {
          val navbar = dom.document.getElementById("nav_bar").asInstanceOf[dom.html.Element]

          navbar.className = navbar.className + " bg-dark navbar-dark shadow"

          dom.document.getElementById("nav_bar_logo")
            .appendChild(a(cls:="navbar-brand py-0", style:="margin-top:8px;", href:=serverUrl(), h5(cls:="freetext", "OOREP")).render)

          // When we come from the main page, we need to delete about and features, but not if we come from /?show=.... which doesn't have these two divs to begin with.
          (dom.document.getElementById("about"), dom.document.getElementById("features")) match {
            case (aboutDiv: dom.Element, featuresDiv: dom.Element) =>
              dom.document.body.removeChild(aboutDiv)
              dom.document.body.removeChild(featuresDiv)
            case _ => ;
          }
        }
      }
    }

    // Make sure #about and #features are REALLY gone at this point
    val about = dom.document.getElementById("about")
    val features = dom.document.getElementById("features")
    if (about != null)
      dom.document.body.removeChild(about)
    if (features != null)
      dom.document.body.removeChild(features)

    getCookieData(dom.document.cookie, CookieFields.id.toString) match {
      case Some(id) => updateMemberFiles(id.toInt)
      case None => ;
    }
  }

  def tabToFront(view: TabView) = {
    _tabViews.map(_.toBack())
    view.toFront()
  }

  def toggleOnBeforeUnload() = {
    if (_tabViews.exists(_.containsUnsavedResults())) {
      if (dom.window.onbeforeunload == null)
        dom.window.onbeforeunload = ((arg: BeforeUnloadEvent) => {
          arg.stopPropagation()
          arg.returnValue
        })
    } else {
      dom.window.onbeforeunload = null
    }
  }

  private def withResults(): JsDom.TypedTag[dom.html.Div] = {
    toggleOnBeforeUnload()

    val divTabs = {

      val divContents = {
        div(cls:="tab-content border-right border-left border-bottom", id:="divTabs_content", style:="padding-top:15px;",
          div(cls:="tab-pane fade show active", id:=_tabViews.head.tabPaneId(), role:="tabpanel", aria.labelledby:="ex1-tab-1",
            _tabViews.head.drawWithResults().render
          ),
          _tabViews.tail.map(tabView =>
            div(cls:="tab-pane fade", id:=tabView.tabPaneId(), role:="tabpanel", aria.labelledby:="ex1-tab-1",
              tabView.drawWithResults().render
            )
          )
        )
      }

      div(cls:="container-fluid", style:="margin-top:30px; margin-bottom:30px;",
        ul(cls:="nav nav-tabs",
          _tabViews.map(tabView =>
            li(cls:="nav-item",
              tabView.tabLink().render
            )
          )
        ),
        divContents
      )
    }

    div(
      divTabs,
      CaseDiv()
    )
  }

  private def withoutResults(): JsDom.TypedTag[dom.html.Div] = {
    val divTabs = {

      val divContents = {
        div(cls:="tab-content border-right border-left border-bottom", id:="divTabs_content", style:="padding:20px;",
          div(cls:="tab-pane fade show active", id:=_tabViews.head.tabPaneId(), role:="tabpanel", aria.labelledby:="ex1-tab-1",
            _tabViews.head.drawWithoutResults().render
          ),
          _tabViews.tail.map(tabView =>
            div(cls:="tab-pane fade", id:=tabView.tabPaneId(), role:="tabpanel", aria.labelledby:="ex1-tab-1",
              tabView.drawWithoutResults().render
            ),
          )
        )
      }

      div(cls:="introduction",

        div(cls:="container-fluid vertical-align",

          h1(cls:="col-sm-12 text-center", img(src:=s"${serverUrl()}/assets/html/img/logo_small.png", style:="width:180px; height:65px;", alt:="OOREP - open online repertory of homeopathy")),

          div(cls:="container text-center",
            ul(cls:="nav nav-tabs",
              _tabViews.map(tabView =>
                li(cls:="nav-item",
                  tabView.tabLink().render
                )
              )
            ),
            divContents
          )
        )

      )
    }

    div(
      divTabs,
      CaseDiv()
    )
  }

  def someResultsHaveBeenShown() = {
    _tabViews.exists(_.containesAnyResults())
  }

  def apply(): JsDom.TypedTag[dom.html.Div] = {
    withoutResults()
  }

}
