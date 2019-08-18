package org.multics.baueran.frep.backend

import play.api.mvc._
import org.multics.baueran.frep.backend.dao.{CazeDao, FileDao, MemberDao}
import org.multics.baueran.frep.shared.Defs.CookieFields

package object controllers {

  var memberDao: MemberDao = _
  var cazeDao: CazeDao = _
  var fileDao: FileDao = _

  /**
    * Returns empty list if request does not contain valid cookies for authorization.
    * Otherwise returns list of valid cookies that were contained in request.
    */
  // TODO: Add database lookup for cookie-data validation!
  def authorizedRequestCookies(request: Request[AnyContent]): List[Cookie] = {
    (request.cookies.get(CookieFields.email.toString),
      request.cookies.get(CookieFields.hash.toString),
      request.cookies.get(CookieFields.id.toString)) match {
      case (Some(cookie_email), Some(cookie_hash), Some(cookie_id)) =>
        List(cookie_email, cookie_hash, cookie_id)
      case _ =>
        List.empty
    }
  }

  def getHash(s: String) = {
    import java.security.MessageDigest
    import java.math.BigInteger
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1,digest)
    val hashedString = bigInt.toString(16)
    hashedString
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
      Some(resultCookies.head.value)
    }
    else
      None
  }

  def doesUserHaveCorrespondingCookie(request: Request[AnyContent], memberId: Int): Either[String, Boolean] = {
    val errorMsg = "Not authorized: bad request"
    authorizedRequestCookies(request) match {
      case Nil => Left(errorMsg)
      case cookies =>
        getFrom(cookies, CookieFields.hash.toString) match {
          case None => Left(errorMsg)
          case Some(hash) =>
            memberDao.get(memberId) match {
              case member :: Nil => {
                if (member.hash == hash)
                  Right(true)
                else
                  Left(errorMsg)
              }
              case _ =>
                Left(errorMsg)
            }
        }
    }
  }

}
