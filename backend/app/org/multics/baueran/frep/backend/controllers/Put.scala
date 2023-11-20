package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._
import org.multics.baueran.frep._
import shared.CaseRubric
import backend.db.db.DBContext

class Put @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {

  private val Logger = play.api.Logger(this.getClass)

  def updateCaseRubricsUserDefinedValues() = Action { implicit request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        val requestData = request.body.asFormUrlEncoded.get

        (requestData("memberID"), requestData("caseID"), requestData("caserubrics")) match {
          case (Seq(memberIdStr), Seq(cazeIDStr), Seq(caserubricsJson)) if (cazeIDStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            (memberIdStr.toInt, cazeIDStr.toInt, CaseRubric.decodeList(caserubricsJson)) match {
              case (memberId, caseID, Some(caseRubrics)) =>
                if (!isUserAuthorized(request, memberId)) {
                  val err = s"Put: updateCaseRubricsUserDefinedValues() failed: not authorised."
                  Logger.error(err)
                  Forbidden(err)
                } else {
                  if (cazeDao.updateCaseRubricsUserDefinedValues(caseID, caseRubrics) > 0) {
                    Logger.debug(s"Put: updateCaseRubricsUserDefinedValues(): success")
                    Ok
                  }
                  else {
                    Logger.error(s"Put: updateCaseRubricsUserDefinedValues(): failed")
                    BadRequest("Put: updateCaseRubricsUserDefinedValues() failed")
                  }
                }
              case _ =>
                Logger.error(s"Put: updateCaseRubricsUserDefinedValues() failed: type conversion error which should never have happened")
                BadRequest("Put: updateCaseRubricsUserDefinedValues() failed: type conversion error which should never have happened")
            }
          case _ => {
            Logger.error(s"Put: updateCaseRubricsUserDefinedValues() failed: no or the wrong form data received.")
            BadRequest(s"Put: updateCaseRubricsUserDefinedValues() failed: no or the wrong form data received.")
          }
        }
      }
      case None =>
        Unauthorized("Put: updateCaseRubricsUserDefinedValues() failed: not authenticated.")
    }
  }

  def updateCaseDescription() = Action { implicit request =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        val requestData = request.body.asFormUrlEncoded.get

        (requestData("memberID"), requestData("caseID"), requestData("casedescription")) match {
          case (Seq(memberIdStr), Seq(cazeIDStr), Seq(casedescription)) if (cazeIDStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            if (!isUserAuthorized(request, memberIdStr.toInt)) {
              val err = s"Put: updateCaseDescription() failed: not authorised."
              Logger.error(err)
              Forbidden(err)
            } else {
              if (cazeDao.updateCaseDescription(cazeIDStr.toInt, casedescription) > 0) {
                Logger.debug(s"Put: updateCaseDescription(): success")
                Ok
              }
              else {
                val err = s"Put: updateCaseDescription() failed"
                Logger.error(err)
                BadRequest(err)
              }
            }
        }
      }
      case None =>
        Unauthorized("Put: updateCaseDescription() failed: not authenticated.")
    }
  }

  def updateFileDescription() = Action { implicit request =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        val requestData = request.body.asFormUrlEncoded.get

        (requestData("filedescr"), requestData("fileId")) match {
          case (Seq(filedescr), Seq(fileId)) if (fileId.forall(_.isDigit)) =>
            // Get file from DB to extract a memberId for authorisation
            fileDao.getFIle(fileId.toInt) match {
              case tmpFile :: Nil =>
                val memberId = tmpFile.member_id
                if (!isUserAuthorized(request, memberId)) {
                  val err = s"Put: updateFileDescription() failed: not authorised."
                  Logger.error(err)
                  Forbidden(err)
                } else {
                  fileDao.changeDescription(fileId.toInt, filedescr)
                  Ok
                }
              case _ =>
                BadRequest("Put: updateFileDescription() failed")
            }
          case _ =>
            BadRequest("Put: updateFileDescription() failed")
        }
      }
      case None =>
        Unauthorized("Put: updateFileDescription() failed: not authenticated.")
    }
  }

}
