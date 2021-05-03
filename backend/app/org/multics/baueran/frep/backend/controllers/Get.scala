package org.multics.baueran.frep.backend.controllers

import javax.inject._
import io.circe.syntax._
import play.api.mvc._
import org.multics.baueran.frep.backend.dao.{CazeDao, EmailHistoryDao, FileDao, MemberDao, PasswordChangeRequestDao, RepertoryDao}
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.shared.Defs._

import java.text.SimpleDateFormat

/**
 * This controller creates an `Action` to handle HTTP requests to the application's home page.
 */

@Singleton
class Get @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {
  cazeDao = new CazeDao(dbContext)
  fileDao = new FileDao(dbContext)
  memberDao = new MemberDao(dbContext)
  passwordDao = new PasswordChangeRequestDao(dbContext)
  emailHistoryDao = new EmailHistoryDao(dbContext)

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
      case Some(uid) =>
        Logger.debug(s"Get: login() completed for user ${uid.toString}.")
        Redirect(sys.env.get("OOREP_APPLICATION_HOST").getOrElse(""))
          .withCookies(
            Cookie(CookieFields.id.toString, uid.toString, httpOnly = false),
            Cookie(CookieFields.cookiePopupAccepted.toString, "1", httpOnly = false)
          )
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

  def show(repertory: String, symptom: String, page: Int, remedyString: String, minWeight: Int) = Action { request: Request[AnyContent] =>
    Ok(views.html.index_lookup(request, repertory, symptom, page - 1, remedyString, minWeight, s"OOREP ${xml.Utility.escape("—")} open online homeopathic repertory"))
  }

  def serve_static_html(page: String) = Action { implicit request: Request[AnyContent] =>
    page match {
      case "index" => Ok(views.html.index_landing(request))
      case "cookies" => Ok(views.html.index_static_content(request, views.html.partial.cookies.render, "OOREP - Privacy policy"))
      case "contact" => Ok(views.html.index_static_content(request, views.html.partial.contact.render, "OOREP - Contact"))
      case "datenschutz" => Ok(views.html.index_static_content(request, views.html.partial.datenschutz.render, "OOREP - Datenschutzerklärung"))
      case "faq" => Ok(views.html.index_static_content(request, views.html.partial.faq.render, "OOREP - Frequently asked questions and answers"))
      case "forgot_password" => Ok(views.html.index_static_content(request, views.html.partial.forgot_password.render, s"OOREP ${xml.Utility.escape("—")} open online homeopathic repertory"))
      case "impressum" => Ok(views.html.index_static_content(request, views.html.partial.impressum.render, "OOREP - Impressum", "de"))
      case "pricing" => Ok(views.html.index_static_content(request, views.html.partial.pricing.render, "OOREP - Pricing"))
      case "register" => Ok(views.html.index_static_content(request, views.html.partial.register.render, "OOREP - Registration"))
      case _ => NotFound(views.html.defaultpages.notFound("GET", page))
    }
  }

  def changePassword(pcrId: String) = Action { implicit request: Request[AnyContent] =>
    val errorMessage = s"The password-change-request ID is invalid or has expired. Go to main page ${serverUrl(request)} and create a new request, if you still want to change your password."

    val passwordChangeRequestInstance = passwordDao.get(pcrId) match {
      case entry :: Nil => Some(entry)
      case _ => None
    }

    passwordChangeRequestInstance match {
      case Some(pcr) =>
        val pcrDate = new MyDate((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).parse(pcr.date_))

        println(pcrDate.age())

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
      Ok.withCookies(Cookie(CookieFields.cookiePopupAccepted.toString, value, httpOnly = false))
    else if (name == CookieFields.theme.toString)
      Ok.withCookies(Cookie(CookieFields.theme.toString, value, httpOnly = false))
    else {
      Logger.error(s"Get: apiStoreCookie(${name}, ${value}): failed")
      BadRequest
    }
  }

  def apiAuthenticate() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(uid) =>
        Ok(uid.toString)
      case None =>
        val errStr = s"Get: apiAuthenticate(): User cannot be authenticated."
        Logger.error(errStr)
        Unauthorized(errStr)
    }
  }

  def apiAvailableRemediesAndRepertories() = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)

    getAuthenticatedUser(request) match {
      case None =>
        Ok((dao.getAllAvailableRemediesForAnonymousUsers(),
          dao.getAllAvailableRepertoryInfosForAnonymousUsers()).asJson.toString())
      case Some(_) =>
        Ok((dao.getAllAvailableRemediesForLoggedInUsers(),
          dao.getAllAvailableRepertoryInfosForLoggedInUsers()).asJson.toString())
    }
  }

  def apiAvailableReps() = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)

    getAuthenticatedUser(request) match {
      case None =>
        Ok(dao.getAllAvailableRepertoryInfosForAnonymousUsers().asJson.toString())
      case Some(_) =>
        Ok(dao.getAllAvailableRepertoryInfosForLoggedInUsers().asJson.toString())
    }
  }

  /**
    * Won't actually return files with associated cases, but a list of tuples, (file ID, file header).
    */
  def apiSecAvailableFiles(memberId: Int) = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case None =>
        val errStr = "Get: apiSecAvailableFiles() failed: not authenticated"
        Logger.error(errStr)
        Unauthorized(errStr)
      case Some(uid) =>
        if (!isUserAuthorized(request, memberId)) {
          val err = s"Get: apiSecAvailableFiles() failed: not authorised"
          Logger.error(err)
          Forbidden(err)
        } else {
          val dbFiles = fileDao.getDbFilesForMember(memberId)
          Ok(dbFiles.map(dbFile => (dbFile.id, dbFile.header)).asJson.toString)
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
      case Some(uid) if (caseId.forall(_.isDigit)) => {
        val dao = new CazeDao(dbContext)
        dao.get(caseId.toInt) match {
          case Right(caze) if (caze.member_id == uid) =>
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

  /**
    * The server response of this method streamed/chunked and ends with "<EOF>" string.
    * Read the method's comments, to see why.  Also, you need special client code for
    * receiving a stream/chunks.
    *
    * @param repertoryAbbrev
    * @param symptom
    * @return
    */
  def apiLookup(repertoryAbbrev: String, symptom: String, page: Int, remedyString: String, minWeight: Int, getRemedies: Int) = Action { request: Request[AnyContent] =>
    if (symptom.trim.length >= maxLengthOfSymptoms) {
      val errStr = s"Get: input exceeded max length of ${maxLengthOfSymptoms}"
      Logger.warn(errStr)
      BadRequest(errStr)
    }
    else {
      val dao = new RepertoryDao(dbContext)
      dao.queryRepertory(repertoryAbbrev.trim, symptom.trim, page, remedyString.trim, minWeight, getRemedies != 0) match {
        case Some((ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, page, results), remedyStats)) if (totalNumberOfPages > 0) =>
          Ok((ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, page, results), remedyStats).asJson.toString())
        case _ =>
          val errStr = s"Get: apiLookup(abbrev: ${repertoryAbbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}): no results found"
          Logger.warn(errStr)
          NoContent
      }
    }
  }

}

// Used in HTML-templates/-files
object Get {
  val staticServerUrl = {
    sys.env.get("OOREP_APPLICATION_HOST") match {
      case Some(host) => host
      case _ => "https://www.oorep.com"
    }
  }
  val staticAssetsPath = "/assets/html"
}
