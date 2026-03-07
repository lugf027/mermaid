package io.lugf027.github.mermaid.core.diagrams.state

import io.lugf027.github.mermaid.core.db.CommonDb
import io.lugf027.github.mermaid.core.types.*

/**
 * 状态图数据存储层。
 * 管理 states、transitions、notes。
 * 对应 mermaid-js stateDb.ts。
 */
class StateDb : CommonDb() {

    private val states = mutableMapOf<String, StateNode>()
    private val transitions = mutableListOf<StateTransition>()
    private var direction: String = "TB"
    private var startEndCount = 0

    /**
     * 添加/更新状态。
     */
    fun addState(id: String, description: String? = null, type: StateType = StateType.DEFAULT) {
        val resolvedId = resolveSpecialId(id)
        val existing = states[resolvedId]
        if (existing != null) {
            if (!description.isNullOrBlank()) existing.description = description
        } else {
            states[resolvedId] = StateNode(
                id = resolvedId,
                description = description ?: resolvedId,
                type = type,
            )
        }
    }

    /**
     * 添加带子文档的复合状态。
     */
    fun addCompositeState(id: String, description: String? = null): StateNode {
        addState(id, description)
        return states[id]!!
    }

    /**
     * 添加转换。
     */
    fun addTransition(from: String, to: String, description: String = "") {
        val resolvedFrom = resolveSpecialId(from)
        val resolvedTo = resolveSpecialId(to)

        addState(resolvedFrom, type = getTypeFromId(from))
        addState(resolvedTo, type = getTypeFromId(to))

        transitions.add(StateTransition(resolvedFrom, resolvedTo, description))
    }

    /**
     * 添加注释。
     */
    fun addNote(stateId: String, text: String, position: NotePosition = NotePosition.RIGHT_OF) {
        val state = states[stateId] ?: run { addState(stateId); states[stateId]!! }
        state.note = StateNote(position, text)
    }

    /**
     * 设置 fork/join/choice 类型。
     */
    fun setStateType(id: String, type: StateType) {
        addState(id)
        states[id]?.type = type
    }

    fun setDirection(dir: String) { direction = dir.uppercase() }
    fun getDirection(): String = direction
    fun getStates(): Map<String, StateNode> = states.toMap()
    fun getTransitions(): List<StateTransition> = transitions.toList()

    /**
     * 转换为通用 LayoutData。
     */
    fun getData(): LayoutData {
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        for ((_, state) in states) {
            val shape = when (state.type) {
                StateType.START -> ShapeId.STATE_START
                StateType.END -> ShapeId.STATE_END
                StateType.FORK, StateType.JOIN -> ShapeId.FORK_JOIN
                StateType.CHOICE -> ShapeId.CHOICE
                StateType.DIVIDER -> ShapeId.RECT
                StateType.DEFAULT -> {
                    if (state.doc.isNotEmpty()) ShapeId.ROUNDED_RECT
                    else ShapeId.ROUNDED_RECT
                }
            }

            val label = if (state.description.isNotEmpty() && state.description != state.id) {
                state.description
            } else {
                state.id
            }

            nodes.add(Node(
                id = state.id,
                label = label,
                shape = shape,
                isGroup = state.doc.isNotEmpty(),
                cssClasses = state.classes.toMutableList(),
            ))
        }

        for (t in transitions) {
            edges.add(Edge(
                start = t.from,
                end = t.to,
                label = t.description,
            ))
        }

        val dir = when (direction) { "BT" -> Direction.BT; "LR" -> Direction.LR; "RL" -> Direction.RL; else -> Direction.TB }
        return LayoutData(nodes = nodes, edges = edges, direction = dir)
    }

    /**
     * 处理 [*] 特殊状态。
     */
    private fun resolveSpecialId(id: String): String {
        if (id == "[*]") {
            // 根据上下文判断是 start 还是 end
            val resolvedId = "start_end_${startEndCount++}"
            return resolvedId
        }
        return id
    }

    private fun getTypeFromId(id: String): StateType {
        return if (id == "[*]") {
            // 第一次出现时假定为 start，后续由 transition 上下文决定
            StateType.START
        } else {
            StateType.DEFAULT
        }
    }

    override fun clear() {
        super.clear()
        states.clear()
        transitions.clear()
        direction = "TB"
        startEndCount = 0
    }
}
