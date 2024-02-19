package org.multics.baueran.frep.backend.controllers

import javax.inject._
import io.circe.syntax._
import play.api.mvc._
import org.multics.baueran.frep.backend.dao.{CazeDao, EmailHistoryDao, FileDao, MMDao, MemberDao, PasswordChangeRequestDao, RepertoryDao}
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.db.db.DBContext
import Defs._
import play.api.libs.json.JsResult.Exception

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

/**
 * This controller creates an `Action` to handle HTTP requests to the application's home page.
 */

@Singleton
class Get @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {
  // TODO: I'm not sure, if DAOs shouldn't be local to the calling methods due to potential locking-waiting issues with many requests. I simply don't know enough how Play controllers handle concurrent access, really...
  // TODO: If there are every problems that smell like that, turn those DAOs local to the method that is using them and remove those global variables here!
  cazeDao = new CazeDao(dbContext)
  fileDao = new FileDao(dbContext)
  memberDao = new MemberDao(dbContext)
  passwordDao = new PasswordChangeRequestDao(dbContext)
  emailHistoryDao = new EmailHistoryDao(dbContext)
  repertoryDao = new RepertoryDao(dbContext)
  mmDao = new MMDao(dbContext)

  private val Logger = play.api.Logger(this.getClass)

  /**
    * Login is handled by an external service provider, such as mod_auth_mellon, and an external
    * identiy provider, such as SimpleSAML, for example. In this scenario, you should run OOREP
    * behind an (Apache) reverse proxy and intercept calls to the location "/login", such that a
    * valid Mellon user is required and requested for this location.
    *
    * OOREP doesn't do any authentication itself, it merely expects the web server (or rather:
    * reverse proxy) to send the variable "X-Remote-User" to the application server, containing
    * the authenticated user's ID (see @getAuthenticatedUser).  So this method here merely
    * redirects to the landing page and won't do anything, unless your web server / reverse proxy
    * handles incoming connections to "/login" separately.
    *
    * see @logout
    */

  def login() = Action { implicit request =>
    getAuthenticatedUser(request) match {
      case None =>
        Logger.error("Get: login() not completed successfully: sending user to logout to be safe.")
        Redirect(sys.env.get("OOREP_URL_LOGOUT").getOrElse(""))
      case Some(member) =>
        memberDao.increaseLoginCounter(member.member_id)
        memberDao.setLastSeen(member.member_id, new MyDate())
        Logger.debug(s"Get: login() completed for user ${member.member_id.toString}.")
        Redirect(serverUrl(request))
          .withCookies(
            Cookie(CookieFields.id.toString, member.member_id.toString, secure = true, httpOnly = false),
            Cookie(CookieFields.cookiePopupAccepted.toString, "1", secure = true, httpOnly = false)
          )
          .withSession("id" -> member.member_id.toString)
    }
  }

  /**
    * Logout is handled by an external service provider, such as mod_auth_mellon, for example.
    * A valid logout URL could be https://www.oorep.com/mellon/logout?ReturnTo=https://www.oorep.com/
    * when /mellon/ is your Mellon-endpoint path. (You will also need an IdP for all this to work,
    * such as SimpleSAML, for example.)
    *
    * see @login
    */

  def logout() = Action { implicit request =>
    Redirect(sys.env.get("OOREP_URL_LOGOUT").getOrElse(""))
  }

  def show(repertory: String, symptom: String, page: Int, remedyString: String, minWeight: Int) = Action { implicit request: Request[AnyContent] =>
    try {
      getAuthenticatedUser(request) match {
        case None =>
          Ok(views.html.index_lookup(request, repertory, URLEncoder.encode(symptom, StandardCharsets.UTF_8.toString()), page - 1, remedyString, minWeight, s"OOREP - ${symptom} (${repertory})"))
            .withSession("id" -> "-1")
        case Some(member) =>
          Ok(views.html.index_lookup(request, repertory, URLEncoder.encode(symptom, StandardCharsets.UTF_8.toString()), page - 1, remedyString, minWeight, s"OOREP - ${symptom} (${repertory})"))
            .withSession("id" -> member.member_id.toString)
      }
    } catch {
      case e: Exception =>
        Logger.debug(s"GET: show() failed; most likely URLEncoder.encode(): ${e.toString}")
        InternalServerError(views.html.defaultpages.badRequest("GET", request.uri, "Something went wrong. Go to main page, https://www.oorep.com/, and try again, or submit a bug report!"))
    }
  }

  def showMM(materiaMedica: String, symptom: String, page: Int, hideSections: Boolean, remedyString: String) = Action { implicit request: Request[AnyContent] =>
    try {
      getAuthenticatedUser(request) match {
        case None =>
          Ok(views.html.index_lookup_mm(request, materiaMedica, URLEncoder.encode(symptom, StandardCharsets.UTF_8.toString()), page - 1, hideSections, remedyString, s"OOREP - ${symptom} (${materiaMedica})"))
            .withSession("id" -> "-1")
        case Some(member) =>
          Ok(views.html.index_lookup_mm(request, materiaMedica, URLEncoder.encode(symptom, StandardCharsets.UTF_8.toString()), page - 1, hideSections, remedyString, s"OOREP - ${symptom} (${materiaMedica})"))
            .withSession("id" -> member.member_id.toString)
      }
    } catch {
      case e: Exception =>
        Logger.debug(s"GET: showMM() failed; most likely URLEncoder.encode(): ${e.toString}")
        InternalServerError(views.html.defaultpages.badRequest("GET", request.uri, "Something went wrong. Go to main page, https://www.oorep.com/, and try again, or submit a bug report!"))
    }
  }

  def serve_static_html(page: String) = Action { implicit request: Request[AnyContent] =>
    page match {
      case "index" => Ok(views.html.index_landing(request))
      case "cookies" => Ok(views.html.index_static_content(request, views.html.partial.cookies.render(), "OOREP - Privacy policy"))
      case "contact" => Ok(views.html.index_static_content(request, views.html.partial.contact.render(), "OOREP - Contact"))
      case "datenschutz" => Ok(views.html.index_static_content(request, views.html.partial.datenschutz.render(), "OOREP - Datenschutzerklärung"))
      case "faq" => Ok(views.html.index_static_content(request, views.html.partial.faq.render(), "OOREP - Frequently asked questions and answers"))
      case "forgot_password" => Ok(views.html.index_static_content(request, views.html.partial.forgot_password.render(), s"OOREP ${xml.Utility.escape("—")} open online homeopathic repertory"))
      case "impressum" => Ok(views.html.index_static_content(request, views.html.partial.impressum.render(), "OOREP - Impressum", "de"))
      case "register" => Ok(views.html.index_static_content(request, views.html.partial.register.render(), "OOREP - Registration"))
      case _ => NotFound(views.html.defaultpages.notFound("GET", page))
    }
  }

  def changePassword(pcrId: String) = Action { implicit request: Request[AnyContent] =>
    val errorMessage = s"Get: The password-change-request ID is invalid or has expired. Go to main page ${serverUrl(request)} and create a new request, if you still want to change your password."

    val passwordChangeRequestInstance = passwordDao.get(pcrId) match {
      case entry :: Nil => Some(entry)
      case _ => None
    }

    passwordChangeRequestInstance match {
      case Some(pcr) =>
        val pcrDate = new MyDate((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).parse(pcr.date_))

        // A password change request must not be older than 5h...
        if (pcrDate.age() <= 5.0)
          Ok(views.html.index_changepassword(request, pcr.member_id, pcr.id, s"OOREP ${xml.Utility.escape("—")} open online homeopathic repertory"))
        else
          BadRequest(views.html.defaultpages.badRequest("GET", request.uri, errorMessage))

      case None =>
        BadRequest(views.html.defaultpages.badRequest("GET", request.uri, errorMessage))
    }
  }

  def apiDisplayGetErrorPage(message: String) = Action { implicit request: Request[AnyContent] =>
    BadRequest(views.html.defaultpages.badRequest("GET", request.uri, message))
  }

  def apiStoreCookie(name: String, value: String) = Action { request: Request[AnyContent] =>
    if (name == CookieFields.cookiePopupAccepted.toString)
      Ok.withCookies(Cookie(CookieFields.cookiePopupAccepted.toString, value, secure = true, httpOnly = false))
    else if (name == CookieFields.theme.toString)
      Ok.withCookies(Cookie(CookieFields.theme.toString, value, secure = true, httpOnly = false))
    else {
      Logger.error(s"Get: apiStoreCookie(${name}, ${value}): failed")
      BadRequest
    }
  }

  def apiAuthenticate() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(member) =>
        Ok(member.member_id.toString)
      case None =>
        val errStr = s"Get: apiAuthenticate(): User cannot be authenticated."
        Logger.error(errStr)
        Unauthorized(errStr)
    }
  }

  def apiAvailableRemedies() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(member) =>
        Ok(repertoryDao.getRemedies().asJson.toString())
          .withSession("id" -> member.member_id.toString)
      case None =>
        Ok(repertoryDao.getRemedies().asJson.toString())
          .withSession("id" -> "-1")
    }
  }

  def apiAvailableRepertoriesAndRemedies() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(member) =>
        Ok((repertoryDao.getRepsAndRemedies(getAuthenticatedUser(request)).asJson.toString))
          .withSession("id" -> member.member_id.toString)
      case None =>
        Ok((repertoryDao.getRepsAndRemedies(getAuthenticatedUser(request)).asJson.toString))
          .withSession("id" -> "-1")
    }
  }

  def apiAvailableMateriaMedicasAndRemedies() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(member) =>
        Ok(mmDao.getMMsAndRemedies(getAuthenticatedUser(request)).asJson.toString())
          .withSession("id" -> member.member_id.toString)
      case None =>
        Ok(mmDao.getMMsAndRemedies(getAuthenticatedUser(request)).asJson.toString())
          .withSession("id" -> "-1")
    }
  }

  /**
    * Won't actually return files with associated cases, but a list of tuples, (file ID, file header, file creation date).
    */
  def apiSecAvailableFiles(memberId: Int) = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case None =>
        val errStr = "Get: apiSecAvailableFiles() failed: not authenticated"
        Logger.error(errStr)
        Unauthorized(errStr)
      case Some(_) =>
        if (!isUserAuthorized(request, memberId)) {
          val err = s"Get: apiSecAvailableFiles() failed: not authorised"
          Logger.error(err)
          Forbidden(err)
        } else {
          val dbFiles = fileDao.getDbFilesForMember(memberId)
          Ok(dbFiles.asJson.toString())
        }
    }
  }

  def apiSecGetFile(fileId: String) = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case None =>
        Logger.error("Get: apiSecGetFile(): not authenticated")
        Unauthorized("Get: apiSecGetFile(): not authenticated")
      case Some(_) => {
        fileDao.getFIle(fileId.toInt) match {
          case file :: Nil =>
            if (!isUserAuthorized(request, file.member_id)) {
              val err = s"Get: apiSecGetFile() failed: not authorised"
              Logger.error(err)
              Forbidden(err)
            } else {
              Ok(file.asJson.toString())
            }
          case _ =>
            val errStr = "Get: apiSecGetFile() returned nothing"
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
    }
  }

  def apiSecGetCase(caseId: String) = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(member) if (caseId.forall(_.isDigit)) => {
        cazeDao.get(caseId.toInt) match {
          case Right(caze) if (caze.member_id == member.member_id) =>
            if (!isUserAuthorized(request, caze.member_id)) {
              val err = s"Get: apiSecGetCase() failed: not authorised."
              Logger.error(err)
              Forbidden(err)
            } else {
              Ok(caze.asJson.toString())
            }
          case _ =>
            val errStr = "Get: apiSecGetCase() failed: DB returned nothing."
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
      case _ =>
        Unauthorized("apiSecGetCase() failed: not authenticated.")
    }
  }

  /**
    * Returns basically a list of pairs with single entries like
    *
    * (file_description, Some(case_id), Some("[date] 'header'")),
    *
    * but JSON-encoded, of course.  (Descr, None, None) if no cases
    * are associated.
    */
  // TODO: This, unfortunately, is a super slow method and needs improvement!! (or rather, it needs improvement inside FileDao.scala)
  def apiSecFileOverview(fileId: String) = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(_) if (fileId.forall(_.isDigit)) =>
        val files = fileDao.getFIle(fileId.toInt)

        if (files.length > 0) {
          val results =
            files
              .flatMap(f =>
                f.cazes match {
                  case Nil => List((f.description, None, None))
                  case cazes => cazes.map(c => (f.description, Some(c.id), Some(s"[${c.date.take(10)}] '${c.header}'")))
                }
              )

          if (!isUserAuthorized(request, files.head.member_id)) {
            val err = s"Get: apiSecFileOverview() failed: not authorised"
            Logger.error(err)
            Forbidden(err)
          } else {
            Ok(results.asJson.toString())
          }
        }
        else {
          Logger.warn(s"Get: apiSecFileOverview(${fileId}): nothing found.")
          NoContent
        }
      case _ =>
        val err = "Get: apiSecFileOverview failed: not authenticated"
        Logger.error(err)
        Unauthorized(err)
    }
  }

  private def isCrossSiteRequest(request: Request[AnyContent]): Boolean = {
    request.session.get("id") == None && getAuthenticatedUser(request) == None
  }

  def apiLookupRep(repertoryAbbrev: String, symptom: String, page: Int, remedyString: String, minWeight: Int, getRemedies: Int): Action[AnyContent] =
    Action { request: Request[AnyContent] =>
      if (isCrossSiteRequest(request)) {
        val errStr = (s"ERROR: request to ${request.uri} not authorized. Make sure your browser allows cookies. (IP: ${request.remoteAddress})")
        Logger.error(errStr)
        Unauthorized(errStr)
      } else {
        // We don't allow '*' in the middle of a search term.  '*' can only be at beginning or end of a word, whether exact search term or not.
        if (symptom.trim.matches(".*\\w+\\*\\w+.*") || symptom.trim.contains(" * ")) {
          NoContent
        } else {
          val searchTerms = new SearchTerms(symptom.trim)
          val cleanedUpAbbrev = repertoryAbbrev.replaceAll("[^0-9A-Za-z\\-]", "")

          // Check if user is allowed to access the resource at all (might be Private or Protected and user not logged in)
          if (repertoryDao.getRepsAndRemedies(getAuthenticatedUser(request)).find(_.info.abbrev == cleanedUpAbbrev) == None) {
            Logger.warn(s"Get: apiLookupRep(abbrev: ${repertoryAbbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}): user not allowed to access ressource.")
            NoContent
          }
          else {
            // Do actual look-up and return results in case of success.
            repertoryDao.queryRepertory(cleanedUpAbbrev, searchTerms, page, remedyString.trim, minWeight, getRemedies != 0) match {
              case Some((ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, page, results), remedyStats)) if (totalNumberOfPages > 0) =>
                Ok((ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, page, results), remedyStats).asJson.toString())
              case _ =>
                Logger.info(s"Get: apiLookupRep(abbrev: ${repertoryAbbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}): no results found")
                NoContent
            }
          }
        }
      }
    }

  def apiLookupMM(mmAbbrev: String, symptom: String, page: Int, remedyString: String): Action[AnyContent] =
    Action { request: Request[AnyContent] =>
      if (isCrossSiteRequest(request)) {
        val errStr = (s"ERROR: request to ${request.uri} not authorized. Make sure your browser allows cookies. (IP: ${request.remoteAddress})")
        Logger.error(errStr)
        Unauthorized(errStr)
      } else {
        // We don't allow '*' in the middle of a search term.  '*' can only be at beginning or end of a word, whether exact search term or not.
        if (symptom.trim.matches(".*\\w+\\*\\w+.*") || symptom.trim.contains(" * ")) {
          NoContent
        } else {
          val searchTerms = new SearchTerms(symptom.trim)
          val cleanedUpAbbrev = mmAbbrev.replaceAll("[^0-9A-Za-z\\-]", "")

          // Check if user is allowed to access the resource at all (might be Private or Protected and user not logged in)
          if (mmDao.getMMsAndRemedies(getAuthenticatedUser(request)).find(_.mminfo.abbrev == cleanedUpAbbrev) == None) {
            Logger.info(s"Get: apiLookupMM(abbrev: ${mmAbbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}): user not allowed to access ressource.")
            NoContent
          }
          else {
            // Do actual look-up and return results in case of success.
            mmDao.getSectionHits(cleanedUpAbbrev, searchTerms, page, Some(remedyString)) match {
              case Some(sectionHits) if (sectionHits.results.length > 0 || sectionHits.numberOfMatchingSectionsPerChapter.length > 0) =>
                Ok(sectionHits.asJson.toString())
              case _ =>
                Logger.info(s"Get: apiLookupMM(abbrev: ${mmAbbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}): no results found")
                NoContent
            }
          }
        }
      }
    }

}

// Used in HTML-templates/-files
object Get extends ServerUrl {
  val staticServerUrl = serverUrl()
  val staticAssetsPath = "/assets/html"
}
