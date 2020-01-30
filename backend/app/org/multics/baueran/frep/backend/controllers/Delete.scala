package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._
import play.api.Logger
import org.multics.baueran.frep._
import shared.CaseRubric
import backend.db.db.DBContext

class Delete @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {

    def delCaseRubricsFromCaze() = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("memberID"), requestData("caseID"), requestData("caserubrics")) match {
          case (Seq(memberIdStr), Seq(cazeIDStr), Seq(caserubricsJson)) if (cazeIDStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            (memberIdStr.toInt, cazeIDStr.toInt, CaseRubric.decodeList(caserubricsJson)) match {
              case (memberId, caseID, Some(caseRubrics)) =>
                isUserAuthorized(request, memberId) match {
                  case Left(err) =>
                    Logger.error(s"Post: delCaseRubricsFromCaze() failed: not authorised: $err")
                    Unauthorized(views.html.defaultpages.unauthorized())
                  case Right(_) =>
                    if (cazeDao.delCaseRubrics(caseID, caseRubrics) > 0) {
                      Logger.debug(s"Post: delCaseRubricsFromCaze(): success")
                      Ok
                    }
                    else {
                      Logger.error(s"Post: delCaseRubricsFromCaze(): failed")
                      BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "delCaseRubrics() failed"))
                    }
                }
              case _ =>
                Logger.error(s"Post: delCaseRubricsFromCaze() failed: type conversion error which should never have happened")
                BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "delCaseRubricsFromCaze() failed: type conversion error which should never have happened"))
            }
          case _ => {
            Logger.error(s"Post: delCaseRubricsFromCaze() failed: no or the wrong form data received.")
            BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "delRubricsFromCaze() failed: no or the wrong form data received."))
          }
        }
      }
      case Left(err) => BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "delCaseRubricsFromCaze() failed: " + err))
    }
  }

  def delCaze() = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("caseId"), requestData("memberId")) match {
          case (Seq(caseIdStr), Seq(memberIdStr)) if (caseIdStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            isUserAuthorized(request, memberIdStr.toInt) match {
              case Left(err) =>
                Logger.error(s"Post: delCaze() failed: not authorised: $err")
                Unauthorized(views.html.defaultpages.unauthorized())
              case Right(_) =>
                cazeDao.delete(caseIdStr.toInt)
                Ok
            }
          case _ =>
            BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "delCaze() failed"))
        }
      }
      case Left(err) =>
        BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "delCaze() failed: " + err))
    }
  }

  def delFileAndCases() = Action { request: Request[AnyContent] =>
    isUserAuthenticated(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("memberId"), requestData("fileId")) match {
          case (Seq(memberIdStr), Seq(fileIdStr)) if (fileIdStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) => {
            isUserAuthorized(request, memberIdStr.toInt) match {
              case Left(err) =>
                Logger.error(s"Post: delFileAndCases() failed: not authorised: $err")
                Unauthorized(views.html.defaultpages.unauthorized())
              case Right(_) =>
                fileDao.delFileAndAllCases(fileIdStr.toInt)
                Ok
            }
          }
          case _ =>
            BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "delFile() failed: wrong data received."))
        }
      }
      case Left(err) =>
        BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "delFile() failed: " + err))
    }
  }

}