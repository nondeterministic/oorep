package org.multics.baueran.frep.backend.controllers

import scala.collection.mutable.ListBuffer
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
import org.multics.baueran.frep.shared.WeightedRemedy
import io.getquill
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
    authorizedRequestCookies(request) match {
      case Nil =>
        val errStr = "Not authorized: bad request"
        Logger.error(errStr)
        BadRequest("Not authorized: bad request")
      case cookies => {
        getFrom(cookies, "oorep_member_email") match {
          case None =>
            val errStr = "Not authorized: user not in database."
            Logger.error(errStr)
            BadRequest("Not authorized: user not in database.")
          case Some(memberEmail) => {
            memberDao.getFromEmail(memberEmail) match {
              case Nil =>
                val errStr = s"Not authorized: user ${memberEmail} not in database."
                Logger.error(errStr)
                BadRequest(errStr)
              case member :: _ =>
                Ok(member.member_id.toString()).withCookies(cookies.map({ c => Cookie(name = c.name, value = c.value, httpOnly = false) }):_*)
            }
          }
        }
      }
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
    doesUserHaveCorrespondingCookie(request, memberId) match {
      case Left(err) =>
        val errStr = "availableFiles() failed: " + err
        Logger.error(errStr)
        BadRequest(errStr)
      case Right(true) =>
        val dao = new FileDao(dbContext)
        Ok(dao.getFilesForMember(memberId).asJson.toString())
    }
  }

  def getFile(memberId: Int, fileId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveCorrespondingCookie(request, memberId) match {
      case Left(err) =>
        Logger.error(err)
        BadRequest(err)
      case Right(true) => {
        val dao = new FileDao(dbContext)
        dao.getFilesForMember(memberId).find(_.header == fileId) match {
          case Some(file) =>
            Ok(file.asJson.toString())
          case None =>
            val errStr = "getFile() returned nothing"
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
    }
  }

  def getCase(memberId: Int, caseId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveCorrespondingCookie(request, memberId) match {
      case Left(err) =>
        Logger.error(err)
        BadRequest(err)
      case Right(true) => {
        val dao = new CazeDao(dbContext)
        dao.get(caseId.toInt, memberId) match {
          case caze::Nil =>
            Ok(caze.asJson.toString())
          case _ =>
            val errStr = "getCase() returned nothing"
            Logger.error(errStr)
            BadRequest(errStr)
        }
      }
    }
  }

  def availableCasesForFile(memberId: Int, fileId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveCorrespondingCookie(request, memberId) match {
      case Left(err) =>
        Logger.error(err)
        BadRequest(err)
      case Right(true) => {
        val dao = new FileDao(dbContext)
        val r = dao.getCasesFromFile(fileId, memberId).asJson.toString()
        Ok(r)
      }
    }
  }

  def repertorise(repertoryAbbrev: String, symptom: String) = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)
    val results: List[CaseRubric] = dao.lookupSymptom(repertoryAbbrev, symptom)
    Logger.info(s"dao.lookupSymptom(${repertoryAbbrev}, ${symptom})")

    if (results.size == 0) {
      val errStr = "No results found"
      Logger.error(errStr)
      BadRequest(errStr)
    }
    else {
      Ok(results.asJson.toString())
    }
  }

}
