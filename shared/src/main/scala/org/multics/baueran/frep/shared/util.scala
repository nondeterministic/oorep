package org.multics.baueran.frep.shared

class BetterString(val s: String) {
  def shorten = if (s.length <= 66) s else s.substring(0,62) + "..."
}


