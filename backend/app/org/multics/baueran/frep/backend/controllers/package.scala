package org.multics.baueran.frep.backend

import play.api.mvc._
import org.multics.baueran.frep.backend.dao.{CazeDao, EmailHistoryDao, FileDao, MMDao, MemberDao, PasswordChangeRequestDao, RepertoryDao}
import org.multics.baueran.frep.shared.Defs.{CookieFields, HeaderFields}
import org.multics.baueran.frep.shared.Member

package object controllers {

  var memberDao: MemberDao = _
  var cazeDao: CazeDao = _
  var fileDao: FileDao = _
  var passwordDao: PasswordChangeRequestDao = _
  var emailHistoryDao: EmailHistoryDao = _
  var repertoryDao: RepertoryDao = _
  var mmDao: MMDao = _

  private val Logger = play.api.Logger(this.getClass)

  def getFromRequestCookie(request: Request[AnyContent], name: String): Option[String] = {
    request.cookies.get(name) match {
      case Some(cookie) => Some(cookie.value)
      case None => None
    }
  }

  /**
    * Check if user has received a cookie after authentication (but do not check authorization here! This is not
    * a login method or the like!  Look inside Post.scala for how login is done, AFTER which a cookie is stored.)
    *
    * This cookie needs to contain a matching member ID and salt (and lately also cookie creation date!), so
    * that a user cannot access protected parts of the site.
    *
    * @param request
    * @return true, if the user who triggered the call of this function has a cookie with the corresponding salt value
    *         as in the DB.  This indicates that the user has prior logged in to the system.
    */

  def getAuthenticatedUser(request: Request[AnyContent]): Option[Member] = {
    request.headers.get("X-Remote-User") match {
      case Some(uidStr) if (uidStr.length > 0 && uidStr.forall(_.isDigit)) =>
        Logger.debug(s"getAuthenticatedUser: SUCCESS: uid: ${uidStr}.")
        return memberDao.get(uidStr.toInt)
      case Some(uidStr) if (uidStr.length == 0) =>
        Logger.debug(s"getAuthenticatedUser: DEBUG: uid: ${uidStr}.")
      case _ =>
        Logger.error(s"getAuthenticatedUser: FAILED: uid: ${request.headers.get("X-Remote-User")}.")
    }

    None
  }

  /**
    * Check if user is authorized to access a resource for a member with ID
    * tries_to_access_data_of_member_id.  Under normal circumstances those IDs should be
    * the same, or we'll assume something fishy is going on (and return false here).
    *
    * @param request
    * @param tries_to_access_data_of_member_id
    * @return true, if user is authorised; false otherwise.
    */

  def isUserAuthorized(request: Request[AnyContent], tries_to_access_data_of_member_id: Int) = {
    getAuthenticatedUser(request) match {
      case Some(member) => member.member_id == tries_to_access_data_of_member_id
      case None => false
    }
  }

}
