package io.lugf027.github.mermaid.core.diagrams.sequence

/**
 * 时序图参与者类型。
 */
enum class ActorType(val id: String) {
    PARTICIPANT("participant"),
    ACTOR("actor"),
}

/**
 * 时序图参与者数据。
 */
data class SequenceActor(
    val name: String,
    var description: String = name,
    var type: ActorType = ActorType.PARTICIPANT,
    var wrap: Boolean = false,
)

/**
 * 消息行类型。
 * 对应 mermaid-js sequenceDb LINETYPE。
 */
enum class LineType(val value: Int) {
    SOLID(0),           // ->>
    DOTTED(1),          // -->>
    NOTE(2),            // note
    SOLID_CROSS(3),     // -x
    DOTTED_CROSS(4),    // --x
    SOLID_OPEN(5),      // ->
    DOTTED_OPEN(6),     // -->
    LOOP_START(10),
    LOOP_END(11),
    ALT_START(12),
    ALT_ELSE(13),
    ALT_END(14),
    OPT_START(15),
    OPT_END(16),
    ACTIVE_START(17),
    ACTIVE_END(18),
    PAR_START(19),
    PAR_AND(20),
    PAR_END(21),
    RECT_START(22),
    RECT_END(23),
    SOLID_POINT(24),    // -)
    DOTTED_POINT(25),   // --)
    AUTONUMBER(26),
    CRITICAL_START(27),
    CRITICAL_OPTION(28),
    CRITICAL_END(29),
    BREAK_START(30),
    BREAK_END(31),
}

/**
 * 时序图消息。
 */
data class SequenceMessage(
    var from: String? = null,
    var to: String? = null,
    var message: String = "",
    var type: LineType = LineType.SOLID,
    var activate: Boolean = false,
    var wrap: Boolean = false,
)

/**
 * 时序图注释。
 */
data class SequenceNote(
    val actor: String,
    val message: String,
    val placement: NotePlacement = NotePlacement.RIGHT_OF,
)

/**
 * 注释位置。
 */
enum class NotePlacement {
    LEFT_OF, RIGHT_OF, OVER
}
