package org.multics.baueran.frep.backend

import play.api.mvc._
import org.multics.baueran.frep.backend.dao.{CazeDao, FileDao, MemberDao}
import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.Member

// Headers relevant for encryption
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import java.security.SecureRandom

package object controllers {

  var memberDao: MemberDao = _
  var cazeDao: CazeDao = _
  var fileDao: FileDao = _
  private val Logger = play.api.Logger(this.getClass)

  /**
    * Returns empty list if request does not contain valid cookies that are useful for authentication
    * and authorization. Otherwise returns list of valid cookies that were contained in request.
    */

  def getRequestCookies(request: Request[AnyContent]): List[Cookie] = {
    (request.cookies.get(CookieFields.creationDate.toString), request.cookies.get(CookieFields.salt.toString), request.cookies.get(CookieFields.id.toString), request.cookies.get(CookieFields.email.toString)) match {
      case (Some(cookie_date), Some(cookie_hash), Some(cookie_id), Some(cookie_email)) =>
        List(cookie_date, cookie_hash, cookie_id, cookie_email)
      case _ =>
        List.empty
    }
  }

  // https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
  def hashWithSalt(str: String, salt: Array[Byte]) = {
    val iterations = 1000
    val spec = new PBEKeySpec(str.toCharArray(), salt, iterations, 64 * 8)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val hash = skf.generateSecret(spec).getEncoded()

    s"${iterations}:${java.util.Base64.getEncoder.encodeToString(salt)}:${java.util.Base64.getEncoder.encodeToString(hash)}"
  }

  /**
    * Generate a hash that contains the hashed password string.  In particular,
    * the returned string will look like this "xxx:yyyy:zzzz", where xxx is the numer
    * of iterations, yyyy the randomly generated salt, and zzzz the hashed password
    * with respect to the salt.  All three values contained in the string are in fact
    * Base64-encoded byte arrays.
    */

  def getRandomHash(enteredPassword: String) = {

    // https://tersesystems.com/blog/2015/12/17/the-right-way-to-use-securerandom/
    def genSalt(): Array[Byte] = {
      val nativeRandom = SecureRandom.getInstance("NativePRNGNonBlocking") // assuming Unix
      val seed = nativeRandom.generateSeed(55) // NIST SP800-90A suggests 440 bits for SHA1 seed
      val sha1Random = SecureRandom.getInstance("SHA1PRNG")
      sha1Random.setSeed(seed)
      val values = new Array[Byte](20)
      sha1Random.nextBytes(values) // SHA1PRNG, seeded properly
      values
    }

    hashWithSalt(enteredPassword, genSalt())
  }

  /**
    * Returns ID for a registered user if password and email are in database and
    * match the security check.  Returns -1 in case of error.
    */

  def getRegisteredMemberForPasswordAndEmail(enteredPassword: String, enteredEmail: String): Option[Member] = {
    var loggedInMember: Option[Member] = None

    memberDao.getFromEmail(enteredEmail) match {
      case List(memberInDatabase) =>
        memberInDatabase.hash.split(':') match {
          case Array(_, salt, _) =>
            val generatedHash = hashWithSalt(enteredPassword, java.util.Base64.getDecoder.decode(salt))
            if (memberInDatabase.hash == generatedHash)
              loggedInMember = Some(memberInDatabase)
          case _ =>
            Logger.error("isPasswordHashOK failed: decoding of database information failed.")
        }
      case _ =>
        Logger.error("isPasswordHashOK failed: database lookup of member data failed.")
    }

    loggedInMember
  }

  /**
    * Extract a specific data item or value, called attribute, from a list of cookies, called cookies.
    */

  def getCookieValue(cookies: List[Cookie], attribute: String) = {
    val resultCookies = cookies.filter(_.name == attribute)
    if (resultCookies.length > 0) {
      Some(resultCookies.head.value)
    }
    else
      None
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
  def isUserAuthenticated(request: Request[AnyContent]): Either[String, Boolean] = {
    val errorMsg = "Not authenticated."
    getRequestCookies(request) match {
      case Nil => Left(errorMsg)
      case cookies =>
        (getCookieValue(cookies, CookieFields.creationDate.toString), getCookieValue(cookies, CookieFields.id.toString), getCookieValue(cookies, CookieFields.salt.toString)) match {
          case (creationDate, Some(memberIdStr), Some(cookieSalt)) if (memberIdStr.forall(_.isDigit)) =>
            val cookieMemberId = memberIdStr.toInt
            memberDao.get(cookieMemberId) match {
              case List(storedMember) =>
                storedMember.hash.split(":") match {
                  case Array(_, storedSalt, _) =>
                    if (storedSalt == cookieSalt && storedMember.member_id == cookieMemberId && creationDate == storedMember.lastseen)
                      Right(true)
                    else
                      Left(errorMsg)
                  case _ => Left(errorMsg)
                }
              case _ => Left(errorMsg)
            }
          case _ => Left(errorMsg)
        }
    }
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
  def isUserAuthorized(request: Request[AnyContent], tries_to_access_data_of_member_id: Int): Either[String, Boolean] = {
    getRequestCookies(request) match {
      case Nil => Left(s"Not authorised to access ${request.path}")
      case cookies =>
        (getCookieValue(cookies, CookieFields.creationDate.toString), getCookieValue(cookies, CookieFields.id.toString), getCookieValue(cookies, CookieFields.email.toString)) match {
          case (creationDate, Some(memberIdStr), Some(memberEmail)) if (memberIdStr.forall(_.isDigit)) =>
            val cookieMemberId = memberIdStr.toInt
            (memberDao.get(cookieMemberId), memberDao.getFromEmail(memberEmail)) match {
              case (foundMemberOne :: Nil, foundMemberTwo :: Nil) if (foundMemberOne == foundMemberTwo) =>
                if (tries_to_access_data_of_member_id == foundMemberOne.member_id  &&  creationDate == foundMemberOne.lastseen)
                  Right(true)
                else
                  Left(s"Member with ID ${cookieMemberId} not authorised to access data of member with ID ${tries_to_access_data_of_member_id} in ${request.path}.")
              case _ =>
                Left(s"Member with ID ${cookieMemberId} not authorised to access data of member with ID ${tries_to_access_data_of_member_id} in ${request.path}.")
            }
          case _ => Left(s"Not authorised to access ${request.path}")
        }
    }
  }
}
