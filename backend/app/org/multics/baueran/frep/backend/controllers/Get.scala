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

  // select * from rubric, rubricremedy, remedy where
  // rubric.abbrev='kent' and
  // rubricremedy.abbrev=rubric.abbrev and
  // remedy.abbrev=rubric.abbrev and
  // fullpath like '%splint%' and
  // rubricremedy.rubricid=rubric.id and
  // remedy.id=rubricremedy.remedyid;

  def repertorise(repertoryAbbrev: String, symptom: String) = Action { request: Request[AnyContent] =>

    def getRemediesForRubric(rubric: Rubric, results: List[(Rubric, RubricRemedy, Remedy)]): Unit = {

    }

    import dbContext._

    val dao = new RepertoryDao(dbContext)
    val results: List[Rubric] = dao.lookupSymptom(repertoryAbbrev, symptom)
    Logger.info(s"dao.lookupSymptom(${repertoryAbbrev}, ${symptom})")

    if (results.size == 0) {
      val errStr = "No results found"
      Logger.error(errStr)
      BadRequest(errStr)
    }
    else {

//      val rawQueryStr =
//        "SELECT * FROM rubric, rubricremedy, remedy WHERE " +
//          s"rubric.abbrev='$repertoryAbbrev' AND " +
//          s"rubric.id IN (${results.map(_.id).mkString(", ")}) AND " +
//          "rubricremedy.abbrev=rubric.abbrev AND " +
//          "remedy.abbrev=rubric.abbrev AND " +
//          "rubricremedy.rubricid=rubric.id " +
//          "AND remedy.id=rubricremedy.remedyid"

//      val queryy = quote {
//        for {
//          rubric <- query[Rubric].filter(rubric => rubric.abbrev == lift(repertoryAbbrev) && liftQuery(results.map(_.id)).contains(rubric.id))
//          rubricremedy <- query[RubricRemedy].join(rr => rr.rubricId == rubric.id && rr.abbrev == rubric.abbrev)
//          remedy <- query[Remedy].join(remedy => remedy.abbrev == rubric.abbrev && rubricremedy.remedyId == remedy.id)
//        } yield (rubric, rubricremedy, remedy)
//      }
//
//      val erg = run(queryy)
//      // println("ERGEBNIS: " + erg.map(r => (r._1.fullPath, r._3.nameAbbrev)).mkString("\n"))
//      println("ERGEBNIS: " + erg.map(r => (r._1.fullPath, (r._2)._3.nameAbbrev)).mkString("\n"))


      var resultSet = ListBuffer[CaseRubric]()

      for (i <- 0 to math.min(100, results.size) - 1) {
        val rubric = results(i)
        val remedyWeightTuples = dao.getRemediesForRubric(rubric)

        // case class CaseRubric(rubric: Rubric, repertoryAbbrev: String, rubricWeight: Int, weightedRemedies: Map[Remedy, Integer])
        // contains sth. like this: (68955) Bladder, afternoon: None, None: Chel.(2), Sulph.(2), Lil-t.(1), Sabad.(1), Petr.(1), Nux-v.(2), Merc.(1), Hyper.(1), Ferr.(1), Equis.(1), Cic.(1), Chin-s.(1), Bell.(1), Indg.(1), Aloe(1), Lyc.(3), Spig.(1), Lith-c.(1), Sep.(1), Coc-c.(1), Chlol.(1), Alumn.(1), Bov.(1)
        resultSet += CaseRubric(rubric, repertoryAbbrev, 1, remedyWeightTuples.map(rwt => WeightedRemedy(rwt._1, rwt._2)).toList)
      }

      Ok(resultSet.asJson.toString())
    }
  }

}
