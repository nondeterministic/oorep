package org.multics.baueran.frep.backend.controllers

import javax.inject._
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import org.multics.baueran.frep._
import shared.{CaseRubric, Caze, EmailHistory, FIle, MyDate, PasswordChangeRequest}
import backend.db.db.DBContext

class Post @Inject()(cc: ControllerComponents, dbContext: DBContext) extends AbstractController(cc) with ServerUrl {

  private val Logger = play.api.Logger(this.getClass)

  def submitNewPassword() = Action { implicit request =>
    val errorMessage = s"Update of password failed. Please go back to main page ${serverUrl(request)} and try again, if you still want to change your password."
    val password = request.body.asFormUrlEncoded.get("pass1").head
    val pcrId = request.body.asFormUrlEncoded.get("pcrId").head
    val memberId = request.body.asFormUrlEncoded.get("memberId").head.toInt

    passwordDao.get(pcrId) match {
      case pcr :: Nil =>
        if ((new MyDate(pcr.date_)).age() > 5.0 || passwordDao.delete(pcrId) != 1) {
          Logger.error(s"Post: submitNewPassword: delete of old request failed for member ${memberId}.")
          BadRequest(views.html.defaultpages.badRequest("GET", request.uri, errorMessage))
        }
        else {
          if (memberDao.updatePassword(password, memberId) != 1) {
            Logger.error(s"Post: submitNewPassword: updating password failed for member ${memberId}.")
            BadRequest(views.html.defaultpages.badRequest("GET", request.uri, errorMessage))
          }
          else
            Redirect(serverUrl(request))
        }
      case _ =>
        Logger.error(s"Post: submitNewPassword: delete of old request aborted for member ${memberId}.")
        BadRequest(views.html.defaultpages.badRequest("GET", request.uri, errorMessage))
    }
  }

  def requestUsername() = Action { implicit request =>
    val inputEmail: String =
      request.body.asFormUrlEncoded.get("inputEmail").head
        .replaceAll("[ ;']", "")
        .toLowerCase

    // Delete old (-ish) entries from email history to clean up...
    val oldEmailHistory = emailHistoryDao.getAllOlderThan(25.0)
    Logger.debug(s"Deleted ${emailHistoryDao.deleteAll(oldEmailHistory.map(_.id))} old entries from email history table.")

    // We don't want OOREP to send more than 6 emails to the same address within 24 hours (to avoid abuse)
    val recentEmailsToMember = emailHistoryDao.getAllYoungerThan(24.0).filter(_.email == inputEmail)

    memberDao.getFromEmail(inputEmail) match {
      case member :: Nil if (recentEmailsToMember.size < 6) => {
        import backend.mail._

        // ID doesn't matter as it is inserted by the database
        val emailHistoryItem = EmailHistory(-1, (new MyDate()).toString(), member.email, "Username")
        emailHistoryDao.insert(emailHistoryItem)

        val f = Future {
          send a new Mail(
            from = ("info@oorep.com", "OOREP-Support"),
            to = member.email,
            subject = "Username",
            message = s"You (or someone else) has requested that your OOREP username be emailed\n" +
              s"to you. Your OOREP username is:\n\n" +
              s"  ${member.member_name}\n\n" +
              s"IMPORTANT: If you did not request your username to be emailed to you, you can\n" +
              s"ignore this email, but be aware that someone possibly tried to gain access\n" +
              "to your OOREP account!\n\n" +
              s"Thanks,\n" +
              s"OOREP-Team"
          )
        }

        f.onComplete {
          case Success(_) => Logger.debug(s"requestUsername() sent an email to ${member.email}")
          case Failure(e) => Logger.error(s"requestUsername() did NOT send an email: ${e.getMessage}")
        }
      }
      case _ =>
        Logger.warn(s"Post: requestUsername: failed for ${inputEmail}.")
    }

    Redirect(serverUrl(request))
  }

  /**
    * This method is called when a user has pressed the button for requesting an email
    * which contains the link for changing the password. The user will have entered
    * either username or email in order to be identified in the database at all, and to
    * retrieve the corresponding email address.
    *
    * @return It is important that this method always returns Ok-status, whether user
    *         exists or not.
    */

  def requestPasswordChange() = Action { implicit request =>
    def genRandomString(length: Int) = {
      val chars = ('A' to 'Z').toList ::: ('a' to 'z').toList ::: (0 to 9).toList.map(_.toString).map(_.head)
      scala.util.Random.shuffle(chars ::: chars).take(length).mkString
    }

    val inputUsernameOrEmail: String =
      request.body.asFormUrlEncoded.get("inputUsernameOrEmail").head
        .replaceAll("[ ;']", "")
        .toLowerCase

    val memberDatabaseResult = memberDao.getFromEmail(inputUsernameOrEmail) match {
      case member :: Nil => Some(member)
      case _ =>
        memberDao.getFromUsername(inputUsernameOrEmail) match {
          case member :: Nil => Some(member)
          case _ => None
        }
    }

    memberDatabaseResult match {
      case Some(member) =>

        // Delete old (-ish) entries from email history to clean up...
        val oldEmailHistory = emailHistoryDao.getAllOlderThan(25.0)
        Logger.debug(s"Deleted ${emailHistoryDao.deleteAll(oldEmailHistory.map(_.id))} old entries from email history table.")

        // We don't want OOREP to send more than 6 emails to the same address within 24 hours (to avoid abuse)
        val recentEmailsToMember = emailHistoryDao.getAllYoungerThan(24.0).filter(_.email == member.email)

        if (recentEmailsToMember.size < 6) {
          import backend.mail._

          val randomIdString = genRandomString(30)
          val requestDate = new MyDate()
          val emailLink = sys.env.get("OOREP_APPLICATION_HOST").getOrElse("https://www.oorep.com") + "/change_password?id=" + randomIdString

          dbContext.transaction {
            // A member may at most have one password change request in the database
            passwordDao.deleteForMemberId(member.member_id)

            // Create new password change request
            passwordDao.insert(PasswordChangeRequest(randomIdString, requestDate.toString(), member.member_id))
          }

          // ID doesn't matter as it is inserted by the database
          val emailHistoryItem = EmailHistory(-1, (new MyDate()).toString(), member.email, "Password")
          emailHistoryDao.insert(emailHistoryItem)

          val f = Future {
            send a new Mail(
              from = ("info@oorep.com", "OOREP-Support"),
              to = member.email,
              subject = "Password reset",
              message = s"You (or someone else) has requested that you reset your OOREP password.\n\n" +
                s"If you click on the link below, you will be directed to a web-page where you can reset\n" +
                s"your password:\n\n" +
                s"  ${emailLink}\n\n" +
                s"IMPORTANT: If you did not request a password change, you may want to ignore this request,\n" +
                s"but be aware that someone possibly tried to gain access to your OOREP account!\n\n" +
                s"Thanks,\n" +
                s"OOREP-Team"
            )
          }

          f.onComplete {
            case Success(_) => Logger.debug(s"requestPasswordChange() sent an email to ${member.email}")
            case Failure(e) => Logger.error(s"requestPasswordChange() did NOT send an email: ${e.getMessage}")
          }
        }
        else
          Logger.warn(s"Post: requestPasswordChange: failed for ${inputUsernameOrEmail}: already too many emails sent today.")
      case None =>
        Logger.warn(s"Post: requestPasswordChange: failed for ${inputUsernameOrEmail}.")
    }

    Redirect(serverUrl(request))
  }

  /**
    * @return Ok(case ID) of stored case as it is in the DB,
    *         or BadRequest(error string) if something went wrong.
    */

  def saveCaze() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts
        (requestData("fileId"), requestData("case").toList) match {
          case (Seq(fileId), cazeJson :: Nil) => {
            Caze.decode(cazeJson.toString) match {
              case Some(caze) =>
                var newCaseId = caze.id

                if (!isUserAuthorized(request, caze.member_id)) {
                  val err = s"Post: saveCaze() failed: not authorised."
                  Logger.error(err)
                  Forbidden(err)
                } else {
                  if (newCaseId < 0)
                    newCaseId = cazeDao.insert(caze)

                  if (fileDao.addCaseIdToFile(newCaseId, fileId.toInt))
                    Ok(newCaseId.toString)
                  else
                    BadRequest(s"Post: saveCaze() failed: failed to add case with new ID ${newCaseId} (old case id: ${caze.id}) to file with ID ${fileId}.")
                }
              case None =>
                BadRequest("Post: saveCaze() failed: decoding of caze failed. Json wrong? " + cazeJson)
            }
          }
          case _ =>
            BadRequest("Post: saveCaze() failed: no data received in request: " + requestData)
        }
      }
      case None =>
        Unauthorized("Post: saveCaze() failed: not authenticated.")
    }
  }

  def addCaseRubricsToCaze() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        val requestData = request.body.asMultipartFormData.get.dataParts

        (requestData("memberID"), requestData("caseID"), requestData("caserubrics")) match {
          case (Seq(memberIdStr), Seq(cazeIDStr), Seq(caserubricsJson)) if (cazeIDStr.forall(_.isDigit) && (memberIdStr.forall(_.isDigit))) =>
            (memberIdStr.toInt, cazeIDStr.toInt, CaseRubric.decodeList(caserubricsJson)) match {
              case (memberId, caseID, Some(caseRubrics)) =>
                if (!isUserAuthorized(request, memberId)) {
                  val err = s"Post: addRubricsToCaze() failed: not authorised."
                  Logger.error(err)
                  Forbidden(err)
                } else {
                  if (cazeDao.addCaseRubrics(caseID, caseRubrics).length > 0) {
                    Logger.debug(s"Post: addCaseRubricsToCaze(): success")
                    Ok
                  }
                  else {
                    val err = s"Post: addCaseRubricsToCaze() failed"
                    Logger.error(err)
                    BadRequest(err)
                  }
                }
              case _ =>
                val err = s"Post: addCaseRubricsToCaze() failed: type conversion error which should never have happened"
                Logger.error(err)
                BadRequest(err)
            }
          case _ => {
            val err = s"Post: addCaseRubricsToCaze() failed: no or the wrong form data received."
            Logger.error(err)
            BadRequest(err)
          }
        }
      }
      case None =>
        Unauthorized("Post: addCaseRubricsToCaze(): not authenticated.")
    }
  }

  def saveFile() = Action { request: Request[AnyContent] =>
    getAuthenticatedUser(request) match {
      case Some(_) => {
        FIle.decode(request.body.asText.get) match {
          case Some(file) =>
            if (!isUserAuthorized(request, file.member_id)) {
              val err = s"Post: saveFile() failed: not authorised."
              Logger.error( err)
              Forbidden(err)
            } else {
              Ok(fileDao.insert(file).toString())
            }
          case None =>
            BadRequest(s"Post: saveFile() failed for input: ${request.body.asText}.")
        }
      }
      case None =>
        Unauthorized("Post: saveFile(): not authenticated.")
    }
  }

}
