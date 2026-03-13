package io.lugf027.github.mermaid.core.diagram.sequence

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 时序图数据库 - 对标 mermaid-js sequenceDb.ts
 *
 * 管理参与者(Actor)、消息(Message)、笔记(Note)、分组框(Box)，
 * 以及激活/创建/销毁状态。
 */
class SequenceDb : DiagramDB {

    private val log = Logger("SequenceDb")

    // ── 状态 ──────────────────────────────────────────

    /** 所有参与者，按添加顺序维护 */
    private val actors: LinkedHashMap<String, Actor> = linkedMapOf()

    /** 动态创建的参与者 → 消息索引 */
    private val createdActors: LinkedHashMap<String, Int> = linkedMapOf()

    /** 动态销毁的参与者 → 消息索引 */
    private val destroyedActors: LinkedHashMap<String, Int> = linkedMapOf()

    /** 分组框列表 */
    private val boxes: MutableList<Box> = mutableListOf()

    /** 所有消息（含控制消息如 LOOP_START 等） */
    private val messages: MutableList<Message> = mutableListOf()

    /** 所有笔记 */
    private val notes: MutableList<Note> = mutableListOf()

    /** 是否启用序列号 */
    private var sequenceNumbersEnabled: Boolean = false

    /** 自动编号起始值 */
    private var autoNumberStart: Int = 1

    /** 自动编号步长 */
    private var autoNumberStep: Int = 1

    /** 全局 wrap 设置 */
    private var wrapEnabled: Boolean = false

    /** 当前正在构建的 box */
    private var currentBox: Box? = null

    /** 上一个添加的参与者 ID（用于维护链表） */
    private var prevActor: String? = null

    /** 最后创建的参与者（用于 create 验证） */
    private var lastCreated: Actor? = null

    /** 最后销毁的参与者（用于 destroy 验证） */
    private var lastDestroyed: Actor? = null

    /** 是否隐藏未使用的参与者 */
    private var hideUnusedParticipants: Boolean = false

    // ── DiagramDB 基础接口 ────────────────────────────

    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescription: String = ""
    private var direction: String = "TB"

    override fun clear() {
        actors.clear()
        createdActors.clear()
        destroyedActors.clear()
        boxes.clear()
        messages.clear()
        notes.clear()
        sequenceNumbersEnabled = false
        autoNumberStart = 1
        autoNumberStep = 1
        wrapEnabled = false
        currentBox = null
        prevActor = null
        lastCreated = null
        lastDestroyed = null
        hideUnusedParticipants = false
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

    // ── 参与者管理 ────────────────────────────────────

    /**
     * 添加参与者
     */
    fun addActor(
        id: String,
        name: String,
        description: String,
        type: ParticipantType = ParticipantType.PARTICIPANT
    ) {
        // 不重复添加
        if (actors.containsKey(id)) {
            // 更新描述如果之前只是隐式创建
            val existing = actors[id]!!
            if (existing.description == existing.name && description != name) {
                existing.description = description
            }
            return
        }

        val actor = Actor(
            name = name,
            description = description,
            wrap = wrapEnabled,
            type = type,
            prevActor = prevActor
        )

        // 维护链表
        prevActor?.let { prevId ->
            actors[prevId]?.nextActor = id
        }
        prevActor = id

        // 如果当前在 box 内，关联到 box
        currentBox?.let { box ->
            actor.box = box
            box.actorKeys.add(id)
        }

        actors[id] = actor
        log.debug("Added actor: $id ($description), type=${type.name}")
    }

    /**
     * 获取所有参与者
     */
    fun getActors(): Map<String, Actor> = actors

    /**
     * 获取有序参与者 key 列表
     */
    fun getActorKeys(): List<String> = actors.keys.toList()

    /**
     * 确保参与者存在（信号引用时自动创建）
     */
    fun ensureActor(id: String) {
        if (!actors.containsKey(id)) {
            addActor(id, id, id)
        }
    }

    // ── 消息/信号管理 ─────────────────────────────────

    /**
     * 添加信号/消息
     */
    fun addSignal(
        from: String?,
        to: String?,
        message: String,
        type: Int,
        activate: Boolean = false
    ) {
        // 确保参与者存在
        from?.let { ensureActor(it) }
        to?.let { ensureActor(it) }

        val msg = Message(
            from = from,
            to = to,
            message = parseMessageText(message),
            type = type,
            wrap = wrapEnabled,
            activate = activate
        )
        messages.add(msg)

        // 如果有 create，标记
        lastCreated?.let { created ->
            if (to != null && actors[to] == created) {
                createdActors[to] = messages.size - 1
                lastCreated = null
            }
        }

        // 如果有 destroy，标记
        lastDestroyed?.let { destroyed ->
            val destroyTarget = from ?: to
            if (destroyTarget != null && actors[destroyTarget] == destroyed) {
                destroyedActors[destroyTarget] = messages.size - 1
                lastDestroyed = null
            }
        }
    }

    /**
     * 添加控制消息（loop/alt/par 等开始/结束标记）
     */
    fun addControlMessage(type: Int, message: String = "") {
        messages.add(Message(
            from = null,
            to = null,
            message = message,
            type = type,
            wrap = wrapEnabled
        ))
    }

    /**
     * 获取所有消息
     */
    fun getMessages(): List<Message> = messages

    // ── 笔记管理 ──────────────────────────────────────

    /**
     * 添加笔记
     */
    fun addNote(
        actorId: String,
        placement: Int,
        message: String
    ) {
        ensureActor(actorId)
        notes.add(Note(
            actor = actorId,
            placement = placement,
            message = parseMessageText(message),
            wrap = wrapEnabled
        ))
        // 笔记也作为消息插入（用于渲染排序）
        messages.add(Message(
            from = actorId,
            to = actorId,
            message = parseMessageText(message),
            type = LineType.NOTE,
            wrap = wrapEnabled,
            placement = placement
        ))
    }

    /**
     * 添加 over 类型的笔记（跨越多个 actor）
     */
    fun addNoteOver(
        actor1: String,
        actor2: String,
        message: String
    ) {
        ensureActor(actor1)
        ensureActor(actor2)
        notes.add(Note(
            actor = actor1,
            actor2 = actor2,
            placement = Placement.OVER,
            message = parseMessageText(message),
            wrap = wrapEnabled
        ))
        messages.add(Message(
            from = actor1,
            to = actor2,
            message = parseMessageText(message),
            type = LineType.NOTE,
            wrap = wrapEnabled,
            placement = Placement.OVER
        ))
    }

    fun getNotes(): List<Note> = notes

    // ── Box 管理 ──────────────────────────────────────

    /**
     * 开始一个分组框
     */
    fun boxStart(name: String, fill: String = "transparent") {
        val box = Box(
            name = name.trim(),
            wrap = wrapEnabled,
            fill = fill,
            actorKeys = mutableListOf()
        )
        boxes.add(box)
        currentBox = box
    }

    /**
     * 结束当前分组框
     */
    fun boxEnd() {
        currentBox = null
    }

    fun getBoxes(): List<Box> = boxes

    // ── 激活管理 ──────────────────────────────────────

    fun activeStart(actorId: String) {
        ensureActor(actorId)
        addControlMessage(LineType.ACTIVE_START, actorId)
    }

    fun activeEnd(actorId: String) {
        ensureActor(actorId)
        addControlMessage(LineType.ACTIVE_END, actorId)
    }

    // ── 创建/销毁 ─────────────────────────────────────

    fun markCreate(actorId: String) {
        if (actors.containsKey(actorId)) {
            lastCreated = actors[actorId]
        }
    }

    fun markDestroy(actorId: String) {
        if (actors.containsKey(actorId)) {
            lastDestroyed = actors[actorId]
        }
    }

    fun getCreatedActors(): Map<String, Int> = createdActors
    fun getDestroyedActors(): Map<String, Int> = destroyedActors

    // ── 序列号 ────────────────────────────────────────

    fun enableSequenceNumbers(start: Int = 1, step: Int = 1) {
        sequenceNumbersEnabled = true
        autoNumberStart = start
        autoNumberStep = step
    }

    fun disableSequenceNumbers() {
        sequenceNumbersEnabled = false
    }

    fun isSequenceNumbersEnabled(): Boolean = sequenceNumbersEnabled
    fun getAutoNumberStart(): Int = autoNumberStart
    fun getAutoNumberStep(): Int = autoNumberStep

    // ── Wrap ──────────────────────────────────────────

    fun setWrap(enabled: Boolean) {
        wrapEnabled = enabled
    }

    fun isWrapEnabled(): Boolean = wrapEnabled

    // ── 隐藏未使用参与者 ──────────────────────────────

    fun setHideUnusedParticipants(hide: Boolean) {
        hideUnusedParticipants = hide
    }

    fun isHideUnusedParticipants(): Boolean = hideUnusedParticipants

    // ── 工具方法 ──────────────────────────────────────

    private fun parseMessageText(text: String): String {
        var result = text.trim()
        // 处理 <br> 和 <br/> 换行标记
        result = result.replace(Regex("<br\\s*/?>"), "\n")
        return result
    }
}

// ════════════════════════════════════════════════════════
//  数据模型
// ════════════════════════════════════════════════════════

/**
 * 参与者类型枚举 - 对标 mermaid-js PARTICIPANT_TYPE
 */
enum class ParticipantType {
    PARTICIPANT,  // 矩形框
    ACTOR,        // 火柴人
    BOUNDARY,     // 边界
    CONTROL,      // 控制
    ENTITY,       // 实体
    DATABASE,     // 圆柱体数据库
    COLLECTIONS,  // 堆叠矩形
    QUEUE         // 横向圆柱体
}

/**
 * 参与者数据类
 */
data class Actor(
    val name: String,
    var description: String,
    var wrap: Boolean = false,
    var type: ParticipantType = ParticipantType.PARTICIPANT,
    var prevActor: String? = null,
    var nextActor: String? = null,
    var box: Box? = null,
    var links: MutableMap<String, Any> = mutableMapOf(),
    var properties: MutableMap<String, Any> = mutableMapOf(),
    // 渲染时填充
    var actorCnt: Int? = null,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0,
    var margin: Double = 0.0,
    var stopy: Double = 0.0
)

/**
 * 消息数据类
 */
data class Message(
    val from: String? = null,
    val to: String? = null,
    val message: String = "",
    val type: Int = LineType.SOLID,
    val wrap: Boolean = false,
    val activate: Boolean = false,
    val placement: Int? = null
)

/**
 * 笔记数据类
 */
data class Note(
    val actor: String,
    val actor2: String? = null,
    val placement: Int = Placement.RIGHTOF,
    val message: String = "",
    val wrap: Boolean = false
)

/**
 * 分组框数据类
 */
data class Box(
    val name: String = "",
    val wrap: Boolean = false,
    val fill: String = "transparent",
    val actorKeys: MutableList<String> = mutableListOf()
)

/**
 * 线/箭头类型 - 对标 mermaid-js LINETYPE
 */
object LineType {
    const val SOLID = 0
    const val DOTTED = 1
    const val NOTE = 2
    const val SOLID_CROSS = 3
    const val DOTTED_CROSS = 4
    const val SOLID_OPEN = 5
    const val DOTTED_OPEN = 6
    const val LOOP_START = 10
    const val LOOP_END = 11
    const val ALT_START = 12
    const val ALT_ELSE = 13
    const val ALT_END = 14
    const val OPT_START = 15
    const val OPT_END = 16
    const val ACTIVE_START = 17
    const val ACTIVE_END = 18
    const val PAR_START = 19
    const val PAR_AND = 20
    const val PAR_END = 21
    const val RECT_START = 22
    const val RECT_END = 23
    const val SOLID_POINT = 24
    const val DOTTED_POINT = 25
    const val AUTONUMBER = 26
    const val CRITICAL_START = 27
    const val CRITICAL_OPTION = 28
    const val CRITICAL_END = 29
    const val BREAK_START = 30
    const val BREAK_END = 31
    const val PAR_OVER_START = 32
    const val BIDIRECTIONAL_SOLID = 33
    const val BIDIRECTIONAL_DOTTED = 34

    fun isMessage(type: Int): Boolean {
        return type in setOf(
            SOLID, DOTTED, SOLID_CROSS, DOTTED_CROSS, SOLID_OPEN, DOTTED_OPEN,
            SOLID_POINT, DOTTED_POINT, BIDIRECTIONAL_SOLID, BIDIRECTIONAL_DOTTED
        )
    }

    fun isDotted(type: Int): Boolean {
        return type in setOf(DOTTED, DOTTED_CROSS, DOTTED_OPEN, DOTTED_POINT, BIDIRECTIONAL_DOTTED)
    }
}

/**
 * 放置位置 - 对标 mermaid-js PLACEMENT
 */
object Placement {
    const val LEFTOF = 0
    const val RIGHTOF = 1
    const val OVER = 2
}
