package io.lugf027.github.mermaid.core.diagrams.state

/**
 * 状态图状态类型。
 */
enum class StateType(val id: String) {
    DEFAULT("default"),
    START("start"),
    END("end"),
    FORK("fork"),
    JOIN("join"),
    CHOICE("choice"),
    DIVIDER("divider"),
}

/**
 * 状态节点。
 */
data class StateNode(
    val id: String,
    var description: String = "",
    val descriptions: MutableList<String> = mutableListOf(),
    var type: StateType = StateType.DEFAULT,
    val doc: MutableList<StateStmt> = mutableListOf(),   // 复合状态的子文档
    var note: StateNote? = null,
    val classes: MutableList<String> = mutableListOf(),
)

/**
 * 状态图注释。
 */
data class StateNote(
    val position: NotePosition,
    val text: String,
)

enum class NotePosition { LEFT_OF, RIGHT_OF }

/**
 * 状态转换。
 */
data class StateTransition(
    val from: String,
    val to: String,
    var description: String = "",
)

/**
 * 解析后的语句类型。
 */
sealed class StateStmt {
    data class StateDef(val state: StateNode) : StateStmt()
    data class Transition(val transition: StateTransition) : StateStmt()
    data class DirectionDef(val dir: String) : StateStmt()
}
