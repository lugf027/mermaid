package io.lugf027.github.mermaid.core.diagram.stateDiagram

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.layout.LayoutData
import io.lugf027.github.mermaid.core.layout.LayoutEdge
import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 状态图数据库 - 对标 mermaid-js stateDb.ts
 *
 * 管理状态(State)、转换(Transition)和嵌套状态。
 */
class StateDb : DiagramDB {

    private val log = Logger("StateDb")

    // ── 状态 ──────────────────────────────────────────
    private val states: LinkedHashMap<String, StateNode> = linkedMapOf()
    private val transitions: MutableList<StateTransition> = mutableListOf()
    private val styleClasses: LinkedHashMap<String, StateStyleDef> = linkedMapOf()
    private var startEndCount = 0
    private var nodeCount = 0

    // ── DiagramDB 基础接口 ────────────────────────────
    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescription: String = ""
    private var direction: String = "TB"

    override fun clear() {
        states.clear()
        transitions.clear()
        styleClasses.clear()
        startEndCount = 0
        nodeCount = 0
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
        direction = "TB"
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription
    override fun getDirection(): String = direction
    override fun setDirection(direction: String) { this.direction = direction }

    // ── 状态管理 ──────────────────────────────────────

    fun addState(
        id: String,
        type: StateType = StateType.DEFAULT,
        description: String? = null,
        parentId: String? = null
    ) {
        if (!states.containsKey(id)) {
            states[id] = StateNode(
                id = id,
                type = type,
                descriptions = if (description != null) mutableListOf(description) else mutableListOf(),
                domId = "state-$id-${nodeCount++}",
                parentId = parentId
            )
            log.debug("Added state: $id (type=$type)")
        } else if (description != null) {
            states[id]?.descriptions?.add(description)
        }
    }

    fun addStateWithAlias(id: String, alias: String, parentId: String? = null) {
        addState(id, parentId = parentId)
        states[id]?.alias = alias
    }

    fun setStateType(id: String, type: StateType) {
        addState(id)
        states[id]?.type = type
    }

    fun addNote(stateId: String, position: String, text: String) {
        addState(stateId)
        states[stateId]?.note = StateNote(position = position, text = text)
    }

    fun getStates(): Map<String, StateNode> = states

    // ── [*] 处理 ──────────────────────────────────────

    /**
     * 将 [*] 转为唯一的 start/end id
     */
    fun startIdIfNeeded(id: String, parentId: String? = null): String {
        if (id != "[*]") return id
        val startId = if (parentId != null) "${parentId}_start" else "start${startEndCount++}"
        addState(startId, StateType.START, parentId = parentId)
        return startId
    }

    fun endIdIfNeeded(id: String, parentId: String? = null): String {
        if (id != "[*]") return id
        val endId = if (parentId != null) "${parentId}_end" else "end${startEndCount++}"
        addState(endId, StateType.END, parentId = parentId)
        return endId
    }

    // ── 转换管理 ──────────────────────────────────────

    fun addTransition(from: String, to: String, label: String = "", parentId: String? = null) {
        val fromId = startIdIfNeeded(from, parentId)
        val toId = endIdIfNeeded(to, parentId)
        addState(fromId, parentId = parentId)
        addState(toId, parentId = parentId)
        transitions.add(StateTransition(from = fromId, to = toId, label = label))
        log.debug("Added transition: $fromId --> $toId : $label")
    }

    fun getTransitions(): List<StateTransition> = transitions

    // ── 嵌套状态（复合状态）──────────────────────────

    fun addCompositeState(id: String, alias: String? = null, parentId: String? = null) {
        addState(id, StateType.DEFAULT, parentId = parentId)
        states[id]?.isComposite = true
        if (alias != null) states[id]?.alias = alias
    }

    fun setCompositeStateDirection(id: String, dir: String) {
        states[id]?.dir = dir
    }

    // ── 样式管理 ──────────────────────────────────────

    fun addStyleClass(id: String, styles: List<String>) {
        styleClasses[id] = StateStyleDef(id = id, styles = styles)
    }

    fun applyStyleClass(stateIds: List<String>, styleId: String) {
        for (stateId in stateIds) {
            states[stateId]?.cssClasses = stateId
        }
    }

    fun applyInlineStyle(stateIds: List<String>, styles: List<String>) {
        for (stateId in stateIds) {
            states[stateId]?.cssStyles?.addAll(styles)
        }
    }

    // ── LayoutData 构建 ──────────────────────────────

    fun getData(config: MermaidConfig): LayoutData {
        val stateConfig = config.state
        val nodeSpacing = stateConfig?.nodeSpacing ?: 50
        val rankSpacing = stateConfig?.rankSpacing ?: 50
        val padding = stateConfig?.padding ?: 8

        val nodes = mutableListOf<LayoutNode>()
        val edges = mutableListOf<LayoutEdge>()

        // 构建状态节点
        for ((stateId, state) in states) {
            val shape = mapStateShape(state)
            val label = state.alias ?: when {
                state.descriptions.isNotEmpty() -> state.descriptions.joinToString("\\n")
                state.type == StateType.START || state.type == StateType.END -> ""
                else -> stateId
            }

            nodes.add(LayoutNode(
                id = stateId,
                label = label,
                shape = shape,
                isGroup = state.isComposite,
                parentId = state.parentId,
                dir = state.dir,
                padding = padding.toDouble(),
                rx = 5.0,
                ry = 5.0,
                domId = state.domId,
                cssClasses = buildCssClasses(state),
                cssStyles = state.cssStyles,
                look = config.look
            ))

            // 注释节点
            if (state.note != null) {
                val noteId = "${stateId}----note"
                nodes.add(LayoutNode(
                    id = noteId,
                    label = state.note!!.text,
                    shape = "note",
                    isGroup = false,
                    padding = 6.0,
                    domId = "state-${stateId}----note-${nodeCount++}",
                    cssClasses = "statediagram-note"
                ))
                // note 和 state 之间的连接边
                val noteEdgeStart = if (state.note!!.position.contains("left")) noteId else stateId
                val noteEdgeEnd = if (state.note!!.position.contains("left")) stateId else noteId
                edges.add(LayoutEdge(
                    id = "edgeNote-$stateId",
                    start = noteEdgeStart,
                    end = noteEdgeEnd,
                    arrowTypeStart = null,
                    arrowTypeEnd = null,
                    pattern = "dotted",
                    stroke = "normal",
                    thickness = "normal"
                ))
            }
        }

        // 构建转换边
        for ((idx, trans) in transitions.withIndex()) {
            edges.add(LayoutEdge(
                id = "edge$idx",
                start = trans.from,
                end = trans.to,
                label = trans.label.ifEmpty { null },
                arrowTypeEnd = "arrow_barb",
                arrowTypeStart = null,
                pattern = "solid",
                stroke = "normal",
                thickness = "normal",
                labelpos = "c"
            ))
        }

        return LayoutData(
            nodes = nodes,
            edges = edges,
            config = config,
            direction = direction,
            nodeSpacing = nodeSpacing,
            rankSpacing = rankSpacing,
            diagramPadding = padding,
            markers = listOf("barb")
        )
    }

    private fun mapStateShape(state: StateNode): String = when (state.type) {
        StateType.START -> "stateStart"
        StateType.END -> "stateEnd"
        StateType.FORK, StateType.JOIN -> "fork"
        StateType.CHOICE -> "diamond"
        StateType.DIVIDER -> "divider"
        StateType.DEFAULT -> when {
            state.isComposite -> "roundedRect"
            state.descriptions.size > 1 -> "rectWithTitle"
            state.descriptions.size == 1 -> "rect"
            else -> "rect"
        }
    }

    private fun buildCssClasses(state: StateNode): String {
        val classes = mutableListOf<String>()
        if (state.cssClasses.isNotEmpty()) classes.add(state.cssClasses)
        classes.add("statediagram-state")
        if (state.isComposite) {
            classes.add("statediagram-cluster")
        }
        return classes.joinToString(" ")
    }
}

// ════════════════════════════════════════════════════════
//  数据模型
// ════════════════════════════════════════════════════════

/**
 * 状态类型枚举 - 对标 mermaid-js StateType
 */
enum class StateType {
    DEFAULT,
    START,
    END,
    FORK,
    JOIN,
    CHOICE,
    DIVIDER
}

/**
 * 状态节点数据类
 */
data class StateNode(
    val id: String,
    var type: StateType = StateType.DEFAULT,
    var alias: String? = null,
    var descriptions: MutableList<String> = mutableListOf(),
    var isComposite: Boolean = false,
    var dir: String? = null,
    var note: StateNote? = null,
    var parentId: String? = null,
    var domId: String = "",
    var cssClasses: String = "",
    var cssStyles: MutableList<String> = mutableListOf(),
)

/**
 * 状态注释
 */
data class StateNote(
    val position: String = "right of",  // "left of" 或 "right of"
    val text: String = ""
)

/**
 * 状态转换（边）
 */
data class StateTransition(
    val from: String,
    val to: String,
    val label: String = ""
)

/**
 * 样式定义
 */
data class StateStyleDef(
    val id: String,
    val styles: List<String> = emptyList(),
)
