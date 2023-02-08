package org.multics.baueran.frep.shared.frontend

import scalatags.JsDom.all.{id, _}
import org.scalajs.dom
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

class Notify(alertId: String, message: String) {

  // cf. https://stackoverflow.com/questions/46617946/sleep-inside-future-in-scala-js
  private def delay(milliseconds: Int): Future[Unit] = {
    val p = Promise[Unit]()
    js.timers.setTimeout(milliseconds) {
      p.success(())
    }
    p.future
  }

  private val readyLater = for {
    delayed <- delay(4000)
  } yield {
    dom.document.getElementById(alertId) match {
      case null => ; // do nothing; alert has most likely already been closed by the user
      case alert => dom.document.body.removeChild(alert)
    }
  }

  dom.document.body.appendChild(
    div(`id`:=alertId, style:="position:fixed; top:0; right:0; opacity:1; z-index: 2000;", cls:="alert alert-danger oorep-notify-alert", role:="alert",
      button(`type`:="button", cls:="close", style:="margin-left:8px;", data.dismiss:="alert", span(aria.hidden:="true", raw("&times;"))),
      b(s"${message}")
    ).render
  )

}

object Notify {
  def noAlertsVisible() = {
    dom.document.body.getElementsByClassName("oorep-notify-alert").length == 0
  }
}
