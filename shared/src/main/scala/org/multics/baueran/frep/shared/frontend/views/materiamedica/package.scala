package org.multics.baueran.frep.shared.frontend.views.materiamedica

import scalatags.JsDom
import JsDom.all.{datalist, _}
import org.scalajs.dom
import dom.html.{Button, DataList, Div, Input}
import dom.Event

import org.multics.baueran.frep.shared.frontend._

package object uielements {

  var _latestSymptomString: Option[String] = None
  var _latestRemedyString: Option[String] = None
  var _selectedMateriaMedicaAbbrev: Option[String] = None

  private var _advancedSearchOptionsVisible = false
  def advancedSearchOptionsVisible(): Boolean = _advancedSearchOptionsVisible

  object SearchButtonsDiv {
    def getId(): String = s"${MateriaMedicaView.getPrefix()}_mainViewSearchButtons"

    def getNode(): Option[dom.html.Div] = {
      dom.document.getElementById(getId()) match {
        case null => None
        case elem => Some(elem.asInstanceOf[dom.html.Div])
      }
    }
  }

  object AdvancedSearchControlsDiv {
    def getId(): String = s"${MateriaMedicaView.getPrefix()}_advancedSearchControlsDiv"

    def getNode(): Option[dom.html.Div] = {
      dom.document.getElementById(getId()) match {
        case null => None
        case elem => Some(elem.asInstanceOf[dom.html.Div])
      }
    }
  }

  object BasicSearchOptionsButton {
    def getId(): String = s"${MateriaMedicaView.getPrefix()}_buttonMainViewBasicSearch"

    def getNode(): Option[dom.html.Button] = {
      dom.document.getElementById(getId()) match {
        case null => None
        case elem => Some(elem.asInstanceOf[dom.html.Button])
      }
    }

    def onShowBasicSearchOptions(): Unit = {
      val parentDiv = AdvancedSearchControlsDiv.getNode()
      parentDiv.get.innerHTML = ""

      val basicButton = BasicSearchOptionsButton.getNode().get
      val buttonDiv = SearchButtonsDiv.getNode().get
      buttonDiv.replaceChild(AdvancedSearchOptionsButton().render, basicButton)

      _advancedSearchOptionsVisible = false
    }

    def apply(): JsDom.TypedTag[Button] = {
      if (!MainView.someResultsHaveBeenShown())
        WithoutResults.SearchButtonsDiv.BasicSearchOptionsButton()
      else
        WithResults.SearchButtonsDiv.BasicSearchOptionsButton()
    }
  }

  object AdvancedSearchOptionsButton {
    def getId(): String = s"${MateriaMedicaView.getPrefix()}_buttonMainViewAdvancedSearch"

    def getNode(): Option[dom.html.Button] = {
      dom.document.getElementById(getId()) match {
        case null => None
        case elem => Some(elem.asInstanceOf[dom.html.Button])
      }
    }

    def onShowAdvancedSearchOptions(): Unit = {
      // If for some reason the remedy input is already visible (which it shouldn't be), do nothing and return.
      if (AdvancedSearchControls.RemedyInput.getNode() != None)
        return

      val parentDiv = AdvancedSearchControlsDiv.getNode()

      if (parentDiv == None)
        return

      parentDiv.get.appendChild(AdvancedSearchControls().render)

      // Add currently selected repertory's remedies for search
      MateriaMedicaView.refreshRemedyDataList()

      val advancedButton = WithResults.SearchButtonsDiv.AdvancedSearchOptionsButton.getNode().get
      val buttonDiv = SearchButtonsDiv.getNode().get
      buttonDiv.replaceChild(BasicSearchOptionsButton().render, advancedButton)

      _advancedSearchOptionsVisible = true
    }

    def apply(): JsDom.TypedTag[Button] = {
      if (!MainView.someResultsHaveBeenShown())
        WithoutResults.SearchButtonsDiv.AdvancedSearchOptionsButton()
      else
        WithResults.SearchButtonsDiv.AdvancedSearchOptionsButton()
    }
  }

  object InputSearchTerms extends OorepHtmlInput {
    override def getId(): String = s"${MateriaMedicaView.getPrefix()}inputField"

    override def apply(): JsDom.TypedTag[Input] = {
      input(cls := "form-control", `id` := getId(),
        onkeydown := { (event: dom.KeyboardEvent) =>
          if (event.keyCode == 13) {
            event.stopPropagation()
            val symptom = getText()
            val abbrev = _selectedMateriaMedicaAbbrev.getOrElse("")
            val remedyName = Some(AdvancedSearchControls.RemedyInput.getText().trim)
            MateriaMedicaView.doLookup(abbrev, symptom, None, remedyName)
          }
        }, `placeholder` := "Enter some search terms (for example: menses, night)"
      )
    }
  }

  object WithoutResults extends OorepHtmlElement {
    override def getId(): String = s"${MateriaMedicaView.getPrefix()}_JHBJHGJKHGKJjhgkjdsflsdfi43bndfsn4323432r5435"

    object AdvancedSearchControlsDiv extends OorepHtmlElement {
      override def getId(): String = uielements.AdvancedSearchControlsDiv.getId()

      override def apply(): JsDom.TypedTag[Div] = div(cls := "col-sm-10", id := getId())
    }

    object SearchButtonsDiv extends OorepHtmlElement {
      override def getId(): String = uielements.SearchButtonsDiv.getId()

      object FindButton extends OorepHtmlButton {
        override def getId(): String = s"${MateriaMedicaView.getPrefix()}_findButton"

        override def apply(): JsDom.TypedTag[Button] = {
          button(cls := "btn btn-primary text-nowrap", style := "width: 140px; margin:5px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              val symptom = InputSearchTerms.getText()
              val abbrev = _selectedMateriaMedicaAbbrev.getOrElse("")
              val remedyName = Some(AdvancedSearchControls.RemedyInput.getText().trim)
              MateriaMedicaView.doLookup(abbrev, symptom, None, remedyName)
            }, span(cls := "oi oi-magnifying-glass", title := "Find", aria.hidden := "true"), " Find"
          )
        }
      }

      object BasicSearchOptionsButton extends OorepHtmlButton {
        override def getId(): String = uielements.BasicSearchOptionsButton.getId()

        override def apply(): JsDom.TypedTag[Button] = {
          button(id := getId(), cls := "btn btn-secondary text-nowrap", style := "width: 140px; margin: 5px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              uielements.BasicSearchOptionsButton.onShowBasicSearchOptions()
            }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"), " Basic")
        }
      }

      object AdvancedSearchOptionsButton extends OorepHtmlButton {
        override def getId(): String = uielements.AdvancedSearchOptionsButton.getId()

        override def apply(): JsDom.TypedTag[Button] = {
          button(id := getId(), cls := "btn btn-secondary text-nowrap", style := "width: 140px; margin: 5px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              uielements.AdvancedSearchOptionsButton.onShowAdvancedSearchOptions()
            }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"), " Advanced...")
        }
      }

      override def apply(): JsDom.TypedTag[Div] = {
        div(id := getId(), cls := "col-sm-12 text-center", style := "margin-top:20px;",
          FindButton(),
          AdvancedSearchOptionsButton()
        )
      }
    }

    object ResultStatusAlertsDiv extends OorepHtmlElement {
      override def getId(): String = s"${MateriaMedicaView.getPrefix()}_resultStatusAlerts"

      override def apply(): JsDom.TypedTag[Div] = div(id := getId(), cls := "container-fluid", style := "margin-top:23px;")
    }

    override def apply(): JsDom.TypedTag[Div] = {
      div(cls := "container-fluid text-center",
        div(cls := "row justify-content-center",
          MMSelectionDropdown(),
          div(cls := "col-sm", style := "margin-top:20px;",
            InputSearchTerms()
          )
        ),
        div(cls := "row justify-content-center",
          AdvancedSearchControlsDiv()
        ),
        SearchButtonsDiv(),
        ResultStatusAlertsDiv()
      )
    }
  }

  object WithResults extends OorepHtmlElement {
    override def getId(): String = s"${MateriaMedicaView.getPrefix()}_dsj43jHJk34nmbJHhjk34&*sdsmMN2323csbn4353654"

    object AdvancedSearchControlsDiv extends OorepHtmlElement {
      override def getId(): String = uielements.AdvancedSearchControlsDiv.getId()

      override def apply(): JsDom.TypedTag[Div] = div(cls := "col-md-12 text-center", id := getId())
    }

    object SearchButtonsDiv extends OorepHtmlElement {
      override def getId(): String = uielements.SearchButtonsDiv.getId()

      object FindButton extends OorepHtmlButton {
        override def getId(): String = s"${MateriaMedicaView.getPrefix()}_findButton"

        override def apply(): JsDom.TypedTag[Button] = {
          button(cls := "btn btn-primary text-nowrap", style := "width: 80px; margin-right:5px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              val symptom = InputSearchTerms.getText()
              val abbrev = _selectedMateriaMedicaAbbrev.getOrElse("")
              val remedyName = Some(AdvancedSearchControls.RemedyInput.getText().trim)
              MateriaMedicaView.doLookup(abbrev, symptom, None, remedyName)
            },
            span(cls := "oi oi-magnifying-glass", title := "Find", aria.hidden := "true")
          )
        }
      }

      object BasicSearchOptionsButton extends OorepHtmlButton {
        override def getId(): String = uielements.BasicSearchOptionsButton.getId()

        override def apply(): JsDom.TypedTag[Button] = {
          button(`id` := getId(), cls := "btn btn-secondary text-nowrap", style := "width: 70px; margin-right:5px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              uielements.BasicSearchOptionsButton.onShowBasicSearchOptions()
            },
            span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true")
          )
        }
      }

      object AdvancedSearchOptionsButton extends OorepHtmlButton {
        override def getId(): String = uielements.AdvancedSearchOptionsButton.getId()

        override def apply(): JsDom.TypedTag[Button] = {
          button(id := getId(), cls := "btn btn-secondary text-nowrap", style := "width: 70px; margin-right: 5px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              uielements.AdvancedSearchOptionsButton.onShowAdvancedSearchOptions()
            }, span(cls := "oi oi-cog", title := "Toggle options", aria.hidden := "true"))
        }
      }

      object FindAgainButton extends OorepHtmlButton {
        override def getId(): String = s"${MateriaMedicaView.getPrefix()}_buttonMainViewFindAgain"

        override def apply(): JsDom.TypedTag[Button] = {
          button(cls := "btn btn-secondary text-nowrap", style := "width: 70px;", `type` := "button",
            onclick := { (event: Event) =>
              event.stopPropagation()
              (_latestSymptomString, _latestRemedyString) match {
                case (Some(symptomString), Some(remedyName)) if (symptomString.trim.length > 0 && remedyName.trim.length > 0) =>
                  uielements.AdvancedSearchOptionsButton.onShowAdvancedSearchOptions()
                  AdvancedSearchControls.RemedyInput.setText(remedyName)
                  InputSearchTerms.setText(symptomString)
                  InputSearchTerms.focus()
                case (Some(symptomString), _) if (symptomString.trim.length > 0) =>
                  InputSearchTerms.setText(symptomString)
                  InputSearchTerms.focus()
                case (_, Some(remedyName)) if (remedyName.trim.length > 0) =>
                  uielements.AdvancedSearchOptionsButton.onShowAdvancedSearchOptions()
                  AdvancedSearchControls.RemedyInput.setText(remedyName)
                  AdvancedSearchControls.RemedyInput.focus()
                case _ => ; // This case should never fire
              }
            },
            span(cls := "oi oi-action-redo", title := "Find again", aria.hidden := "true")
          )
        }
      }

      override def apply(): JsDom.TypedTag[Div] = {
        div(id := getId(), cls := "col-md-auto text-center center-block", style := "margin-top:20px;",
          FindButton(),
          AdvancedSearchOptionsButton(),
          FindAgainButton()
        )
      }
    }

    override def apply(): JsDom.TypedTag[Div] = {
      div(cls := "container-fluid", style := "padding-bottom:20px",
        div(cls := "col-md-12 justify-content-md-center",
          div(cls := "row justify-content-center",
            MMSelectionDropdown(),
            div(cls := "col-md-7", style := "margin-top:20px;",
              InputSearchTerms()
            ),
            SearchButtonsDiv()
          ),
          AdvancedSearchControlsDiv(),
          div(id := s"${MateriaMedicaView.getPrefix()}_result_div", cls := "container-fluid", style := "margin-top:23px;",
            div(MateriaMedicaView.getResultsHtml(),
              div(cls := "container-fluid", id := s"${MateriaMedicaView.getPrefix()}_paginationDiv",
                MateriaMedicaView.getPaginatorHtml() match {
                  case None => div()
                  case Some(resultDiv) => resultDiv
                }
              )
            )
          )
        )
      )
    }
  }

  private object AdvancedSearchControls extends OorepHtmlElement {
    override def getId(): String = s"${MateriaMedicaView.getPrefix()}_advancedSearchControlsContent"

    object RemedyList extends OorepHtmlElement {
      override def getId(): String = s"${MateriaMedicaView.getPrefix()}_remedyDataList"

      override def apply(): JsDom.TypedTag[DataList] = datalist(`id` := getId())
    }

    object RemedyInput extends OorepHtmlInput {
      override def getId(): String = s"${MateriaMedicaView.getPrefix()}_inputRemedy"

      override def apply(): JsDom.TypedTag[Input] = {
        input(cls := "form-control", `id` := getId(), list := RemedyList.getId(),
          onkeydown := { (event: dom.KeyboardEvent) =>
            if (event.keyCode == 13) {
              event.stopPropagation()
              val symptom = InputSearchTerms.getText()
              val abbrev = _selectedMateriaMedicaAbbrev.getOrElse("")
              val remedyName = Some(getText().trim)
              MateriaMedicaView.doLookup(abbrev, symptom, None, remedyName)
            }
          }, `placeholder` := "Enter a remedy abbreviation or fullname (for example: Sil. or Silica)"
        )
      }
    }

    override def apply(): JsDom.TypedTag[Div] = {
      div(id := getId(), cls := "row justify-content-center", style := "margin-top:15px;",
        div(cls := "col-md-10",
          div(cls := "row",
            div(cls := "col-md-auto my-auto", "Remedy:"),
            div(cls := "col",
              RemedyInput(),
              RemedyList()
            )
          )
        )
      )
    }
  }

  object MMSelectionDropdown extends OorepHtmlElement {
    object Button extends OorepHtmlButton {
      override def getId(): String = s"${MateriaMedicaView.getPrefix()}mmSelectionDropDownButton"

      override def apply(): JsDom.TypedTag[Button] = {
        button(`type` := "button",
          style := "min-width: 195px;",
          cls := "text-nowrap btn btn-block dropdown-toggle btn-secondary",
          data.toggle := "dropdown",
          `id` := getId(),
          "Materia Medicas"
        )
      }

      object Label {
        def refresh(): Unit = {
          getNode() match {
            case None => ;
            case Some(button) =>
              if (_selectedMateriaMedicaAbbrev == None && MateriaMedicaView.defaultMMAbbrev() != None)
                button.textContent = s"M. Medica: ${MateriaMedicaView.defaultMMAbbrev()}"
              else if (_selectedMateriaMedicaAbbrev != None)
                button.textContent = s"M. Medica: ${_selectedMateriaMedicaAbbrev.getOrElse("Materia Medicas")}"
              else {
                ; // do nothing
              }
          }
        }
      }
    }

    object Menu extends OorepHtmlElement {
      override def getId(): String = s"${MateriaMedicaView.getPrefix()}mmSelectionDropDownMenu"

      override def apply(): JsDom.TypedTag[Div] = div(cls := "dropdown-menu", `id` := getId())

      def refresh(): Unit = {
        getNode() match {
          case Some(dropDown: dom.html.Div) =>
            rmAllChildren()
            MateriaMedicaView.materiaMedicas().getAll()
              .sortBy { case (mminfo, remedies) => mminfo.abbrev }
              .foreach { case (mminfo, remedies) =>
                dropDown.append(
                  a(cls := "dropdown-item", href := "#",
                    onclick := { (_: Event) =>
                      _selectedMateriaMedicaAbbrev = Some(mminfo.abbrev)
                      MMSelectionDropdown.Button.setTextContent(s"M. Medica: ${mminfo.abbrev}")
                      MateriaMedicaView.refreshRemedyDataList()
                    },
                    s"${mminfo.abbrev} - ${mminfo.displaytitle.getOrElse("")}"
                  ).render
                )
              }
          case _ => ;
        }
      }
    }

    override def getId(): String = s"${MateriaMedicaView.getPrefix()}mmSelection_sdfjhsdljkhsdkjhsdjkhsdkjfhsdkfjhsdf"

    override def apply(): JsDom.TypedTag[Div] = {
      div(cls := "dropdown col-md-2", style := "min-width:200px; margin-top:20px;", `id` := getId(),
        Button(), Menu()
      )
    }

    def refresh(): Unit = {
      Menu.refresh()
      Button.Label.refresh()
    }

  }

}
