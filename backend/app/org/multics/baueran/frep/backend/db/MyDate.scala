package org.multics.baueran.frep.backend.db

import java.text.SimpleDateFormat
import java.util.Date

class MyDate(isoDateString: String) {

  def this() {
    this(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()))
  }

  def this(javaDate: Date) {
    this(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(javaDate))
  }

  override def toString() = isoDateString
}
