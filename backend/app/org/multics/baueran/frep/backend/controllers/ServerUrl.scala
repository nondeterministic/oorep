package org.multics.baueran.frep.backend.controllers

import play.api.mvc._

trait ServerUrl {

  def serverUrl(req: Request[AnyContent]) = {
    (sys.env.get("OOREP_APP_PROTOCOL"), sys.env.get("OOREP_APP_HOSTNAME"), sys.env.get("OOREP_APP_DOMAIN"), sys.env.get("OOREP_APP_PORT")) match {
      case (Some(protocol), Some(hostname), Some(domain), Some(port)) =>
        protocol + "://" + hostname + "." + domain + ":" + port
      case (Some(protocol), Some(hostname), Some(domain), None) =>
        protocol + "://" + hostname + "." + domain
      case (Some(protocol), Some(hostname), None, Some(port)) =>
        protocol + "://" + hostname + ":" + port
      case (Some(protocol), Some(hostname), None, None) =>
        protocol + "://" + hostname
      case _ =>
        val path = req.host
        if (req.connection.secure)
          "https://" + path
        else
          "http://" + path
    }
  }

  def serverUrl() = {
    (sys.env.get("OOREP_APP_PROTOCOL"), sys.env.get("OOREP_APP_HOSTNAME"), sys.env.get("OOREP_APP_DOMAIN"), sys.env.get("OOREP_APP_PORT")) match {
      case (Some(protocol), Some(hostname), Some(domain), Some(port)) =>
        protocol + "://" + hostname + "." + domain + ":" + port
      case (Some(protocol), Some(hostname), Some(domain), None) =>
        protocol + "://" + hostname + "." + domain
      case (Some(protocol), Some(hostname), None, Some(port)) =>
        protocol + "://" + hostname + ":" + port
      case (Some(protocol), Some(hostname), None, None) =>
        protocol + "://" + hostname
      case _ => "https://www.oorep.com/"
    }
  }

  def serverDomain(req: Request[AnyContent]) =
    sys.env.get("OOREP_DOMAIN").getOrElse(req.domain)

  def apiPrefix() = "api"

}
