package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._
import play.api.Logger
import org.multics.baueran.frep._
import shared.CaseRubric
import backend.db.db.DBContext

class Put @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {

  def updateCaseRubricsWeights() = Action { request: Request[AnyContent] =>
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
                    if (cazeDao.updateCaseRubricsWeights(caseID, caseRubrics) > 0) {
                      Logger.debug(s"Post: updateCaseRubricsWeights(): success")
                      Ok
                    }
                    else {
                      Logger.error(s"Post: updateCaseRubricsWeights(): failed")
                      BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateCaseRubricsWeights() failed"))
                    }
                }
              case _ =>
                Logger.error(s"Post: updateCaseRubricsWeights() failed: type conversion error which should never have happened")
                BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateCaseRubricsWeights() failed: type conversion error which should never have happened"))
            }
          case _ => {
            Logger.error(s"Post: updateCaseRubricsWeights() failed: no or the wrong form data received.")
            BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateCaseRubricsWeights() failed: no or the wrong form data received."))
          }
        }
      }
      case Left(err) =>
        BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateCaseRubricsWeights() failed: " + err))
    }
  }

  def updateCaseDescription() = Action { implicit request =>
    isUserAuthenticated(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("memberID"), requestData("caseID"), requestData("casedescription")) match {
          case (Seq(memberIdStr), Seq(cazeIDStr), Seq(casedescription)) if (cazeIDStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            isUserAuthorized(request, memberIdStr.toInt) match {
              case Left(err) =>
                Logger.error(s"Post: updateCaseDescription() failed: not authorised: $err")
                Unauthorized(views.html.defaultpages.unauthorized())
              case Right(_) =>
                if (cazeDao.updateCaseDescription(cazeIDStr.toInt, casedescription) > 0) {
                  Logger.debug(s"Post: updateCaseDescription(): success")
                  Ok
                }
                else {
                  Logger.error(s"Post: updateCaseDescription(): failed")
                  BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateCaseDescription() failed"))
                }
            }
        }
      }
      case Left(err) => BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateCaseDescription() failed: " + err))
    }
  }

  def updateFileDescription() = Action { implicit request =>
    isUserAuthenticated(request) match {
      case Right(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts
        (requestData("filedescr"), requestData("fileId")) match {
          case (Seq(filedescr), Seq(fileId)) if (fileId.forall(_.isDigit)) =>
            // Get file from DB to extract a memberId for authorisation
            fileDao.getFIle(fileId.toInt) match {
              case tmpFile :: Nil =>
                val memberId = tmpFile.member_id
                isUserAuthorized(request, memberId) match {
                  case Left(err) =>
                    Logger.error(s"Post: updateFileDescription() failed: not authorised: $err")
                    Unauthorized(views.html.defaultpages.unauthorized())
                  case Right(_) =>
                    fileDao.changeDescription(fileId.toInt, filedescr)
                    Ok
                }
              case _ =>
                BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateFileDescription() failed"))
            }
          case _ =>
            BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateFileDescription() failed"))
        }
      }
      case Left(err) =>
        BadRequest(views.html.defaultpages.badRequest("POST", request.uri, "updateFileDescription() failed: " + err))
    }
  }

}
