package io.lugf027.github.mermaid.core.diagrams.sequence

import io.lugf027.github.mermaid.core.db.CommonDb

/**
 * 时序图数据存储层。
 * 管理 actors（参与者）、messages（消息）、notes（注释）。
 * 对应 mermaid-js sequenceDb.ts。
 */
class SequenceDb : CommonDb() {

    private val actors = mutableMapOf<String, SequenceActor>()
    private val actorOrder = mutableListOf<String>()
    private val messages = mutableListOf<SequenceMessage>()
    private val notes = mutableListOf<SequenceNote>()
    private var autoNumber = false

    /**
     * 添加参与者。
     */
    fun addActor(name: String, description: String? = null, type: ActorType = ActorType.PARTICIPANT) {
        if (name !in actors) {
            actorOrder.add(name)
        }
        actors[name] = SequenceActor(
            name = name,
            description = description ?: name,
            type = type,
        )
    }

    fun getActors(): Map<String, SequenceActor> = actors.toMap()
    fun getActorKeys(): List<String> = actorOrder.toList()

    /**
     * 添加消息（信号）。
     */
    fun addSignal(from: String, to: String, message: String, type: LineType) {
        // 确保参与者存在
        if (from !in actors) addActor(from)
        if (to !in actors) addActor(to)

        messages.add(SequenceMessage(
            from = from,
            to = to,
            message = message,
            type = type,
        ))
    }

    /**
     * 添加结构块消息（loop/alt/opt/par/critical/break/rect）。
     */
    fun addBlockMessage(type: LineType, message: String = "") {
        messages.add(SequenceMessage(type = type, message = message))
    }

    /**
     * 添加注释。
     */
    fun addNote(actor: String, message: String, placement: NotePlacement = NotePlacement.RIGHT_OF) {
        if (actor !in actors) addActor(actor)
        notes.add(SequenceNote(actor, message, placement))
        messages.add(SequenceMessage(type = LineType.NOTE, from = actor, message = message))
    }

    /**
     * 设置激活。
     */
    fun addActivation(actor: String, activate: Boolean) {
        val type = if (activate) LineType.ACTIVE_START else LineType.ACTIVE_END
        messages.add(SequenceMessage(type = type, from = actor))
    }

    /**
     * 设置自动编号。
     */
    fun enableAutoNumber() { autoNumber = true }
    fun isAutoNumber(): Boolean = autoNumber

    fun getMessages(): List<SequenceMessage> = messages.toList()
    fun getNotes(): List<SequenceNote> = notes.toList()

    override fun clear() {
        super.clear()
        actors.clear()
        actorOrder.clear()
        messages.clear()
        notes.clear()
        autoNumber = false
    }
}
