package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._
import play.api.Logger
import io.circe.syntax._
import org.multics.baueran.frep.backend.dao.{FileDao, RepertoryDao, CazeDao}
import org.multics.baueran.frep.backend.repertory._
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.dao.MemberDao
import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.shared.Defs._
import Defs.maxNumberOfResults

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

@Singleton
class Get @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) {

  memberDao = new MemberDao(dbContext)
  RepDatabase.setup(dbContext)

  def index() = Action { request: Request[AnyContent] =>
    if (authorizedRequestCookies(request) == List.empty)
      Redirect(serverUrl() + "/assets/html/index.html")
    else
      Redirect(serverUrl() + "/assets/html/private/index.html")
  }

  /**
    * If method is called, it is expected that the browser has sent a cookie with the
    * request.  The method then checks, if this cookie authenticates the user for access
    * of further application functionality.
    */
  def authenticate() = Action { request: Request[AnyContent] =>
    val cookies = authorizedRequestCookies(request)

    getFrom(cookies, CookieFields.id.toString) match {
      case Some(memberIdStr) => {
        doesUserHaveAuthorizedCookie(request) match {
          case Right(_) =>
            Ok(memberIdStr).withCookies(cookies.map({ c => Cookie(name = c.name, value = c.value, httpOnly = false) }):_*)
          case _ =>
            val errStr = s"Get: authenticate(): Not authorized: user not authorized to login."
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
      case _ =>
        val errStr = s"Get: authenticate(): Not authorized: user not in database."
        Logger.error(errStr)
        BadRequest(errStr)
    }
  }

  def availableReps() = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)

    if (authorizedRequestCookies(request) == List.empty)
      Ok(dao.getAllAvailableRepertoryInfos().filter(r => r.access == RepAccess.Default || r.access == RepAccess.Public).asJson.toString())
    else
      Ok(dao.getAllAvailableRepertoryInfos().asJson.toString())
  }

  def availableFiles(memberId: Int) = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Left(err) =>
        val errStr = "Get: availableFiles(): availableFiles() failed: " + err
        Logger.error(errStr)
        BadRequest(errStr)
      case Right(true) =>
        val dao = new FileDao(dbContext)
        Ok(dao.getFilesForMember(memberId).asJson.toString())
    }
  }

  def getFile(fileId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Left(err) =>
        Logger.error(err)
        BadRequest(err)
      case Right(true) => {
        val dao = new FileDao(dbContext)
        dao.get(fileId.toInt) match {
          case file :: Nil =>
            Ok(file.asJson.toString())
          case _ =>
            val errStr = "Get: getFile() returned nothing"
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
    }
  }

  def getCase(memberId: Int, caseId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Right(_) if (caseId.forall(_.isDigit)) => {
        val dao = new CazeDao(dbContext)
        dao.get(caseId.toInt) match {
          case Right(caze) if (caze.member_id == memberId) =>
            Ok(caze.asJson.toString())
          case _ =>
            val errStr = "Get: getCase() failed: DB returned nothing."
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
      case _ =>
        BadRequest("Get: getCase() failed.")
    }
  }

  def availableCasesForFile(fileId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveAuthorizedCookie(request) match {
      case Left(err) =>
        Logger.error(err)
        BadRequest(err)
      case Right(true) => {
        val dao = new FileDao(dbContext)
        val r = dao.getCasesFromFile(fileId).asJson.toString()
        Ok(r)
      }
    }
  }

  def repertorise(repertoryAbbrev: String, symptom: String) = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)
    val results: List[CaseRubric] = dao.lookupSymptom(repertoryAbbrev, symptom)

    if (results.size == 0) {
      val errStr = s"Get: repertorise(${repertoryAbbrev}, ${symptom}): no results found"
      Logger.warn(errStr)
      BadRequest(errStr)
    }
    else {
      Logger.debug(s"Get: repertorise(${repertoryAbbrev}, ${symptom}): #results: ${results.size}.")
      Ok(results.take(maxNumberOfResults).asJson.toString())
    }
  }

}
