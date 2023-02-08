package org.multics.baueran.frep.frontend.public.base

import scala.scalajs.js
import js.annotation.JSGlobal

// A mini-facade around the AOS library

@js.native
@JSGlobal
object AOS extends js.Object

@js.native
@JSGlobal("AOS.init")
class AOSInit extends js.Object

object AOSInit {
  def apply() = {
    new AOSInit()
  }
}
