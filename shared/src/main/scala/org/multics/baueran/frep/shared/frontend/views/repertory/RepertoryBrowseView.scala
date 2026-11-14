package org.multics.baueran.frep.shared.frontend.views.repertory

import io.circe.parser.parse
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.shared.Defs.{HeaderFields, ResourceAccessLvl}
import org.multics.baueran.frep.shared.TopLevelUtilCode.getDocumentCsrfCookie
import org.multics.baueran.frep.shared.frontend._
import org.scalajs.dom
import dom.Event
import dom.html
import scalatags.JsDom
import scalatags.JsDom.all._

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.timers

object RepertoryBrowseView extends TabView {

  private val _prefix = "repertoryBrowseView"

  // Cache: chapter metadata per repertory, and bounded LRU cache for loaded chapter rubrics (last 30, across repertories).
  private val _chaptersCache = mutable.Map.empty[String, List[RepertoryBrowseChapterInfo]]
  private val _chapterRubricsCache = new RepertoryBrowseCache()
  private val _chapterLoading = mutable.Set.empty[(String, Int)]
  private val _expandedChapterIds = mutable.Set.empty[(String, Int)]
  private val _expandedRubricNodeIds = mutable.Map.empty[(String, Int), mutable.Set[Int]]

  private var _selectedRepertory: String = ""
  private var _selectedRepInfo: Option[Info] = None
  private var _availableReps: List[Info] = List.empty
  private var _repsRequested: Boolean = false
  private var _defaultRepertory: Option[String] = None
  private var _autoLoadedDefault: Boolean = false
  private var _loadError: Option[String] = None
  private var _loadingChapters: Boolean = false

  // ------------------------------------------------------------------------------------------------------------------
  // OorepHtml-based UI elements

  object SelectorButton extends OorepHtmlButton {
    override def getId(): String = s"${_prefix}_repSelectionDropDownButton"
    override def apply(): JsDom.TypedTag[html.Element] =
      button(`type` := "button", id := getId(),
        cls := "text-nowrap btn btn-block dropdown-toggle btn-secondary",
        style := "min-width: 195px;",
        data.toggle := "dropdown", aria.haspopup := "true", aria.expanded := "false",
        if (_selectedRepertory.trim.isEmpty) "Repertories" else s"Repertory: ${_selectedRepertory}").asInstanceOf[JsDom.TypedTag[html.Element]]
  }

  object SelectorMenu extends OorepHtmlElement {
    override def getId(): String = s"${_prefix}_repSelectionDropDown"
    override def apply(): JsDom.TypedTag[html.Element] =
      div(cls := "dropdown-menu", id := getId(), aria.labelledby := SelectorButton.getId()).asInstanceOf[JsDom.TypedTag[html.Element]]
  }

  object StatusDiv extends OorepHtmlElement {
    override def getId(): String = s"${_prefix}_status"
    override def apply(): JsDom.TypedTag[html.Element] = div(id := getId()).asInstanceOf[JsDom.TypedTag[html.Element]]
  }

  object ContentDiv extends OorepHtmlElement {
    override def getId(): String = s"${_prefix}_content"
    override def apply(): JsDom.TypedTag[html.Element] = div(id := getId(), cls := "container-fluid", style := "margin-top: 10px;").asInstanceOf[JsDom.TypedTag[html.Element]]
  }

  object ModalTitleEl extends OorepHtmlElement {
    override def getId(): String = s"${_prefix}_rubricModalLabel"
    override def apply(): JsDom.TypedTag[html.Element] = h5(cls := "modal-title", id := getId(), "Rubric").asInstanceOf[JsDom.TypedTag[html.Element]]
  }

  object ModalBodyEl extends OorepHtmlElement {
    override def getId(): String = s"${_prefix}_rubricModalBody"
    override def apply(): JsDom.TypedTag[html.Element] = div(cls := "modal-body", id := getId(), "Loading...").asInstanceOf[JsDom.TypedTag[html.Element]]
  }

  object ModalFooterEl extends OorepHtmlElement {
    override def getId(): String = s"${_prefix}_rubricModalFooter"
    override def apply(): JsDom.TypedTag[html.Element] =
      div(cls := "modal-footer", id := getId(),
        button(`type` := "button", cls := "btn btn-secondary", data.dismiss := "modal", "Close")).asInstanceOf[JsDom.TypedTag[html.Element]]
  }

  object RubricModal extends OorepHtmlElement {
    override def getId(): String = s"${_prefix}_rubricModal"
    override def apply(): JsDom.TypedTag[html.Element] =
      div(cls := "modal fade", id := getId(), tabindex := "-1", role := "dialog", aria.labelledby := ModalTitleEl.getId(), aria.hidden := "true",
        div(cls := "modal-dialog modal-lg", role := "document",
          div(cls := "modal-content",
            div(cls := "modal-header",
              ModalTitleEl(),
              button(`type` := "button", cls := "close", data.dismiss := "modal", aria.label := "Close",
                span(aria.hidden := "true", raw("&times;"))
              )
            ),
            ModalBodyEl(),
            ModalFooterEl()
          )
        )
      ).asInstanceOf[JsDom.TypedTag[html.Element]]
  }

  // ------------------------------------------------------------------------------------------------------------------
  // TabView

  override def getPrefix(): String = _prefix
  override def tabLinkId(): String = "tab_repertory_browse_link"
  override def tabPaneId(): String = "tab_repertory_browse"

  override def toFront(): Unit = {
    Option(dom.document.getElementById(tabLinkId())).foreach { el => el.classList.add("show"); el.classList.add("active") }
    Option(dom.document.getElementById(tabPaneId())).foreach { el => el.classList.add("show"); el.classList.add("active") }
  }

  override def toBack(): Unit = {
    Option(dom.document.getElementById(tabLinkId())).foreach { el => el.classList.remove("show"); el.classList.remove("active") }
    Option(dom.document.getElementById(tabPaneId())).foreach { el => el.classList.remove("show"); el.classList.remove("active") }
  }

  override def tabLink() = {
    a(cls := "nav-link", href := s"#${tabPaneId()}", id := tabLinkId(), data.toggle := "tab",
      onclick := { (_: Event) =>
        timers.setTimeout(0) {
          bindCollapseEvents()
          populateRepertorySelector()
          maybeLoadDefaultRepertory()
        }
        if (CaseSection.size() > 0) CaseSection.showCase(RepertoryView.remedyFormat())
      },
      "Repertory browsing")
  }

  private def chapterDomId(chapterId: Int): String = s"${_prefix}_chapter_${chapterId}"
  private def chapterBodyId(chapterId: Int): String = s"${_prefix}_chapter_body_${chapterId}"
  private def chapterChevronId(chapterId: Int): String = s"${_prefix}_chapter_chevron_${chapterId}"
  private def rubricChildrenDomId(rubricId: Int): String = s"${_prefix}_children_${rubricId}"
  private def rubricChevronId(rubricId: Int): String = s"${_prefix}_chevron_${rubricId}"

  private def isRoot(mother: Option[Int]): Boolean = mother.isEmpty || mother.contains(-1)
  private def rubricLabel(r: RepertoryBrowseRubric): String = r.path.filter(_.trim.nonEmpty).getOrElse(r.fullPath)

  private def chaptersForSelected(): List[RepertoryBrowseChapterInfo] =
    _chaptersCache.getOrElse(_selectedRepertory, Nil)

  // ------------------------------------------------------------------------------------------------------------------
  // Rendering

  private def selectorView(): JsDom.TypedTag[html.Div] =
    div(cls := "container-fluid", style := "margin-top: 23px; margin-bottom: 15px;",
      div(cls := "row",
        div(cls := "dropdown col-md-2", style := "min-width:200px;",
          SelectorButton(), SelectorMenu())
      )
    )

  private def renderStatus(): Unit = {
    Option(dom.document.getElementById(StatusDiv.getId())).foreach { el =>
      el.innerHTML = ""
      val node: JsDom.TypedTag[html.Div] =
        if (_loadingChapters)
          div(cls := "alert alert-info", role := "alert", b("Loading chapters..."))
        else _loadError match {
          case Some(msg) => div(cls := "alert alert-danger", role := "alert", b(msg))
          case None if _selectedRepertory.trim.isEmpty =>
            div(cls := "alert alert-secondary", role := "alert", b("Select a repertory to browse."))
          case None => div()
        }
      el.appendChild(node.render)
    }
  }

  private def renderChapters(): Unit = {
    Option(dom.document.getElementById(ContentDiv.getId())).foreach { container =>
      container.innerHTML = ""
      val chs = chaptersForSelected()
      if (_selectedRepertory.trim.nonEmpty && !_loadingChapters && _loadError.isEmpty && chs.nonEmpty) {
        container.appendChild(
          div(scalatags.JsDom.attrs.id := "RepertoryBrowseViewShowHowManyRubricsAreInARepertory", cls := "alert alert-secondary", role := "alert",
            b(s"${chs.size} chapters (unfold to load rubrics)")
          ).render)
        chs.foreach(ch => container.appendChild(renderChapter(ch).render))
        bindCollapseEvents()
        restoreExpandedChapters()
      }
    }
  }

  private def restoreExpandedChapters(): Unit = {
    val abbrev = _selectedRepertory
    chaptersForSelected().foreach { info =>
      if (_expandedChapterIds.contains((abbrev, info.chapter.id))) {
        loadChapterIfNeeded(abbrev, info.chapter.id)
        Option(dom.document.getElementById(chapterRootCollapseId(info.chapter.id))).foreach { root =>
          root.classList.add("show")
          root.asInstanceOf[dom.html.Element].style.height = "auto"
        }
        toggleChevron(chapterChevronId(info.chapter.id), open = true)
      }
    }
  }

  private def chapterRootCollapseId(chapterId: Int): String = s"${_prefix}_chapter_root_${chapterId}"

  private def renderChapter(info: RepertoryBrowseChapterInfo): JsDom.TypedTag[html.Div] = {
    val ch = info.chapter
    val abbrev = ch.abbrev
    if (info.remedyRubricCount <= 0) {
      div(cls := "card bg-light", style := "margin-bottom: 16px;",
        div(cls := "card-body py-2",
          ch.text,
          span(cls := "text-muted", " (empty)")
        )
      )
    } else {
      div(cls := "card", style := "margin-bottom: 16px;",
        div(cls := "card-header",
          div(cls := "row",
            div(cls := "col",
              b(ch.text),
              span(cls := "text-muted", style := "font-weight: normal;", s" (${info.remedyRubricCount} rubrics)")
            ),
            div(cls := "col-auto text-right",
              a(href := "#", onclick := { (e: Event) => e.preventDefault(); expandEntireChapter(ch.id) }, "Expand all rubrics"),
              " | ",
              a(href := "#", onclick := { (e: Event) => e.preventDefault(); collapseEntireChapter(ch.id) }, "Hide all rubrics")
            )
          )
        ),
        div(cls := "card-body",
          a(id := chapterChevronId(ch.id), cls := "oi oi-chevron-right", aria.hidden := "true",
            href := s"#${chapterRootCollapseId(ch.id)}", data.toggle := "collapse", aria.expanded := "false",
            attr("data-browse-chapter") := ch.id.toString,
            onclick := { (_: Event) => js.timers.setTimeout(0)(loadChapterIfNeeded(abbrev, ch.id)) },
            style := "text-decoration: none;"
          ),
          raw("&nbsp;"),
          b(title := ch.text, ch.text),
          div(id := chapterRootCollapseId(ch.id), cls := "collapse",
            attr("data-browse-chapter") := ch.id.toString,
            div(id := chapterBodyId(ch.id), style := "margin-left: 8px; margin-top: 6px;",
              em(cls := "text-muted", "Unfold to load…")
            )
          )
        )
      )
    }
  }

  // ---- Lazy rubric rendering helpers ----
  private def chapterRubrics(abbrev: String, chapterId: Int): Option[List[RepertoryBrowseRubric]] =
    _chapterRubricsCache.getPage(abbrev, chapterId).map(_.rubrics)

  private def byMotherMap(rubrics: List[RepertoryBrowseRubric]): Map[Int, List[RepertoryBrowseRubric]] =
    rubrics.filter(r => !isRoot(r.mother)).groupBy(_.mother.get)

  private def effectiveRootsFor(chapterId: Int, rubrics: List[RepertoryBrowseRubric]): List[RepertoryBrowseRubric] = {
    val byMother = byMotherMap(rubrics)
    val roots = rubrics.filter(r => isRoot(r.mother)).sortBy(r => (r.fullPath.toLowerCase, r.id))
    val chName = chaptersForSelected().find(_.chapter.id == chapterId).map(_.chapter.text.trim.toLowerCase).getOrElse("")
    if (roots.size == 1) {
      val r = roots.head
      val rLabel = rubricLabel(r).trim.toLowerCase
      val children = byMother.getOrElse(r.id, Nil)
      if (children.nonEmpty && chName.nonEmpty && (rLabel == chName || r.fullPath.trim.toLowerCase == chName))
        children.sortBy(c => (c.fullPath.toLowerCase, c.id))
      else roots
    } else {
      roots.filterNot(r => chName.nonEmpty && rubricLabel(r).trim.toLowerCase == chName && byMother.getOrElse(r.id, Nil).isEmpty)
    }
  }

  private def childrenRendered(container: dom.Element): Boolean =
    container.getAttribute("data-children-rendered") == "true"

  private def renderChildrenForNode(nodeId: Int, chapterId: Int): Unit = {
    val containerOpt = Option(dom.document.getElementById(rubricChildrenDomId(nodeId)))
    containerOpt.foreach { container =>
      if (!childrenRendered(container)) {
        (for {
          rubrics <- chapterRubrics(_selectedRepertory, chapterId)
        } yield rubrics) match {
          case Some(rubrics) =>
            val byMother = byMotherMap(rubrics)
            val byId = rubrics.map(r => r.id -> r).toMap
            val rubricOpt = byId.get(nodeId)
            val directChildren = rubricOpt match {
              case Some(r) => compressDisplay(r, byMother)._3
              case None => byMother.getOrElse(nodeId, Nil).sortBy(c => (c.fullPath.toLowerCase, c.id))
            }
            val fragNode = dom.document.createDocumentFragment()
            directChildren.foreach(c => fragNode.appendChild(renderRubricNodeShell(c, byMother, chapterId).render))
            container.appendChild(fragNode)
            container.setAttribute("data-children-rendered", "true")
          case None => ()
        }
      }
    }
  }

  private def renderChapterTree(chapterId: Int, rubrics: List[RepertoryBrowseRubric]): Unit = {
    Option(dom.document.getElementById(chapterBodyId(chapterId))).foreach { body =>
      body.innerHTML = ""
      if (rubrics.isEmpty) {
        body.appendChild(span(cls := "text-muted", "No rubrics.").render)
      } else {
        val byMother = byMotherMap(rubrics)
        val effectiveRoots = effectiveRootsFor(chapterId, rubrics)
        val fragNode = dom.document.createDocumentFragment()
        effectiveRoots.foreach(r => fragNode.appendChild(renderRubricNodeShell(r, byMother, chapterId).render))
        body.appendChild(fragNode)
        restoreExpandedRubricNodes(chapterId, body)
      }
    }
  }

  private def rubricNodeIdFromCollapseId(tid: String): Option[Int] =
    if (tid.startsWith(s"${_prefix}_children_")) tid.stripPrefix(s"${_prefix}_children_").toIntOption else None

  private def setExpandedRubricNode(abbrev: String, chapterId: Int, nodeId: Int, expanded: Boolean): Unit = {
    val key = (abbrev, chapterId)
    val set = _expandedRubricNodeIds.getOrElseUpdate(key, mutable.Set.empty[Int])
    if (expanded) set.add(nodeId) else set.remove(nodeId)
  }

  private def allExpandableNodeIds(chapterId: Int): List[Int] = {
    chapterRubrics(_selectedRepertory, chapterId) match {
      case Some(rubrics) =>
        val byMother = byMotherMap(rubrics)
        val result = scala.collection.mutable.ListBuffer.empty[Int]
        def walk(r: RepertoryBrowseRubric): Unit = {
          val (_, node, children) = compressDisplay(r, byMother)
          if (children.nonEmpty) {
            result += node.id
            children.foreach(walk)
          }
        }
        effectiveRootsFor(chapterId, rubrics).foreach(walk)
        result.toList.distinct
      case None => Nil
    }
  }

  private def setAllRubricNodesExpanded(abbrev: String, chapterId: Int, body: dom.Element, expanded: Boolean): Unit = {
    val key = (abbrev, chapterId)
    if (expanded) {
      _expandedRubricNodeIds(key) = mutable.Set.from(allExpandableNodeIds(chapterId))
    } else {
      _expandedRubricNodeIds.getOrElseUpdate(key, mutable.Set.empty[Int]).clear()
    }
  }

  private def setAllCollapses(body: dom.Element, show: Boolean): Unit = {
    val nodes = body.querySelectorAll(".collapse")
    for (i <- 0 until nodes.length) {
      val node = nodes.item(i).asInstanceOf[dom.html.Element]
      if (show) {
        node.classList.add("show")
        node.style.height = "auto"
      } else {
        node.classList.remove("show")
        node.style.height = ""
      }
    }
    val chevrons = body.querySelectorAll(s"[id^='${_prefix}_chevron_']")
    for (i <- 0 until chevrons.length) {
      chevrons.item(i).asInstanceOf[dom.Element]
        .setAttribute("class", if (show) "oi oi-chevron-bottom" else "oi oi-chevron-right")
    }
  }

  private def restoreExpandedRubricNodes(chapterId: Int, body: dom.Element): Unit = {
    val abbrev = _selectedRepertory
    val key = (abbrev, chapterId)
    val wanted = _expandedRubricNodeIds.getOrElse(key, mutable.Set.empty[Int]).toSet
    if (wanted.isEmpty) return
    chapterRubrics(abbrev, chapterId) match {
      case Some(rubrics) =>
        val byMother = byMotherMap(rubrics)
        // Build parent mapping in terms of actually rendered (compressed) node ids,
        // following the same effective-roots + lazy child-rendering structure as the UI.
        val renderedIdToParent: Map[Int, Option[Int]] = {
          val m = mutable.Map.empty[Int, Option[Int]]
          def walk(r: RepertoryBrowseRubric, parentRenderedId: Option[Int]): Unit = {
            val (_, node, children) = compressDisplay(r, byMother)
            m.update(node.id, parentRenderedId)
            if (children.nonEmpty) children.foreach(ch => walk(ch, Some(node.id)))
          }
          effectiveRootsFor(chapterId, rubrics).foreach(r => walk(r, None))
          m.toMap
        }
        def ancestors(id: Int, acc: List[Int] = Nil): List[Int] = {
          renderedIdToParent.getOrElse(id, None) match {
            case Some(pid) => ancestors(pid, pid :: acc)
            case None => acc
          }
        }
        val needed = wanted.flatMap(id => ancestors(id).toSet ++ Set(id))
        val batchSize = 20
        var stall = 0
        def step(): Unit = {
          if (chapterRubrics(abbrev, chapterId).isEmpty) return
          var processed = 0
          var remaining = false
          needed.foreach { nodeId =>
            if (processed < batchSize) {
              Option(dom.document.getElementById(rubricChildrenDomId(nodeId))).foreach { c =>
                if (!childrenRendered(c) || !c.classList.contains("show")) {
                  if (!childrenRendered(c)) renderChildrenForNode(nodeId, chapterId)
                  if (!c.classList.contains("show")) {
                    c.classList.add("show")
                    c.asInstanceOf[dom.html.Element].style.height = "auto"
                    toggleChevron(rubricChevronId(nodeId), open = true)
                  }
                  processed += 1
                }
              }
            }
          }
          remaining = needed.exists { nodeId =>
            Option(dom.document.getElementById(rubricChildrenDomId(nodeId))) match {
              case Some(c) => !(childrenRendered(c) && c.classList.contains("show"))
              case None => true
            }
          }
          if (remaining) {
            if (processed > 0) { stall = 0; js.timers.setTimeout(0)(step()) }
            else { stall += 1; if (stall < 50) js.timers.setTimeout(20)(step()) }
          }
        }
        step()
      case None => ()
    }
  }

  private def expandEntireChapter(chapterId: Int): Unit = {
    loadChapterIfNeeded(_selectedRepertory, chapterId)
    _expandedChapterIds.add((_selectedRepertory, chapterId))

    def doExpand(): Unit = {
      // open chapter root
      Option(dom.document.getElementById(chapterRootCollapseId(chapterId))).foreach { root =>
        root.classList.add("show")
        root.asInstanceOf[dom.html.Element].style.height = "auto"
      }
      toggleChevron(chapterChevronId(chapterId), open = true)
      Option(dom.document.getElementById(chapterBodyId(chapterId))).foreach { body =>
        // mark all expandable nodes as wanted
        setAllRubricNodesExpanded(_selectedRepertory, chapterId, body, expanded = true)
      }
      // progressively render+open all expandable nodes in small batches to keep UI responsive
      val batchSize = 15
      var stall = 0
      def step(): Unit = {
        if (chapterRubrics(_selectedRepertory, chapterId).isEmpty) return
        // if user collapsed chapter in the meantime, stop
        if (!_expandedChapterIds.contains((_selectedRepertory, chapterId))) return
        val wanted = _expandedRubricNodeIds.getOrElse((_selectedRepertory, chapterId), mutable.Set.empty[Int]).toSet
        if (wanted.isEmpty) return

        var processed = 0
        var remaining = false
        // process a bounded number of actionable nodes per tick
        wanted.foreach { nodeId =>
          if (processed < batchSize) {
            Option(dom.document.getElementById(rubricChildrenDomId(nodeId))).foreach { c =>
              if (!childrenRendered(c) || !c.classList.contains("show")) {
                if (!childrenRendered(c)) renderChildrenForNode(nodeId, chapterId)
                if (!c.classList.contains("show")) {
                  c.classList.add("show")
                  c.asInstanceOf[dom.html.Element].style.height = "auto"
                  toggleChevron(rubricChevronId(nodeId), open = true)
                }
                processed += 1
              }
            }
          }
        }
        // check if anything still pending (not yet rendered/open or container not yet created)
        remaining = wanted.exists { nodeId =>
          Option(dom.document.getElementById(rubricChildrenDomId(nodeId))) match {
            case Some(c) => !(childrenRendered(c) && c.classList.contains("show"))
            case None => true
          }
        }
        if (remaining) {
          if (processed > 0) { stall = 0; js.timers.setTimeout(20)(step()) }
          else { stall += 1; if (stall < 50) js.timers.setTimeout(20)(step()) }
        }
      }
      step()
    }

    if (!_chapterRubricsCache.contains(_selectedRepertory, chapterId)) js.timers.setTimeout(600)(doExpand()) else doExpand()
  }

  private def collapseEntireChapter(chapterId: Int): Unit = {
    Option(dom.document.getElementById(chapterBodyId(chapterId))).foreach { body =>
      setAllCollapses(body, show = false)
      setAllRubricNodesExpanded(_selectedRepertory, chapterId, body, expanded = false)
    }
    Option(dom.document.getElementById(chapterRootCollapseId(chapterId))).foreach { root =>
      root.classList.remove("show")
      root.asInstanceOf[dom.html.Element].style.height = ""
    }
    _expandedChapterIds.remove((_selectedRepertory, chapterId))
    toggleChevron(chapterChevronId(chapterId), open = false)
  }

  private def plusIcon(r: RepertoryBrowseRubric): Frag =
    if (!r.hasRemedies) frag()
    else frag(
      raw("&nbsp;"),
      button(`type` := "button", cls := "btn btn-sm px-1 py-0 btn-outline-primary oi oi-info",
        style := "vertical-align: middle; line-height: 1; border-radius: 50%; width: 18px; height: 18px; padding: 0 !important; font-size: 10px;",
        title := "Show remedies / add to case",
        aria.hidden := "true",
        onclick := { (e: Event) => e.preventDefault(); e.stopPropagation(); openRubricModal(r) }
      )
    )

  private def tooltipFor(r: RepertoryBrowseRubric): String = {
    val fp = Option(r.fullPath).getOrElse("")
    val chText = chaptersForSelected().find(_.chapter.id == r.chapterId).map(_.chapter.text.trim).getOrElse("")
    if (chText.nonEmpty && fp.nonEmpty && !fp.toLowerCase.startsWith(chText.toLowerCase))
      s"$chText, $fp"
    else fp
  }

  private def compressDisplay(r: RepertoryBrowseRubric, byMother: Map[Int, List[RepertoryBrowseRubric]]): (String, RepertoryBrowseRubric, List[RepertoryBrowseRubric]) = {
    var current = r
    var label = rubricLabel(r)
    var children = byMother.getOrElse(r.id, Nil).sortBy(c => (c.fullPath.toLowerCase, c.id))
    var guard = 0
    while (!current.hasRemedies && children.size == 1 && guard < 50) {
      val next = children.head
      label = label + ", " + rubricLabel(next)
      current = next
      children = byMother.getOrElse(current.id, Nil).sortBy(c => (c.fullPath.toLowerCase, c.id))
      guard += 1
    }
    (label, current, children)
  }

  private def renderRubricNodeShell(r: RepertoryBrowseRubric, byMother: Map[Int, List[RepertoryBrowseRubric]], chapterId: Int): JsDom.TypedTag[html.Div] = {
    val (label, node, children) = compressDisplay(r, byMother)
    val hasChildren = children.nonEmpty
    val tip = tooltipFor(node)

    if (!hasChildren) {
      div(cls := "rep-browse-node", style := "margin-left: 18px; padding: 2px 0;", title := tip,
        label,
        plusIcon(node)
      )
    } else {
      div(cls := "rep-browse-node", style := "margin-left: 10px; padding: 2px 0;", title := tip,
        a(id := rubricChevronId(node.id), cls := "oi oi-chevron-right", aria.hidden := "true",
          href := s"#${rubricChildrenDomId(node.id)}", data.toggle := "collapse", aria.expanded := "false",
          style := "text-decoration: none;"
        ),
        raw("&nbsp;"),
        label,
        plusIcon(node),
        div(id := rubricChildrenDomId(node.id), cls := "collapse",
          attr("data-browse-chapter") := chapterId.toString,
          attr("data-children-rendered") := "false",
          style := "margin-left: 14px; border-left: 1px solid #ddd; padding-left: 6px;"
        )
      )
    }
  }

  private def contentView(): JsDom.TypedTag[html.Div] =
    div(cls := "container-fluid", selectorView(), StatusDiv(), ContentDiv(), RubricModal())

  // ------------------------------------------------------------------------------------------------------------------
  // Collapse events / lazy loading

  private def bindCollapseEvents(): Unit = {
    Option(dom.document.getElementById(tabPaneId())).foreach { pane =>
      val jq = js.Dynamic.global.jQuery
      jq(pane).off("show.bs.collapse.repbrowse hide.bs.collapse.repbrowse")
      jq(pane).on("show.bs.collapse.repbrowse", { (e: dom.Event) =>
        val target = e.target.asInstanceOf[dom.Element]
        val tid = Option(target.getAttribute("id")).getOrElse("")
        val chAttr = Option(target.getAttribute("data-browse-chapter")).getOrElse("")
        if (tid.startsWith(chapterRootCollapseId(0).stripSuffix("0")) && chAttr.nonEmpty) {
          val chId = chAttr.toInt
          _expandedChapterIds.add((_selectedRepertory, chId))
          try loadChapterIfNeeded(_selectedRepertory, chId) catch { case _: Throwable => () }
          toggleChevron(chapterChevronId(chId), open = true)
        } else if (tid.startsWith(s"${_prefix}_children_") && chAttr.nonEmpty) {
          val rid = tid.stripPrefix(s"${_prefix}_children_")
          val chId = chAttr.toInt
          rid.toIntOption.foreach { nodeId =>
            setExpandedRubricNode(_selectedRepertory, chId, nodeId, expanded = true)
            renderChildrenForNode(nodeId, chId)
          }
          toggleChevron(s"${_prefix}_chevron_${rid}", open = true)
        }
      })
      jq(pane).on("hide.bs.collapse.repbrowse", { (e: dom.Event) =>
        val target = e.target.asInstanceOf[dom.Element]
        val tid = Option(target.getAttribute("id")).getOrElse("")
        val chAttr = Option(target.getAttribute("data-browse-chapter")).getOrElse("")
        if (tid.startsWith(chapterRootCollapseId(0).stripSuffix("0")) && chAttr.nonEmpty) {
          val chId = chAttr.toInt
          _expandedChapterIds.remove((_selectedRepertory, chId))
          toggleChevron(chapterChevronId(chId), open = false)
        } else if (tid.startsWith(s"${_prefix}_children_") && chAttr.nonEmpty) {
          val rid = tid.stripPrefix(s"${_prefix}_children_")
          val chId = chAttr.toInt
          rid.toIntOption.foreach(nodeId => setExpandedRubricNode(_selectedRepertory, chId, nodeId, expanded = false))
          toggleChevron(s"${_prefix}_chevron_${rid}", open = false)
        }
      })
    }
  }

  private def toggleChevron(chevronId: String, open: Boolean): Unit = {
    Option(dom.document.getElementById(chevronId)).foreach { el =>
      if (open) el.setAttribute("class", "oi oi-chevron-bottom")
      else el.setAttribute("class", "oi oi-chevron-right")
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  // Data loading & cache

  private def selectRepertory(abbrev: String): Unit = {
    _selectedRepertory = abbrev
    _selectedRepInfo = _availableReps.find(_.abbrev == abbrev)
    _loadError = None
    _expandedChapterIds.retain(_._1 == abbrev)
    _expandedRubricNodeIds.keys.filter(_._1 != abbrev).foreach(_expandedRubricNodeIds.remove)
    SelectorButton.setTextContent(s"Repertory: ${abbrev}")
    if (_chaptersCache.contains(abbrev)) {
      _loadingChapters = false
      renderStatus(); renderChapters()
    } else {
      loadChapters(abbrev)
    }
  }

  private def loadChapters(abbrev: String): Unit = {
    _loadingChapters = true
    _loadError = None
    renderStatus(); renderChapters()
    HttpRequest2("browse_repertory")
      .withQueryParameters(("repertory", abbrev))
      .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
      .onSuccess { (response: String) =>
        parse(response).flatMap(_.as[RepertoryBrowseChaptersResponse]) match {
          case Right(res) =>
            _chaptersCache(abbrev) = res.chapters
            _loadingChapters = false
            _loadError = None
          case Left(err) =>
            _loadingChapters = false
            _loadError = Some(s"Failed to load chapters: ${err.getMessage}")
        }
        if (_selectedRepertory == abbrev) { renderStatus(); renderChapters() }
      }
      .onFailure { (error: String) =>
        _loadingChapters = false
        _loadError = if (error == "204") Some("No chapters available for this repertory.") else Some(s"Failed to load chapters. ${error}")
        if (_selectedRepertory == abbrev) { renderStatus(); renderChapters() }
      }
      .send()
  }

  private def loadChapterIfNeeded(abbrev: String, chapterId: Int): Unit = {
    if (abbrev.trim.isEmpty) return
    _chapterRubricsCache.getPage(abbrev, chapterId) match {
      case Some(cached) =>
        Option(dom.document.getElementById(chapterBodyId(chapterId))).foreach { body =>
          if (Option(body.innerHTML).getOrElse("").contains("Unfold to load") || Option(body.innerHTML).getOrElse("").contains("Loading"))
            renderChapterTree(chapterId, cached.rubrics)
        }
      case None =>
        if (_chapterLoading.contains((abbrev, chapterId))) return
        _chapterLoading.add((abbrev, chapterId))
        Option(dom.document.getElementById(chapterBodyId(chapterId))).foreach(_.innerHTML = em(cls := "text-muted", "Loading…").toString)
        HttpRequest2("browse_chapter")
          .withQueryParameters(("repertory", abbrev), ("chapterId", chapterId.toString))
          .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
          .onSuccess { (response: String) =>
            _chapterLoading.remove((abbrev, chapterId))
            parse(response).flatMap(_.as[RepertoryBrowseChapterResponse]) match {
              case Right(res) =>
                _chapterRubricsCache.addPage(BrowseChapterCachePage(abbrev, chapterId, res.rubrics))
                if (_selectedRepertory == abbrev) renderChapterTree(chapterId, res.rubrics)
              case Left(err) =>
                if (_selectedRepertory == abbrev)
                  Option(dom.document.getElementById(chapterBodyId(chapterId))).foreach(_.innerHTML = span(cls := "text-danger", s"Failed to load chapter: ${err.getMessage}").toString)
            }
          }
          .onFailure { (error: String) =>
            _chapterLoading.remove((abbrev, chapterId))
            if (_selectedRepertory == abbrev)
              Option(dom.document.getElementById(chapterBodyId(chapterId))).foreach(_.innerHTML = span(cls := "text-danger", s"Failed to load chapter. ${error}").toString)
          }
          .send()
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  // Repertory selector & default

  private def renderRepertorySelectorItems(): Unit = {
    Option(dom.document.getElementById(SelectorMenu.getId())).foreach { m =>
      m.innerHTML = ""
      val sorted = _availableReps.sortBy(_.abbrev.toLowerCase)
      val (de, nonDe) = sorted.partition(_.abbrev.toLowerCase.endsWith("-de"))
      val ordered = if (nonDe.nonEmpty && de.nonEmpty) (nonDe, Some(de)) else if (de.nonEmpty) (de, None) else (nonDe, None)
      def appendInfo(info: Info): Unit = {
        val item = a(cls := "dropdown-item", href := "#",
          onclick := { (e: Event) => e.preventDefault(); selectRepertory(info.abbrev) },
          s"${info.abbrev} - ${info.displaytitle.getOrElse(info.title)}").render
        m.appendChild(item)
      }
      ordered match {
        case (first, Some(second)) =>
          first.foreach(appendInfo)
          m.appendChild(div(cls := "dropdown-divider", role := "separator").render)
          second.foreach(appendInfo)
        case (only, None) =>
          only.foreach(appendInfo)
      }
    }
  }

  private def ensureRepertoriesLoaded(): Unit = {
    if (_availableReps.nonEmpty) { renderRepertorySelectorItems(); maybeLoadDefaultRepertory(); return }
    if (_repsRequested) return
    _repsRequested = true
    HttpRequest2("available_rems_and_reps")
      .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
      .onSuccess { (response: String) =>
        parse(response).flatMap(_.as[List[InfoExtended]]) match {
          case Right(infos) =>
            _availableReps = infos.map(_.info)
            _defaultRepertory = infos.find(_.info.access == ResourceAccessLvl.Default).map(_.info.abbrev)
              .orElse(_availableReps.find(_.abbrev == "publicum").map(_.abbrev))
            renderRepertorySelectorItems()
            maybeLoadDefaultRepertory()
          case Left(err) =>
            _repsRequested = false
            println(s"ERROR: RepertoryBrowseView failed to parse available repertories: $err")
        }
      }
      .onFailure { (error: String) =>
        _repsRequested = false
        println(s"ERROR: RepertoryBrowseView failed to load available repertories: $error")
      }
      .send()
  }

  private def maybeLoadDefaultRepertory(): Unit = {
    if (_selectedRepertory.trim.nonEmpty || _autoLoadedDefault) return
    val target = _defaultRepertory.orElse(_availableReps.find(_.abbrev == "publicum").map(_.abbrev)).orElse(_availableReps.headOption.map(_.abbrev))
    target.foreach { abbrev =>
      _autoLoadedDefault = true
      selectRepertory(abbrev)
    }
  }

  private def populateRepertorySelector(): Unit = ensureRepertoriesLoaded()

  // ------------------------------------------------------------------------------------------------------------------
  // Modal / remedies

  private def repertoryDisplayName(): String =
    _selectedRepInfo.map(i => i.abbrev.trim() + " - "+ i.displaytitle.getOrElse(i.title)).filter(_.trim.nonEmpty).getOrElse(_selectedRepertory)

  private def setModal(title: String, body: Frag, footerButtons: List[JsDom.TypedTag[html.Button]]): Unit = {
    Option(dom.document.getElementById(ModalTitleEl.getId())).foreach(_.textContent = title)
    Option(dom.document.getElementById(ModalBodyEl.getId())).foreach { b => b.innerHTML = ""; b.appendChild(div(body).render) }
    Option(dom.document.getElementById(ModalFooterEl.getId())).foreach { f =>
      f.innerHTML = ""
      footerButtons.foreach(btn => f.appendChild(btn.render))
      f.appendChild(button(`type` := "button", cls := "btn btn-secondary", data.dismiss := "modal", "Close").render)
    }
  }

  private def openRubricModal(r: RepertoryBrowseRubric): Unit = {
    val modalTitle = repertoryDisplayName()
    setModal(modalTitle, span("Loading remedies..."), Nil)
    showModal()
    HttpRequest2("browse_rubric_remedies")
      .withQueryParameters(("repertory", _selectedRepertory), ("rubricId", r.id.toString))
      .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
      .onSuccess { (response: String) =>
        parse(response).flatMap(_.as[RepertoryRubricRemedies]) match {
          case Right(result) =>
            val caseId = CaseSection.descr.map(_.id).getOrElse(-1)
            val cazeRubric = CazeRubric(-1, caseId, List(CazeSubRubric(result.rubric, result.weightedRemedies)), 1, None)
            val alreadyInCase = CaseSection.cRubrics.exists(_.containsSubRubric(result.rubric.id, result.rubric.abbrev))
            val remediesFrags =
              if (result.weightedRemedies.isEmpty) List(span("No remedies."))
              else {
                val formatted = new BetterCaseRubric(cazeRubric).getFormattedRemedyNames(RepertoryView.remedyFormat())
                if (formatted.isEmpty) List(span("No remedies."))
                else if (formatted.size <= 1) formatted
                else formatted.init.flatMap(f => List[Frag](f, ", ")) :+ formatted.last
              }
            val body = frag(p(b(result.rubric.fullPath)), p(remediesFrags))
            val addBtn =
              if (alreadyInCase) button(`type` := "button", cls := "btn btn-primary", disabled := true, "Add to case")
              else button(`type` := "button", cls := "btn btn-primary", onclick := { (_: Event) => addToCase(result) }, "Add to case")
            setModal(modalTitle, body, List(addBtn))
          case Left(_) => setModal(modalTitle, span("Failed to load remedies."), Nil)
        }
      }
      .onFailure { (error: String) =>
        val msg = if (error == "204") "No remedies found for this rubric." else s"Failed to load remedies. ${error}"
        setModal(modalTitle, span(msg), Nil)
      }
      .send()
  }

  private def addToCase(result: RepertoryRubricRemedies): Unit = {
    val caseId = CaseSection.descr.map(_.id).getOrElse(-1)
    val cazeRubric = CazeRubric(-1, caseId, List(CazeSubRubric(result.rubric, result.weightedRemedies)), 1, None)
    CaseSection.addRepertoryLookup(cazeRubric)
    CaseSection.updateCaseViewAndDataStructures()
    CaseSection.showCase(RepertoryView.remedyFormat())
    MainView.toggleOnBeforeUnload()
    hideModal()
  }

  private def showModal(): Unit = {
    try { js.Dynamic.global.jQuery(s"#${RubricModal.getId()}").modal("show") } catch { case _: Throwable => () }
  }

  private def hideModal(): Unit = {
    try { js.Dynamic.global.jQuery(s"#${RubricModal.getId()}").modal("hide") } catch { case _: Throwable => () }
  }

  // ------------------------------------------------------------------------------------------------------------------
  // Lifecycle

  private def openBrowseView(): Unit = {
    if (_selectedRepertory.trim.isEmpty)
      maybeLoadDefaultRepertory()
    MainView.resetContentView()
    MainView.tabToFront(this)
  }

  private def landingView(): JsDom.TypedTag[html.Div] =
    div(cls := "container-fluid text-center",
      div(cls := "row justify-content-center align-items-center", style := "margin-bottom: 30px;",
        div(cls := "dropdown col-md-2", style := "min-width:200px; margin-top:20px; margin-right:8px;",
          SelectorButton(), SelectorMenu()
        ),
        div(cls := "col-md-auto", style := "margin-top:20px;",
          button(cls := "btn btn-primary text-nowrap", style := "margin-top: 0;", `type` := "button",
            onclick := { (e: Event) => e.stopPropagation(); openBrowseView() },
            span(cls := "oi oi-book", title := "Browse repertory", aria.hidden := "true"), " Browse repertory"
          )
        )
      )
    )

  private def updateView(): Unit = {
    Option(dom.document.getElementById(tabPaneId())).foreach { pane =>
      pane.innerHTML = ""
      pane.appendChild(contentView().render)
      bindCollapseEvents()
      renderRepertorySelectorItems()
      ensureRepertoriesLoaded()
      renderStatus()
      renderChapters()
    }
    Option(dom.document.getElementById(SelectorButton.getId())).foreach { btn =>
      if (_selectedRepertory.trim.nonEmpty) btn.textContent = s"Repertory: ${_selectedRepertory}"
    }
    if (CaseSection.size() > 0) CaseSection.showCase(RepertoryView.remedyFormat())
  }

  override def drawWithoutResults() = {
    timers.setTimeout(0) { populateRepertorySelector(); maybeLoadDefaultRepertory() }
    landingView()
  }

  override def drawWithResults() = {
    timers.setTimeout(0) { bindCollapseEvents(); populateRepertorySelector(); maybeLoadDefaultRepertory(); renderStatus(); renderChapters() }
    contentView()
  }

  override def onResultsDrawn(): Unit = {
    bindCollapseEvents()
    populateRepertorySelector()
    maybeLoadDefaultRepertory()
    renderStatus()
    renderChapters()
  }

  override def containsAnyResults(): Boolean = _chapterRubricsCache.nonEmpty
  override def containsUnsavedResults(): Boolean = CaseSection.containsUnsavedResults()
  override def updateDataStructures(remedies: List[Remedy]): Unit = ()
}
