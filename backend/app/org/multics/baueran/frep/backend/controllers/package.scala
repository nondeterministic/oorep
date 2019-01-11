package org.multics.baueran.frep.backend

import scala.collection.mutable.ListBuffer
import javax.inject._
import play.api.mvc._
import io.circe.syntax._

import org.multics.baueran.frep.backend.dao.MemberDao

package object controllers {

  var members: MemberDao = _

  /**
    * Returns empty list if request does not contain valid cookies for authorization.
    * Otherwise returns list of valid cookies that were contained in request.
    */
  // TODO: Add database lookup for cookie-data validation!
  def authorizedRequestCookies(request: Request[AnyContent]): List[Cookie] = {
    (request.cookies.get("oorep_user_email"), request.cookies.get("oorep_user_password")) match {
      case (Some(cookie_email), Some(cookie_password)) =>
        List(cookie_email, cookie_password)
      case _ =>
        List.empty
    }
  }

  def getFrom(cookies: List[Cookie], attribute: String) = {
    val resultCookies = cookies.filter(_.name == attribute)
    if (resultCookies.length > 0) {
      println("INFO: getFrom(Cookies): " + resultCookies.head.value)
      Some(resultCookies.head.value)
    }
    else
      None
  }
}
