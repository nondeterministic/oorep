package org.multics.baueran.frep.frontend.public.base

import org.multics.baueran.frep.shared.frontend.Repertorise
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw._
import org.scalajs.dom.document
import scalatags.JsDom.all._
import rx._
import rx.{Rx, Var, _}
import scalatags.rx.all._
import rx.Ctx.Owner.Unsafe._

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation._
import org.querki.jquery._
import scalatags.JsDom.all._

@JSExportTopLevel("Main")
object Main {
  val hspace = div(cls:="col-xs-12", style:="height:50px;")

  def main(args: Array[String]): Unit = {

// Alternatively to JQuery selector:
//
//    import org.scalajs.dom.document
//    document.body.appendChild(
//      div(style:="width:100%; margin-bottom:100px;",
//        scalatags.JsDom.attrs.id:="nav_bar").render)

    $("#nav_bar").empty()
    $("#nav_bar").append(NavBar.apply().render)
    $("#content").append(Repertorise.apply().render)
    $("#content_bottom").append(Disclaimer.toHTML().render)
  }
}
