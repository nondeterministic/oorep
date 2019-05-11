package org.multics.baueran.frep.shared.sec_frontend

import scalatags.JsDom.all._
import rx.Var
import rx.Rx
import rx.Ctx.Owner.Unsafe._
import org.scalajs.dom.html.Anchor

class FileModal {

  val files: Var[List[Anchor]] =
    Var(
      List(
        a(cls:="list-group-item list-group-item-action", data.toggle:="list", id:="none", href:="#list-profile", role:="tab", "<no files created yet>").render
      )
    )

  def empty() = files() = List()

  def appendItem(listItem: Anchor) = Rx { files() = listItem :: files.now }

}
