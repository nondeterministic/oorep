package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.Element
import scalatags.JsDom

trait OorepHtmlElement {
  def getId(): String

  def getNode(): Option[Element] = {
    dom.document.getElementById(getId()) match {
      case null => None
      case elem => Some(elem)
    }
  }

  def rmAllChildren(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) =>
        while (elem.hasChildNodes())
          elem.removeChild(elem.firstChild)
    }
  }

  // https://stackoverflow.com/questions/6242976/javascript-hide-show-element
  def hide() = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.Element].style.display = "none"
    }
  }

  def show() = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.Element].style.display = ""
    }
  }

  def apply(): JsDom.TypedTag[html.Element]
}

trait OorepHtmlInput extends OorepHtmlElement {
  def setEditable(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.Input].removeAttribute("readonly")
    }
  }

  def setReadOnly(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.Input].setAttribute("readonly", "")
    }
  }

  def setText(newText: String): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.Input].value = newText
    }
  }

  def getText(): String = {
    getNode() match {
      case None => ""
      case Some(elem) => elem.asInstanceOf[html.Input].value
    }
  }
}

trait OorepHtmlTextArea extends OorepHtmlElement {
  def setEditable(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.TextArea].removeAttribute("readonly")
    }
  }

  def setReadOnly(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.TextArea].setAttribute("readonly", "")
    }
  }

  def setText(newText: String): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.TextArea].value = newText
    }
  }

  def getText(): String = {
    getNode() match {
      case None => ""
      case Some(elem) => elem.asInstanceOf[html.TextArea].value
    }
  }
}

trait OorepHtmlButton extends OorepHtmlElement {
  def click(): Unit = {
    getNode() match {
      case None => ;
      case Some(btn) => btn.asInstanceOf[html.Button].click()
    }
  }

  def isDisabled(): Boolean = {
    getNode() match {
      case None => false // If we can't locate the button, we return false. Not great, but what can you do?!
      case Some(elem) => elem.asInstanceOf[html.Button].hasOwnProperty("disabled")
    }
  }

  def enable(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.asInstanceOf[html.Button].removeAttribute("disabled")
    }
  }

  def disable(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) =>
        if (!isDisabled())
          elem.asInstanceOf[html.Button].setAttribute("disabled", "")
    }
  }
}
