package org.multics.baueran.frep.backend.controllers

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import scala.collection.mutable.ListBuffer
import javax.inject._
import play.api.mvc._
import io.circe.syntax._
import org.multics.baueran.frep.backend.dao.{FileDao, RepertoryDao, CazeDao}
import org.multics.baueran.frep.backend.repertory._
// import org.multics.baueran.frep.backend.views.html._
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.dao.MemberDao
import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.shared.Defs._
import org.multics.baueran.frep.shared.WeightedRemedy
import org.multics.baueran.frep.shared.FIle

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
      case Nil => println("ERR: 1"); BadRequest("Not authorized: bad request")
      case cookies => {
        getFrom(cookies, "oorep_member_email") match {
          case None => println("ERR: 2"); BadRequest("Not authorized: user not in database.")
          case Some(memberEmail) => {
            memberDao.getFromEmail(memberEmail) match {
              case Nil => println("ERR: 3"); BadRequest(s"Not authorized: user ${memberEmail} not in database.")
              case member :: _ => Ok(member.member_id.toString()).withCookies(cookies.map({ c => Cookie(name = c.name, value = c.value, httpOnly = false) }):_*)
                // Ok(member.member_id.toString()).withCookies(cookies:_*)
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
      case Left(err) => println("ERR: 4"); BadRequest(err)
      case Right(true) =>
        val dao = new FileDao(dbContext)
        Ok(dao.getFilesForMember(memberId).asJson.toString())
    }
  }

  def getFile(memberId: Int, fileId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveCorrespondingCookie(request, memberId) match {
      case Left(err) => println("ERR 4a"); BadRequest(err)
      case Right(true) => {
        val dao = new FileDao(dbContext)
        dao.getFilesForMember(memberId).find(_.header == fileId) match {
          case Some(file) =>
            Ok(file.asJson.toString())
          case None =>
            println("ERR 4b: " + memberId + ", " + fileId); BadRequest("getFile() returned nothing.")
        }
      }
    }
  }

  def getCase(memberId: Int, caseId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveCorrespondingCookie(request, memberId) match {
      case Left(err) => println("ERR: 5"); BadRequest(err)
      case Right(true) => {
        val dao = new CazeDao(dbContext)
        dao.get(caseId.toInt, memberId) match {
          case caze::Nil =>
            Ok(caze.asJson.toString())
          case _ =>
            println("ERR 6"); BadRequest("getCase() returned nothing.")
        }
      }
    }
  }

  def availableCasesForFile(memberId: Int, fileId: String) = Action { request: Request[AnyContent] =>
    doesUserHaveCorrespondingCookie(request, memberId) match {
      case Left(err) => println("ERR 7"); BadRequest(err)
      case Right(true) => {
        val dao = new FileDao(dbContext)
        val r = dao.getCasesFromFile(fileId, memberId).asJson.toString()
        Ok(r)
      }
    }
  }

  def repertorise(repertoryAbbrev: String, symptom: String) = Action { request: Request[AnyContent] =>
    val dao = new RepertoryDao(dbContext)
    val results = dao.lookupSymptom(repertoryAbbrev, symptom)

    if (results.size == 0) {
      println("ERR 8")
      BadRequest("No results found.")
    }
    else {
      val resultSetTooLarge = results.size > 100
      var resultSet = ListBuffer[CaseRubric]()

      for (i <- 0 to math.min(100, results.size) - 1) {
        val rubric = results(i)
        val remedyWeightTuples = dao.getRemediesForRubric(rubric)
        val response = ("(" + rubric.id + ") " + rubric.fullPath + ": " + rubric.path + ", " + rubric.textt + ": "
          + remedyWeightTuples.map { case (r, w) => r.nameAbbrev + "(" + w + ")" }.mkString(", "))

        // case class CaseRubric(rubric: Rubric, repertoryAbbrev: String, rubricWeight: Int, weightedRemedies: Map[Remedy, Integer])
        // contains sth. like this: (68955) Bladder, afternoon: None, None: Chel.(2), Sulph.(2), Lil-t.(1), Sabad.(1), Petr.(1), Nux-v.(2), Merc.(1), Hyper.(1), Ferr.(1), Equis.(1), Cic.(1), Chin-s.(1), Bell.(1), Indg.(1), Aloe(1), Lyc.(3), Spig.(1), Lith-c.(1), Sep.(1), Coc-c.(1), Chlol.(1), Alumn.(1), Bov.(1)
        resultSet += CaseRubric(rubric, repertoryAbbrev, 1, remedyWeightTuples.map(rwt => WeightedRemedy(rwt._1, rwt._2)).toList)
      }

      Ok(resultSet.asJson.toString()) // .withCookies(request.cookies.toList:_*).bakeCookies()
    }
  }

}
