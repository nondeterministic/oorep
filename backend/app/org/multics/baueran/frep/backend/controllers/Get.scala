package org.multics.baueran.frep.backend.controllers

import javax.inject._
import io.circe.syntax._
import play.api.mvc._
import org.multics.baueran.frep.backend.dao.{CazeDao, FileDao, RepertoryDao}
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.dao.MemberDao
import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.shared.Defs._

/**
 * This controller creates an `Action` to handle HTTP requests to the application's home page.
 */

@Singleton
class Get @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {
  cazeDao = new CazeDao(dbContext)
  fileDao = new FileDao(dbContext)
  memberDao = new MemberDao(dbContext)

  private val Logger = play.api.Logger(this.getClass)

  def index() = Action { request: Request[AnyContent] =>
    if (getRequestCookies(request) == List.empty)
      Ok(views.html.index_landing.render)
    else
      Ok(views.html.index_landing_private.render)
  }

  def serve_partial_html(page: String) = Action { request: Request[AnyContent] =>
    page match {
      case "about" => Ok(views.html.partial.about.render)
      case "cookies" => Ok(views.html.partial.cookies.render("enabled"))
      case "contact" => Ok(views.html.partial.contact.render)
      case "datenschutz" => Ok(views.html.partial.datenschutz.render("enabled"))
      case "disclaimer_text_only" => Ok(views.html.partial.disclaimer_text_only.render)
      case "faq" => Ok(views.html.partial.faq.render)
      case "features" => Ok(views.html.partial.features.render)
      case "impressum" => Ok(views.html.partial.impressum.render)
      case "pricing" => Ok(views.html.partial.pricing.render)
      case _ => BadRequest
    }
  }

  def serve_noscript_whole_html(page: String) = Action { request: Request[AnyContent] =>
    page match {
      case "contact" => Ok(views.html.contact_noscript.render)
      case "cookies" => Ok(views.html.cookies_noscript.render)
      case "datenschutz" => Ok(views.html.datenschutz_noscript.render)
      case "impressum" => Ok(views.html.impressum_noscript.render)
      case _ => BadRequest
    }
  }

  def acceptCookies() = Action { request: Request[AnyContent] =>
    Ok.withCookies(Cookie(CookieFields.cookiePopupAccepted.toString, "1", httpOnly = false))
  }

  /**
    * If method is called, it is expected that the browser has sent a cookie with the
    * request.  The method then checks, if this cookie authenticates the user for access
    * of further application functionality.
    */
  def authenticate() = Action { request: Request[AnyContent] =>
    val cookies = getRequestCookies(request)

    getCookieValue(cookies, CookieFields.id.toString) match {
      case Some(memberIdStr) => {
        isUserAuthenticated(request) match {
          case Right(_) =>
            Ok(memberIdStr).withCookies(cookies.map({ c => Cookie(name = c.name, value = c.value, httpOnly = false) }): _*)
          case _ =>
            val errStr = s"Get: authenticate(): User ${memberIdStr} cannot be authenticated."
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
      case _ =>
        val errStr = s"Get: authenticate(): User ${CookieFields.id.toString} not in database."
        Logger.error(errStr)
        BadRequest(errStr)
    }
  }

  def availableRemediesAndRepertories() = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)

    if (getRequestCookies(request) == List.empty)
      Ok((dao.getAllAvailableRemediesForAnonymousUsers(),
        dao.getAllAvailableRepertoryInfosForAnonymousUsers()).asJson.toString())
    else
      Ok((dao.getAllAvailableRemediesForLoggedInUsers(),
        dao.getAllAvailableRepertoryInfosForLoggedInUsers()).asJson.toString())
  }

  def availableReps() = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)

    if (getRequestCookies(request) == List.empty)
      Ok(dao.getAllAvailableRepertoryInfosForAnonymousUsers().asJson.toString())
    else
      Ok(dao.getAllAvailableRepertoryInfosForLoggedInUsers().asJson.toString())
  }

  /**
    * Won't actually return files with associated cases, but a list of tuples, (file ID, file header).
    */
  def availableFiles(memberId: Int) = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Left(err) =>
        val errStr = "Get: availableFiles(): availableFiles() failed: " + err
        Logger.error(errStr)
        BadRequest(errStr)
      case Right(_) =>
        isUserAuthorized(request, memberId) match {
          case Left(err) =>
            Logger.error(s"Get: availableFiles() failed: not authorised: $err")
            Unauthorized
          case Right(_) =>
            val dbFiles = fileDao.getDbFilesForMember(memberId)
            Ok(dbFiles.map(dbFile => (dbFile.id, dbFile.header)).asJson.toString)
        }
    }
  }

  def getFile(fileId: String) = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Left(err) =>
        Logger.error(err)
        BadRequest(err)
      case Right(_) => {
        fileDao.getFIle(fileId.toInt) match {
          case file :: Nil =>
            isUserAuthorized(request, file.member_id) match {
              case Left(err) =>
                Logger.error(s"Get: getFile() failed: not authorised: $err")
                Unauthorized
              case Right(_) =>
                Ok(file.asJson.toString())
            }
          case _ =>
            val errStr = "Get: getFile() returned nothing"
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
    }
  }

  def getCase(memberId: Int, caseId: String) = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Right(_) if (caseId.forall(_.isDigit)) => {
        val dao = new CazeDao(dbContext)
        dao.get(caseId.toInt) match {
          case Right(caze) if (caze.member_id == memberId) =>
            isUserAuthorized(request, caze.member_id) match {
              case Left(err) =>
                Logger.error(s"Get: getCase() failed: not authorised: $err")
                Unauthorized
              case Right(_) =>
                Ok(caze.asJson.toString())
            }
          case _ =>
            val errStr = "Get: getCase() failed: DB returned nothing."
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
      case _ =>
        BadRequest("getCase() failed")
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
  def fileOverview(fileId: String) = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Right(_) if (fileId.forall(_.isDigit)) =>
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

          isUserAuthorized(request, files.head.member_id) match {
            case Left(err) =>
              Logger.error(s"Get: fileOverview() failed: not authorised: $err")
              Unauthorized
            case Right(_) =>
              Ok(results.asJson.toString())
          }
        }
        else
          BadRequest(s"fileOverview($fileId) failed: no files found.")
      case Left(err) =>
        Logger.error(err)
        BadRequest(err)
      case _ =>
        val err = "Get: fileOverview failed."
        Logger.error(err)
        BadRequest(err)
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
  def repertorise(repertoryAbbrev: String, symptom: String, page: Int, remedyString: String, minWeight: Int, getRemedies: Int) = Action { request: Request[AnyContent] =>
    if (symptom.trim.length >= maxLengthOfSymptoms) {
      val errStr = s"Get: input exceeded max length of ${maxLengthOfSymptoms}."
      Logger.warn(errStr)
      BadRequest(errStr)
    }
    else {
      val dao = new RepertoryDao(dbContext)
      dao.queryRepertory(repertoryAbbrev.trim, symptom.trim, page, remedyString.trim, minWeight, getRemedies != 0) match {
        case Some((ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, page, results), remedyStats)) if (totalNumberOfPages > 0) =>
          Ok((ResultsCaseRubrics(totalNumberOfRepertoryRubrics, totalNumberOfResults, totalNumberOfPages, page, results), remedyStats).asJson.toString())
        case _ =>
          val errStr = s"Get: repertorise(abbrev: ${repertoryAbbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}): no results found"
          Logger.warn(errStr)
          BadRequest(errStr)
      }
    }
  }

  def displayGetErrorPage(message: String) = Action { implicit request: Request[AnyContent] =>
    BadRequest(views.html.defaultpages.badRequest("GET", request.uri, message))
  }

  def show(repertory: String, symptom: String, page: Int, remedyString: String, minWeight: Int) = Action { request: Request[AnyContent] =>
    if (getRequestCookies(request) == List.empty)
      Ok(views.html.index(repertory, symptom, page - 1, remedyString, minWeight, s"OOREP ${xml.Utility.escape("—")} open online homeopathic repertory"))
    else
      Ok(views.html.index_private(repertory, symptom, page - 1, remedyString, minWeight, s"OOREP ${xml.Utility.escape("—")} open online homeopathic repertory"))
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
