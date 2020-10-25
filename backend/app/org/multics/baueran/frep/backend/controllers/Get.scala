package org.multics.baueran.frep.backend.controllers

import javax.inject._
import io.circe.syntax._
import akka.stream.scaladsl.Source
import play.api.mvc._
import org.multics.baueran.frep.backend.dao.{CazeDao, FileDao, RepertoryDao}
import org.multics.baueran.frep.backend.repertory._
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.dao.MemberDao
import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.shared.Defs._
import Defs.maxNumberOfResults

/**
 * This controller creates an `Action` to handle HTTP requests to the application's home page.
 */

@Singleton
class Get @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {
  cazeDao = new CazeDao(dbContext)
  fileDao = new FileDao(dbContext)
  memberDao = new MemberDao(dbContext)

  private val Logger = play.api.Logger(this.getClass)

  RepDatabase.setup(dbContext)

  def index() = Action { request: Request[AnyContent] =>
    if (getRequestCookies(request) == List.empty)
      Redirect(s"${serverUrl(request)}/assets/html/index.html")
    else
      Redirect(s"${serverUrl(request)}/assets/html/private/index.html")
  }

  def acceptCookies() = Action { request: Request[AnyContent] =>
    Redirect(serverUrl(request))
        .withCookies(Cookie(CookieFields.cookiePopupAccepted.toString, "1", httpOnly = false))
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
            Ok(memberIdStr).withCookies(cookies.map({ c => Cookie(name = c.name, value = c.value, httpOnly = false) }):_*)
          case _ =>
            val errStr = s"Get: authenticate(): User cannot be authenticated."
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
      case _ =>
        val errStr = s"Get: authenticate(): User not in database."
        Logger.error(errStr)
        BadRequest(errStr)
    }
  }

  def availableRemedies() = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)

    if (getRequestCookies(request) == List.empty)
      Ok(dao.getAllAvailableRemedies()
        .filter{ case (_,r,_) => r == RepAccess.Default || r == RepAccess.Public }
        .map{ case (abbrev, access, remedyAbbrevs) => (abbrev, access.toString, remedyAbbrevs) }
        .asJson.toString())
    else
      Ok(dao.getAllAvailableRemedies()
        .filter{ case (_,r,_) => r != RepAccess.Protected }
        .map{ case (abbrev, access, remedyAbbrevs) => (abbrev, access.toString, remedyAbbrevs) }
        .asJson.toString())
  }

  def availableReps() = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)

    if (getRequestCookies(request) == List.empty)
      Ok(dao.getAllAvailableRepertoryInfos().filter(r => r.access == RepAccess.Default || r.access == RepAccess.Public).asJson.toString())
    else
      Ok(dao.getAllAvailableRepertoryInfos().filter(_.access != RepAccess.Protected).asJson.toString())
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
   *   (file_description, Some(case_id), Some("[date] 'header'")),
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
  def repertorise(repertoryAbbrev: String, symptom: String, page: Int, remedyString: String, minWeight: Int) = Action { request: Request[AnyContent] =>
    if (symptom.trim.length >= maxLengthOfSymptoms) {
      val errStr = s"Get: input exceeded max length of ${maxLengthOfSymptoms}."
      Logger.warn(errStr)
      BadRequest(errStr)
    }
    else {
      val dao = new RepertoryDao(dbContext)
      dao.queryRepertory(repertoryAbbrev.trim, symptom.trim, page, remedyString.trim, minWeight) match {
        case Some((ResultsCaseRubrics(totalNumberOfResults, totalNumberOfPages, page, results), remedyStats)) if (totalNumberOfPages > 0) =>
          Ok((ResultsCaseRubrics(totalNumberOfResults, totalNumberOfPages, page, results), remedyStats).asJson.toString())
        case _ =>
          val errStr = s"Get: repertorise(abbrev: ${repertoryAbbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}): no results found"
          Logger.warn(errStr)
          BadRequest(errStr)
      }
    }
  }

}
