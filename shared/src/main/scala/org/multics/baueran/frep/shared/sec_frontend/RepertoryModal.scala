package org.multics.baueran.frep.shared.sec_frontend

import org.multics.baueran.frep.shared.Info
import rx.{Rx, Var}
import scalatags.rx.all._
import rx.Ctx.Owner.Unsafe._
import scalatags.JsDom.all.{id, input, _}

object RepertoryModal {

  val info: Var[Option[Info]] = Var(None)
  private val err = "ERROR: Repertory name not received!"

  private val dialogHeader = Rx {
    info() match {
      case Some(i) => s"Info for repertory ${i.abbrev}"
      case None => err
    }
  }

  private val title = Rx {
    info() match {
      case Some(i) => i.title
      case None => err
    }
  }

  private val abbrev = Rx {
    info() match {
      case Some(i) => i.abbrev
      case None => err
    }
  }

  private val author = Rx {
    info() match {
      case Some(i) => i.authorFirstName.getOrElse("") + " " + i.authorLastName.getOrElse("")
      case None => err
    }
  }

  private val publisher = Rx {
    info() match {
      case Some(i) => i.publisher.getOrElse("") + " " + i.yearr.getOrElse("")
      case None => err
    }
  }

  private val edition = Rx {
    info() match {
      case Some(i) => i.edition.getOrElse("")
      case None => err
    }
  }

  private val license = Rx {
    info() match {
      case Some(i) => i.license.getOrElse("")
      case None => err
    }
  }

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="repertoryInfoModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", dialogHeader),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              form(
                div(cls:="form-group",
                  label(`for`:="repTitleInfoModal", "Repertory name"),
                  input(cls:="form-control", id:="repTitleInfoModal", readonly:=true, placeholder:=title)
                ),
                div(cls:="form-group",
                  label(`for`:="repAbbrevInfoModal", "Repertory abbreviation"),
                  // input(`type`:="text", cls:="form-control-plaintext", id:="repAbbrevInfoModal", readonly:=true, value:=abbrev)
                  input(cls:="form-control", id:="repAbbrevInfoModal", readonly:=true, placeholder:=abbrev)
                ),
                div(cls:="form-group",
                  label(`for`:="repAuthorInfoModal", "Author"),
                  input(cls:="form-control", id:="repAuthorInfoModal", readonly:=true, placeholder:=author)
                ),
                div(cls:="form-group",
                  label(`for`:="repPublisherInfoModal", "Published by"),
                  input(cls:="form-control", id:="repPublisherInfoModal", readonly:=true, placeholder:=publisher)
                ),
                div(cls:="form-group",
                  label(`for`:="repEditionInfoModal", "Edition"),
                  input(cls:="form-control", id:="repEditionInfoModal", readonly:=true, placeholder:=edition)
                ),
                div(cls:="form-group",
                  label(`for`:="repLicenseInfoModal", "License"),
                  input(cls:="form-control", id:="repLicenseInfoModal", readonly:=true, placeholder:=license)
                )
              )
            )
          )
        )
      )
    )

  }
}
