package org.multics.baueran.frep.backend.controllers

import scala.collection.mutable.ListBuffer
import javax.inject._
import java.util.Date
import play.api.mvc._
import io.circe.syntax._
import org.multics.baueran.frep.backend.dao.{CazeDao, RepertoryDao}
import org.multics.baueran.frep.backend.repertory._
// import org.multics.baueran.frep.backend.views.html._
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.dao.MemberDao
import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.shared.Defs._
import org.multics.baueran.frep.shared.WeightedRemedy

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

@Singleton
class Get @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) {

  members = new MemberDao(dbContext)
  RepDatabase.setup(dbContext)

  // //////////////////////////////////////////////////////////////////////////////////
  def testDao() = {
    println("********************************** S-DAO *************************************")
    val cazeDao = new CazeDao(dbContext)
    val caze = Caze("header", 1, new Date(), "descr", List.empty)
    if (cazeDao.get("header", 1).length == 0) {
      println("INSERTING.")
      cazeDao.insert(caze)
    }
    else {
      println("NOTHING INSERTED.")
    }
    println("********************************** E-DAO *************************************")
  }
  testDao()
  // //////////////////////////////////////////////////////////////////////////////////


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
      case Nil => BadRequest("Not authorized: bad request")
      case cookies => {
        getFrom(cookies, "oorep_member_email") match {
          case None => BadRequest("Not authorized: user not in database.")
          case Some(memberEmail) => {
            members.getFromEmail(memberEmail) match {
              case Nil => BadRequest(s"Not authorized: user ${memberEmail} not in database.")
              case member :: _ => Ok(member.member_id.toString()).withCookies(cookies.map({ c => Cookie(name = c.name, value = c.value, httpOnly = false) }):_*)
                // Ok(member.member_id.toString()).withCookies(cookies:_*)
            }
          }
        }
      }
    }
  }

  def availableReps() = Action { request: Request[AnyContent] =>
    if (authorizedRequestCookies(request) == List.empty)
      Ok(RepDatabase.availableRepertories().filter(r => r.access == RepAccess.Default || r.access == RepAccess.Public).asJson.toString())
    else
      Ok(RepDatabase.availableRepertories().asJson.toString())
  }

  def repertorise(repertoryAbbrev: String, symptom: String) = Action { request: Request[AnyContent] =>
    RepDatabase.repertory(repertoryAbbrev) match {
      case Some(loadedRepertory) =>
        val resultRubrics = loadedRepertory.findRubrics(symptom).filter(_.chapterId >= 0)

        if (resultRubrics.size <= 0)
          BadRequest("No results found.")
        else {
          val resultSetTooLarge = resultRubrics.size > 100
          var resultSet = ListBuffer[CaseRubric]()

          for (i <- 0 to math.min(100, resultRubrics.size) - 1) {
            val rubric = resultRubrics(i)
            (loadedRepertory.chapter(rubric.chapterId): Option[Chapter]) match {
              case Some(chapter) => {
                val remedyWeightTuples = rubric.remedyWeightTuples(loadedRepertory.remedies, loadedRepertory.rubricRemedies)
                val response = ("(" + rubric.id + ") " + rubric.fullPath + ": " + rubric.path + ", " + rubric.textt + ": "
                               + remedyWeightTuples.map { case (r, w) => r.nameAbbrev + "(" + w + ")" }.mkString(", "))
                // println(response)

                // case class CaseRubric(rubric: Rubric, repertoryAbbrev: String, rubricWeight: Int, weightedRemedies: Map[Remedy, Integer])
                // contains sth. like this: (68955) Bladder, afternoon: None, None: Chel.(2), Sulph.(2), Lil-t.(1), Sabad.(1), Petr.(1), Nux-v.(2), Merc.(1), Hyper.(1), Ferr.(1), Equis.(1), Cic.(1), Chin-s.(1), Bell.(1), Indg.(1), Aloe(1), Lyc.(3), Spig.(1), Lith-c.(1), Sep.(1), Coc-c.(1), Chlol.(1), Alumn.(1), Bov.(1)
                resultSet += CaseRubric(rubric, repertoryAbbrev, 1,
                  remedyWeightTuples.map(f => WeightedRemedy(f._1, f._2)).toList)
                  // remedyWeightTuples.foldLeft(Map[Remedy, Int]()) { (e1, e2) => e1 + (e2._1 -> e2._2) })
              }
              case None => ;
            }
          }

          Ok(resultSet.asJson.toString()) // .withCookies(request.cookies.toList:_*).bakeCookies()
        }
      case None => BadRequest(s"Repertory $repertoryAbbrev not found. Available repertories: " + RepDatabase.availableRepertories().map(_.abbrev).mkString(", "))
    }
  }
}
