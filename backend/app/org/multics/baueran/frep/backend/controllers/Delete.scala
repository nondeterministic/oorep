package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._
import org.multics.baueran.frep._
import shared.CaseRubric
import backend.db.db.DBContext

class Delete @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {

  private val Logger = play.api.Logger(this.getClass)

  def apiSecDelCaseRubricsFromCaze() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("memberID"), requestData("caseID"), requestData("caserubrics")) match {
          case (Seq(memberIdStr), Seq(cazeIDStr), Seq(caserubricsJson)) if (cazeIDStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            (memberIdStr.toInt, cazeIDStr.toInt, CaseRubric.decodeList(caserubricsJson)) match {
              case (memberId, caseID, Some(caseRubrics)) =>
                if (!isUserAuthorized(request, memberId)) {
                  Logger.error(s"Delete: delCaseRubricsFromCaze() failed: not authorised.")
                  Forbidden
                } else {
                  if (cazeDao.delCaseRubrics(caseID, caseRubrics) > 0) {
                    Logger.debug(s"Delete: delCaseRubricsFromCaze(): success")
                    Ok
                  }
                  else {
                    Logger.error(s"Delete: delCaseRubricsFromCaze(): failed")
                    BadRequest("Delete: delCaseRubrics() failed")
                  }
                }
              case _ =>
                Logger.error(s"Delete: delCaseRubricsFromCaze() failed: type conversion error which should never have happened")
                BadRequest("Delete: delCaseRubricsFromCaze() failed: type conversion error which should never have happened")
            }
          case _ => {
            Logger.error(s"Post: delCaseRubricsFromCaze() failed: no or the wrong form data received.")
            BadRequest("Delete: delRubricsFromCaze() failed: no or the wrong form data received.")
          }
        }
      }
      case None =>
        Unauthorized("Delete: delCaseRubricsFromCaze() failed: not authenticated.")
    }
  }

  def apiSecDelCaze() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("caseId"), requestData("memberId")) match {
          case (Seq(caseIdStr), Seq(memberIdStr)) if (caseIdStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            if (!isUserAuthorized(request, memberIdStr.toInt)) {
              Logger.error(s"Delete: delCaze() failed: not authorised.")
              Forbidden
            } else {
              cazeDao.delete(caseIdStr.toInt)
              Ok
            }
          case _ =>
            BadRequest("Delete: delCaze() failed")
        }
      }
      case None =>
        Unauthorized("Delete: delCaze() failed: not authenticated.")
    }
  }

  def apiSecDelFileAndCases() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("memberId"), requestData("fileId")) match {
          case (Seq(memberIdStr), Seq(fileIdStr)) if (fileIdStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) => {
            if (!isUserAuthorized(request, memberIdStr.toInt)) {
              Logger.error(s"Delete: delFileAndCases() failed: not authorised.")
              Forbidden
            } else {
              fileDao.delFileAndAllCases(fileIdStr.toInt)
              Ok
            }
          }
          case _ =>
            BadRequest("Delete: delFile() failed: wrong data received.")
        }
      }
      case None =>
        Unauthorized("Delete: delFile() failed: not authenticated.")
    }
  }

}