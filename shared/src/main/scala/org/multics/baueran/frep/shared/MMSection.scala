package org.multics.baueran.frep.shared

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all._

import scala.collection.mutable.ArrayBuffer

case class MMSection(id: Int,
                     mmchapter_id: Int,
                     depth: Int,
                     parent_sec_id: Option[Int],
                     succ_sec_id: Option[Int],
                     heading: Option[String],
                     content: Option[String])
{

  override def equals(that: Any): Boolean = {
    that match {
      case that: MMSection => {
        that.isInstanceOf[MMSection] &&
          that.id == id &&
          that.mmchapter_id == mmchapter_id &&
          that.depth == depth &&
          that.parent_sec_id.getOrElse(-1) == parent_sec_id.getOrElse(-1) &&
          that.succ_sec_id.getOrElse(-1) == succ_sec_id.getOrElse(-1) &&
          that.heading.getOrElse("") == heading.getOrElse("") &&
          that.content.getOrElse("") == content.getOrElse("")
      }
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 29
    (prime * id * mmchapter_id * depth + parent_sec_id.getOrElse(0) + succ_sec_id.getOrElse(0) + heading.getOrElse("").hashCode * content.getOrElse("").hashCode) + prime * prime
  }

  /**
    * Checks if materia medica section matches all words in SearchTerms.posStrings,
    * so long as it doesn't match a word in SearchTerms.negStrings.
    */
  def isMatchFor(searchTerms: SearchTerms, caseSensitive: Boolean = false): Boolean = {
    def isExpressionInContent(expression: String, caseSensitive: Boolean) = searchTerms.isExpressionInTextPassage(expression, content, caseSensitive)
    def isExpressionInTitle(expression: String, caseSensitive: Boolean) = searchTerms.isExpressionInTextPassage(expression, heading, caseSensitive)

    if (searchTerms.positive.length == 0)
      return false

    val isPosMatch = searchTerms.positive.map(expression =>
      isExpressionInContent(expression, caseSensitive) || isExpressionInTitle(expression, caseSensitive))
      .foldLeft(true) { (x, y) => x && y }

    if (isPosMatch && searchTerms.negative.length > 0) {
      searchTerms.negative.map(expression =>
        !isExpressionInContent(expression, caseSensitive) && !isExpressionInTitle(expression, caseSensitive))
        .foldLeft(true) { (x, y) => x && y }
    }
    else
      isPosMatch
  }

  def renderContent(): scalatags.JsDom.TypedTag[dom.html.Div] = div(raw(s" ${renderContent("")}"))

  def render(prefix: String, hideSections: Boolean, symptomString: String, indent: Int) = {
    val headingDiv =
      div(cls:="row",
        button(cls:="close", `type`:="button", scalatags.JsDom.all.id:=s"${prefix}_collapse_section_${id}_button", data.toggle:="collapse",
          onclick := { (_: Event) =>
            val toggleSpan = dom.document.getElementById(s"${prefix}_collapse_section_${id}_span").asInstanceOf[dom.html.Span]

            if (toggleSpan.getAttribute("class") == "oi oi-chevron-right") {
              toggleSpan.setAttribute("class", "oi oi-chevron-bottom")
              toggleSpan.setAttribute("title", "Show less")
            } else {
              toggleSpan.setAttribute("class", "oi oi-chevron-right")
              toggleSpan.setAttribute("title", "Show more...")
            }

            for (i <- 0 to dom.document.getElementsByName(s"parent_${id}").length - 1) {
              val currSubDiv = dom.document.getElementsByName(s"parent_${id}").item(i).asInstanceOf[dom.html.Div]

              if (currSubDiv.getAttribute("class").contains("hide"))
                currSubDiv.setAttribute("class", "collapse show")
              else
                currSubDiv.setAttribute("class", "collapse hide")
            }
          },
          if (hideSections)
            span(scalatags.JsDom.all.id:=s"${prefix}_collapse_section_${id}_span", `type`:=s"${prefix}_mm_section_chevron_span", cls:="oi oi-chevron-right", style:="font-size: 14px;", title:="Show more...", aria.hidden:="true")
          else
            span(scalatags.JsDom.all.id:=s"${prefix}_collapse_section_${id}_span", `type`:=s"${prefix}_mm_section_chevron_span", cls:="oi oi-chevron-bottom", style:="font-size: 14px;", title:="Show less", aria.hidden:="true")
        ),
        div(style:="padding:5px;",
          b(s"${heading.getOrElse("")}")
        )
      )

    val contentDiv =
      div(name:=s"parent_${id}", `type`:=s"${prefix}_mm_section_span", cls := s"collapse ${if (hideSections) "hide" else "show"}",
        raw(s" ${renderContent(symptomString)}")
      )

    if (content == None || content.get.trim.length == 0) {
      div(cls:="col", headingDiv)
    } else {
      div(cls:="col", headingDiv, contentDiv)
    }
  }

  // Markdown-parser for dummies...
  //
  // This sucks a bit as we loose type safety by adding html directly into string.
  // Plus, it needs to be rendered using raw(...).  But for this tiny weeny bit of
  // string replacement, I didn't feel like including yet another library and adding
  // a parser overhead. Some day, it may be necessary though...  => fastparse

  private def renderContent(rawSearchTerm: String) = {
    def replaceBold(input: String) = {
      val pattern = "\\*\\*([^\\*]+)\\*\\*".r
      pattern.replaceAllIn(input, "<b class='text-info'>$1</b>")
    }

    def replaceItalic(input: String) = {
      val pattern = "\\*([^\\*]+)\\*".r
      pattern.replaceAllIn(input, "<i class='text-info'>$1</i>")
    }

    def highlight(input: String): String = {
      val searchTerms = new SearchTerms(rawSearchTerm)

      // If there are no search terms, e.g., when browsing a whole chapter in itself, then return simply unhighlighted text
      if (searchTerms.positive.length == 0)
        return input

      val regExpressions = searchTerms.positive.map(term =>
        // Search with a wildcard, i.e., map wildcards to RE first
        if (term.contains("*")) {
          var result = term

          if (result.startsWith("*") && result.endsWith("*"))
            result = result.substring(1, result.length - 1)
          else if (result.startsWith("*")) {
            result = result.substring(1)
            result = result + "\\b"
          }
          else if (result.endsWith("*")) {
            result = result.substring(0, result.length - 1)
            result = "\\b" + result
          }

          // If we had wildcards in the middle of a word, we would need this, but the PostgreSQL search doesn't currently support it. So we return result instead.
          // result.replace("*", "[a-zA-Z\\-]*")
          result
        }
        // Search with word boundaries for full words only
        else
          "\\b" + term + "\\b"
      )

      val result = "(?i)(" + regExpressions.mkString("|") + ")"
      val pattern = (result).r
      pattern.replaceAllIn(input, """<font style='background:yellow;color:black;'>$1</font>""")
    }

    // The commented out replacements under the HTML-headings are probably OK, so long as headings always end up in a bootstrap card header or
    // are, in fact, a section's heading in the database, which means they're not getting replaced by this function here anyway...
    def replaceMisc(input: String) = {
      var ulScope = false
      val output = new ArrayBuffer[String]()
      input.split('\n').foreach { tmpLine =>
        val pattern = ("\\[\\^([^\\]]+)\\]").r
        val line = pattern.replaceAllIn(tmpLine, "<sup>$1</sup>")

        if (line.trim().startsWith("##### ")) {
          if (ulScope) {
            output.append("</ul>")
            ulScope = false
          }
          // output.append("<h5>" + line.replaceAll("##### ", "") + "</h5>")
        } else if (line.trim().startsWith("#### ")) {
          if (ulScope) {
            output.append("</ul>")
            ulScope = false
          }
          // output.append("<h4>" + line.replaceAll("#### ", "") + "</h4>")
        } else if (line.trim().startsWith("### ")) {
          if (ulScope) {
            output.append("</ul>")
            ulScope = false
          }
          // output.append("<h3>" + line.replaceAll("### ", "") + "</h3>")
        } else if (line.trim().startsWith("## ")) {
          if (ulScope) {
            output.append("</ul>")
            ulScope = false
          }
          // output.append("<h2>" + line.replaceAll("## ", "") + "</h2>")
        } else if (line.trim().startsWith("# ")) {
          if (ulScope) {
            output.append("</ul>")
            ulScope = false
          }
          // output.append("<h1>" + line.replaceAll("## ", "") + "</h1>")
        } else if (line.trim().startsWith("- ")) {
          if (!ulScope) {
            ulScope = true
            output.append("<ul>")
          }
          output.append(line.replaceFirst("\\- ", "<li>") + "</li>")
        }
        else if (line.trim().startsWith("<")) { // ...a tag
          if (ulScope) {
            output.append("</ul>")
            ulScope = false
          }
          output.append(line)
        } else {
          if (ulScope) {
            output.append("</ul>")
            ulScope = false
          }
          output.append("<p>" + line + "</p>")
        }
      }
      output.mkString("\n")
    }

    highlight(replaceItalic(replaceBold(replaceMisc(content.getOrElse("")))))
  }
}

object MMSection {
  implicit val myDecoder: Decoder[MMSection] = deriveDecoder[MMSection]
  implicit val myEncoder: Encoder[MMSection] = deriveEncoder[MMSection]
}
