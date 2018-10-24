package org.multics.baueran.frep.frontend.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav

object NavBar {
  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    val myHTML = nav(cls:="navbar navbar-expand-sm bg-dark navbar-dark",
      ul(cls:="navbar-nav",
          li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Link1")),
          li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("Link2"))
          ))
    myHTML
  }
}