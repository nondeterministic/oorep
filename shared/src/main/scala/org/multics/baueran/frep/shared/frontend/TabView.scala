package org.multics.baueran.frep.shared.frontend

import org.multics.baueran.frep.shared.Remedy
import org.scalajs.dom
import dom.html
import scalatags.JsDom
import JsDom.all._

trait TabView {

  def toFront(): Unit
  def toBack(): Unit
  def tabPaneId(): String
  def tabLinkId(): String
  def tabLink(): JsDom.TypedTag[html.Anchor]
  def drawWithoutResults(): JsDom.TypedTag[dom.html.Div]
  def drawWithResults(): JsDom.TypedTag[dom.html.Div]
  def onResultsDrawn(): Unit // This is an event handler which fires after tab views' results were rendered; e.g., to maniupulate the DOM afterwards
  def containsAnyResults(): Boolean
  def containsUnsavedResults(): Boolean
  def updateDataStructures(remedy: List[Remedy]): Unit
  def getPrefix(): String

  // The drop down menu containing all the remedies for a repertory or materia medica...
  def refreshRemedyDataList(remedies: List[Remedy]): Unit = {
    dom.document.getElementById(s"${getPrefix()}_remedyDataList") match {
      case null => return
      case dataList =>
        while (dataList.asInstanceOf[dom.html.DataList].childNodes.length > 0)
          dataList.removeChild(dataList.firstChild)

        for (remedy <- remedies.sortBy(_.nameAbbrev)) {
          var remedyStringRepr = remedy.nameLong + " (" + remedy.nameAbbrev + ")"
          if (remedy.namealt.length > 0)
            remedyStringRepr += " [" + remedy.namealt.mkString(", ") + "]"
          dataList.appendChild(option(value:=s"${remedyStringRepr}").render)
        }
    }
  }

}
