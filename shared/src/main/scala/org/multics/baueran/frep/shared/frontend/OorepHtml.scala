package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import org.scalajs.dom.html
import scalatags.JsDom

trait OorepHtmlElement {
  def getId(): String

  def getNode(): Option[dom.html.Element] = {
    dom.document.getElementById(getId()) match {
      case null => None
      case elem => Some(elem.asInstanceOf[dom.html.Element])
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
      case Some(elem) => elem.style.display = "none"
    }
  }

  def show() = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.style.display = ""
    }
  }

  def focus(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.focus()
    }
  }

  def apply(): JsDom.TypedTag[html.Element]
}

trait OorepHtmlInput extends OorepHtmlElement {
  override def getNode(): Option[dom.html.Input] = {
    dom.document.getElementById(getId()) match {
      case null => None
      case elem => Some(elem.asInstanceOf[dom.html.Input])
    }
  }

  def setEditable(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.removeAttribute("readonly")
    }
  }

  def setReadOnly(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.setAttribute("readonly", "")
    }
  }

  def setText(newText: String): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.value = newText
    }
  }

  def getText(): String = {
    getNode() match {
      case None => ""
      case Some(elem) => elem.value
    }
  }
}

trait OorepHtmlTextArea extends OorepHtmlElement {
  override def getNode(): Option[dom.html.TextArea] = {
    dom.document.getElementById(getId()) match {
      case null => None
      case elem => Some(elem.asInstanceOf[dom.html.TextArea])
    }
  }

  def setEditable(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.removeAttribute("readonly")
    }
  }

  def setReadOnly(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.setAttribute("readonly", "")
    }
  }

  def setText(newText: String): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.value = newText
    }
  }

  def getText(): String = {
    getNode() match {
      case None => ""
      case Some(elem) => elem.value
    }
  }
}

trait OorepHtmlButton extends OorepHtmlElement {
  override def getNode(): Option[dom.html.Button] = {
    dom.document.getElementById(getId()) match {
      case null => None
      case elem => Some(elem.asInstanceOf[dom.html.Button])
    }
  }

  def click(): Unit = {
    getNode() match {
      case None => ;
      case Some(btn) => btn.click()
    }
  }

  def isDisabled(): Boolean = {
    getNode() match {
      case None => false // If we can't locate the button, we return false. Not great, but what can you do?!
      case Some(elem) => elem.hasOwnProperty("disabled")
    }
  }

  def enable(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.removeAttribute("disabled")
    }
  }

  def disable(): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) =>
        if (!isDisabled())
          elem.setAttribute("disabled", "")
    }
  }

  def setTextContent(content: String): Unit = {
    getNode() match {
      case None => ;
      case Some(elem) => elem.textContent = content
    }
  }
}
