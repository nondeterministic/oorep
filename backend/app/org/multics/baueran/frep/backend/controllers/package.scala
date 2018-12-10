package org.multics.baueran.frep.backend

import scala.collection.mutable.ListBuffer
import javax.inject._
import play.api.mvc._
import io.circe.syntax._

import org.multics.baueran.frep.backend.repertory._
import org.multics.baueran.frep.shared._

import org.multics.baueran.frep.backend.models.Users
import org.multics.baueran.frep.backend.dao.UsersDao
import org.multics.baueran.frep.backend.db.db.DBContext

package object controllers {

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

}
