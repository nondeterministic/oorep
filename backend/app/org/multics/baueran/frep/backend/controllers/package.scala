package org.multics.baueran.frep.backend

import scala.collection.mutable.ListBuffer
import javax.inject._
import play.api.mvc._
import io.circe.syntax._
import org.multics.baueran.frep.backend.dao.{CazeDao, MemberDao}

package object controllers {

  var memberDao: MemberDao = _
  var cazeDao: CazeDao = _

  /**
    * Returns empty list if request does not contain valid cookies for authorization.
    * Otherwise returns list of valid cookies that were contained in request.
    */
  // TODO: Add database lookup for cookie-data validation!
  def authorizedRequestCookies(request: Request[AnyContent]): List[Cookie] = {
    (request.cookies.get("oorep_member_email"), request.cookies.get("oorep_member_password"), request.cookies.get("oorep_member_id")) match {
      case (Some(cookie_email), Some(cookie_password), Some(cookie_id)) =>
        List(cookie_email, cookie_password, cookie_id)
      case _ =>
        List.empty
    }
  }

  /**
    * Extract data from a cookie, e.g. a cookie like this "attribute=data;"
    *
    * @param cookies
    * @param attribute
    * @return
    */
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
