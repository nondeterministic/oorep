package org.multics.baueran.frep.shared.sec_frontend

import org.multics.baueran.frep.shared.MMInfo
import rx.Ctx.Owner.Unsafe._
import rx.{Rx, Var}
import scalatags.JsDom.all.{id, input, _}
import scalatags.rx.all._

object MMModal {

  val info: Var[Option[MMInfo]] = Var(None)
  private val err = "ERROR: Materia medica name not received!"

  private val dialogHeader = Rx {
    info() match {
      case Some(i) => s"Info for materia medica ${i.abbrev}"
      case None => err
    }
  }

  private val title = Rx {
    info() match {
      case Some(i) => i.fulltitle.getOrElse("")
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
      case Some(i) => i.authorfirstname.getOrElse("") + " " + i.authorlastname.getOrElse("")
      case None => err
    }
  }

  private val publisher = Rx {
    info() match {
      case Some(i) => i.publisher.getOrElse("") + " " + i.yearr
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
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="mmInfoModal",
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
                  label(`for`:="mmTitleInfoModal", "Materia medica name"),
                  input(cls:="form-control", id:="mmTitleInfoModal", readonly:=true, placeholder:=title)
                ),
                div(cls:="form-group",
                  label(`for`:="mmAbbrevInfoModal", "Materia medica abbreviation"),
                  input(cls:="form-control", id:="mmAbbrevInfoModal", readonly:=true, placeholder:=abbrev)
                ),
                div(cls:="form-group",
                  label(`for`:="mmAuthorInfoModal", "Author"),
                  input(cls:="form-control", id:="mmAuthorInfoModal", readonly:=true, placeholder:=author)
                ),
                div(cls:="form-group",
                  label(`for`:="mmPublisherInfoModal", "Published by"),
                  input(cls:="form-control", id:="mmPublisherInfoModal", readonly:=true, placeholder:=publisher)
                ),
                div(cls:="form-group",
                  label(`for`:="mmLicenseInfoModal", "License"),
                  input(cls:="form-control", id:="mmLicenseInfoModal", readonly:=true, placeholder:=license)
                )
              )
            )
          )
        )
      )
    )

  }
}
