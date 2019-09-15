package org.multics.baueran.frep.backend.controllers

import play.api.mvc._

trait ServerUrl {

  def serverUrl(req: Request[AnyContent]) = {
    val path = req.host
    if (req.connection.secure)
      "https://" + path
    else
      "http://" + path
  }

}
