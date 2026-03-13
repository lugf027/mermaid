package io.lugf027.github.mermaid.core.diagram.sequence

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 时序图解析器 - 对标 mermaid-js sequenceDiagram.jison
 *
 * 手写递归下降解析器，支持以下语法:
 * - participant/actor 声明（8种类型）
 * - create/destroy 参与者
 * - 信号/消息（31种箭头类型）
 * - note left of/right of/over
 * - loop/alt/else/opt/par/and/critical/option/break/rect/end
 * - box ... end
 * - activate/deactivate
 * - autonumber
 * - title/accTitle/accDescr
 * - links/link/properties/details
 */
class SequenceParser : DiagramParser {

    private val log = Logger("SequenceParser")

    // ── 箭头正则 ─────────────────────────────────────
    // 顺序很重要：长模式优先匹配

    private data class ArrowDef(val pattern: String, val lineType: Int)

    private val arrowDefs = listOf(
        // 双向
        ArrowDef("<<-->>", LineType.BIDIRECTIONAL_DOTTED),
        ArrowDef("<<->>", LineType.BIDIRECTIONAL_SOLID),
        // 虚线 (-- 前缀)
        ArrowDef("-->>", LineType.DOTTED),
        ArrowDef("--x", LineType.DOTTED_CROSS),
        ArrowDef("--)", LineType.DOTTED_POINT),
        ArrowDef("-->", LineType.DOTTED_OPEN),
        // 实线
        ArrowDef("->>", LineType.SOLID),
        ArrowDef("-x", LineType.SOLID_CROSS),
        ArrowDef("-)", LineType.SOLID_POINT),
        ArrowDef("->", LineType.SOLID_OPEN),
    )

    // 合并为一个正则来快速检测是否包含箭头
    private val arrowPatterns = arrowDefs.map { Regex.escape(it.pattern) }
    private val RE_HAS_ARROW = Regex(arrowPatterns.joinToString("|"))

    override fun parse(text: String, db: DiagramDB) {
        val seqDb = db as? SequenceDb ?: throw IllegalArgumentException("Expected SequenceDb")
        seqDb.clear()

        val lines = text.lines()
        var i = 0

        // 跳过空行
        while (i < lines.size && lines[i].trim().isEmpty()) i++

        if (i >= lines.size) return

        // 第一行必须包含 "sequenceDiagram"
        val headerLine = lines[i].trim()
        if (!headerLine.startsWith("sequenceDiagram", ignoreCase = false)) {
            throw IllegalArgumentException("Sequence diagram must start with 'sequenceDiagram'")
        }
        i++

        // 逐行解析
        while (i < lines.size) {
            val rawLine = lines[i]
            val line = rawLine.trim()
            i++

            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("%%")) continue

            // 尝试解析各种语句
            // parseAccDescr 需要特殊处理（可能推进行号），所以不放在 when 中
            val accDescrResult = parseAccDescr(line, lines, i, seqDb)
            if (accDescrResult != null) {
                i = accDescrResult
                continue
            }

            when {
                parseParticipant(line, seqDb) -> continue
                parseBox(line, seqDb) -> continue
                parseNote(line, seqDb) -> continue
                parseActivation(line, seqDb) -> continue
                parseAutoNumber(line, seqDb) -> continue
                parseLoop(line, seqDb) -> continue
                parseAlt(line, seqDb) -> continue
                parseOpt(line, seqDb) -> continue
                parsePar(line, seqDb) -> continue
                parseCritical(line, seqDb) -> continue
                parseBreak(line, seqDb) -> continue
                parseRect(line, seqDb) -> continue
                parseEnd(line, seqDb) -> continue
                parseElse(line, seqDb) -> continue
                parseAnd(line, seqDb) -> continue
                parseOption(line, seqDb) -> continue
                parseTitle(line, seqDb) -> continue
                parseAccTitle(line, seqDb) -> continue
                parseCreateDestroy(line, seqDb) -> continue
                parseSignal(line, seqDb) -> continue
                else -> {
                    log.debug("Skipping unrecognized line: $line")
                }
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  语句解析方法
    // ════════════════════════════════════════════════════

    private val RE_PARTICIPANT = Regex(
        """^(participant|actor|boundary|control|entity|database|collections|queue)\s+(.+?)(?:\s+as\s+(.+))?\s*$""",
        RegexOption.IGNORE_CASE
    )

    private fun parseParticipant(line: String, db: SequenceDb): Boolean {
        val match = RE_PARTICIPANT.matchEntire(line) ?: return false
        val typeStr = match.groupValues[1].lowercase()
        val rawName = match.groupValues[2].trim()
        val alias = match.groupValues[3].trim().ifEmpty { null }

        val type = when (typeStr) {
            "actor" -> ParticipantType.ACTOR
            "boundary" -> ParticipantType.BOUNDARY
            "control" -> ParticipantType.CONTROL
            "entity" -> ParticipantType.ENTITY
            "database" -> ParticipantType.DATABASE
            "collections" -> ParticipantType.COLLECTIONS
            "queue" -> ParticipantType.QUEUE
            else -> ParticipantType.PARTICIPANT
        }

        val id = rawName.removeSurrounding("\"").removeSurrounding("'")
        val description = alias?.removeSurrounding("\"")?.removeSurrounding("'") ?: id

        db.addActor(id, id, description, type)
        return true
    }

    private fun parseBox(line: String, db: SequenceDb): Boolean {
        if (!line.startsWith("box", ignoreCase = true)) return false
        if (line.length > 3 && !line[3].isWhitespace() && line[3] != '\t') return false

        val afterBox = if (line.length > 3) line.substring(3).trim() else ""
        // 解析颜色和标题: box rgb(xxx) Title 或 box #color Title 或 box Title
        val colorRegex = Regex("""^(rgb\([^)]+\)|#[\da-fA-F]{3,8}|[a-zA-Z]+)\s*(.*)$""")
        val colorMatch = colorRegex.matchEntire(afterBox)

        if (colorMatch != null) {
            val fill = colorMatch.groupValues[1]
            val name = colorMatch.groupValues[2].trim()
            db.boxStart(name, fill)
        } else {
            db.boxStart(afterBox)
        }
        return true
    }

    private val RE_NOTE_SINGLE = Regex(
        """^note\s+(left\s+of|right\s+of|over)\s+([^:,]+?)(?:\s*,\s*([^:]+?))?\s*:\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private fun parseNote(line: String, db: SequenceDb): Boolean {
        val match = RE_NOTE_SINGLE.matchEntire(line) ?: return false

        val placementStr = match.groupValues[1].trim().lowercase()
        val actor1 = match.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
        val actor2 = match.groupValues[3].trim().removeSurrounding("\"").removeSurrounding("'").ifEmpty { null }
        val message = match.groupValues[4].trim()

        val placement = when {
            placementStr.startsWith("left") -> Placement.LEFTOF
            placementStr.startsWith("right") -> Placement.RIGHTOF
            else -> Placement.OVER
        }

        if (placement == Placement.OVER && actor2 != null) {
            db.addNoteOver(actor1, actor2, message)
        } else if (placement == Placement.OVER) {
            db.addNote(actor1, Placement.OVER, message)
        } else {
            db.addNote(actor1, placement, message)
        }
        return true
    }

    private fun parseActivation(line: String, db: SequenceDb): Boolean {
        val activateMatch = Regex("""^activate\s+(.+)\s*$""", RegexOption.IGNORE_CASE).matchEntire(line)
        if (activateMatch != null) {
            val actorId = activateMatch.groupValues[1].trim()
            db.activeStart(actorId)
            return true
        }

        val deactivateMatch = Regex("""^deactivate\s+(.+)\s*$""", RegexOption.IGNORE_CASE).matchEntire(line)
        if (deactivateMatch != null) {
            val actorId = deactivateMatch.groupValues[1].trim()
            db.activeEnd(actorId)
            return true
        }
        return false
    }

    private fun parseAutoNumber(line: String, db: SequenceDb): Boolean {
        if (!line.startsWith("autonumber", ignoreCase = true)) return false

        val afterAuto = line.substring(10).trim()
        if (afterAuto.equals("off", ignoreCase = true)) {
            db.disableSequenceNumbers()
            return true
        }

        val nums = Regex("""(\d+)""").findAll(afterAuto).map { it.value.toInt() }.toList()
        val start = nums.getOrElse(0) { 1 }
        val step = nums.getOrElse(1) { 1 }
        db.enableSequenceNumbers(start, step)
        return true
    }

    private fun parseLoop(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^loop\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.LOOP_START, match.groupValues[1].trim())
        return true
    }

    private fun parseAlt(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^alt\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.ALT_START, match.groupValues[1].trim())
        return true
    }

    private fun parseElse(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^else\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.ALT_ELSE, match.groupValues[1].trim())
        return true
    }

    private fun parseOpt(line: String, db: SequenceDb): Boolean {
        // 确保匹配 "opt" 后面是空白或行尾，不匹配 "option"
        val match = Regex("""^opt(?:\s+(.*))?$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        if (line.startsWith("option", ignoreCase = true)) return false  // 让 parseOption 处理
        db.addControlMessage(LineType.OPT_START, match.groupValues[1].trim())
        return true
    }

    private fun parsePar(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^par\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.PAR_START, match.groupValues[1].trim())
        return true
    }

    private fun parseAnd(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^and\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.PAR_AND, match.groupValues[1].trim())
        return true
    }

    private fun parseCritical(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^critical\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.CRITICAL_START, match.groupValues[1].trim())
        return true
    }

    private fun parseOption(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^option\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.CRITICAL_OPTION, match.groupValues[1].trim())
        return true
    }

    private fun parseBreak(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^break\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.BREAK_START, match.groupValues[1].trim())
        return true
    }

    private fun parseRect(line: String, db: SequenceDb): Boolean {
        val match = Regex("""^rect\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.addControlMessage(LineType.RECT_START, match.groupValues[1].trim())
        return true
    }

    private fun parseEnd(line: String, db: SequenceDb): Boolean {
        if (!line.equals("end", ignoreCase = true)) return false

        // end 可以结束 box 或 loop/alt/par 等
        // 通过查看最近的未闭合块来决定
        // 简单实现：根据消息历史中最近的 START 类型决定 END 类型
        val lastStartType = db.getMessages().lastOrNull { msg ->
            msg.type in setOf(
                LineType.LOOP_START, LineType.ALT_START, LineType.OPT_START,
                LineType.PAR_START, LineType.CRITICAL_START, LineType.BREAK_START,
                LineType.RECT_START, LineType.PAR_OVER_START
            )
        }?.type

        // 同时处理 box end
        if (db.getBoxes().isNotEmpty()) {
            // 可能是 box end
            db.boxEnd()
        }

        val endType = when (lastStartType) {
            LineType.LOOP_START -> LineType.LOOP_END
            LineType.ALT_START -> LineType.ALT_END
            LineType.OPT_START -> LineType.OPT_END
            LineType.PAR_START -> LineType.PAR_END
            LineType.CRITICAL_START -> LineType.CRITICAL_END
            LineType.BREAK_START -> LineType.BREAK_END
            LineType.RECT_START -> LineType.RECT_END
            else -> LineType.LOOP_END // fallback
        }

        db.addControlMessage(endType)
        return true
    }

    private fun parseTitle(line: String, db: SequenceDb): Boolean {
        if (!line.startsWith("title", ignoreCase = true)) return false
        if (line.length > 5 && !line[5].isWhitespace() && line[5] != ':') return false
        val text = line.substring(5).trim().removePrefix(":").trim()
        if (text.isNotEmpty()) {
            db.setDiagramTitle(text)
        }
        return true
    }

    private fun parseAccTitle(line: String, db: SequenceDb): Boolean {
        if (!line.startsWith("accTitle", ignoreCase = true)) return false
        val colonIdx = line.indexOf(':')
        if (colonIdx >= 0) {
            db.setAccTitle(line.substring(colonIdx + 1).trim())
        }
        return true
    }

    private fun parseAccDescr(line: String, lines: List<String>, currentI: Int, db: SequenceDb): Int? {
        if (!line.startsWith("accDescr", ignoreCase = true)) return null

        // 多行描述
        if (line.contains("{")) {
            val sb = StringBuilder()
            var j = currentI
            while (j < lines.size) {
                val descLine = lines[j]
                j++
                if (descLine.trim().contains("}")) break
                sb.appendLine(descLine)
            }
            db.setAccDescription(sb.toString().trim())
            return j
        }

        // 单行
        val colonIdx = line.indexOf(':')
        if (colonIdx >= 0) {
            db.setAccDescription(line.substring(colonIdx + 1).trim())
        }
        return currentI
    }

    private fun parseCreateDestroy(line: String, db: SequenceDb): Boolean {
        val createMatch = Regex(
            """^create\s+(participant|actor|boundary|control|entity|database|collections|queue)\s+(.+?)(?:\s+as\s+(.+))?\s*$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(line)
        if (createMatch != null) {
            val typeStr = createMatch.groupValues[1].lowercase()
            val id = createMatch.groupValues[2].trim().removeSurrounding("\"")
            val alias = createMatch.groupValues[3].trim().ifEmpty { null }
            val type = parseParticipantType(typeStr)
            val description = alias?.removeSurrounding("\"") ?: id
            db.addActor(id, id, description, type)
            db.markCreate(id)
            return true
        }

        val destroyMatch = Regex("""^destroy\s+(.+)\s*$""", RegexOption.IGNORE_CASE).matchEntire(line)
        if (destroyMatch != null) {
            val actorId = destroyMatch.groupValues[1].trim()
            db.markDestroy(actorId)
            return true
        }
        return false
    }

    /**
     * 解析信号/消息行: Actor1 ->> Actor2: Message text
     */
    private fun parseSignal(line: String, db: SequenceDb): Boolean {
        // 查找箭头
        var arrowIdx = -1
        var arrowLen = 0
        var lineType = LineType.SOLID

        for (def in arrowDefs) {
            val idx = line.indexOf(def.pattern)
            if (idx >= 0) {
                arrowIdx = idx
                arrowLen = def.pattern.length
                lineType = def.lineType
                break
            }
        }

        if (arrowIdx < 0) return false

        val fromPart = line.substring(0, arrowIdx).trim()
        val afterArrow = line.substring(arrowIdx + arrowLen)

        // 检查激活修饰符 (+/-) 在箭头后面
        var activate = false
        var deactivate = false
        var remaining = afterArrow.trimStart()

        if (remaining.startsWith("+")) {
            activate = true
            remaining = remaining.substring(1).trimStart()
        } else if (remaining.startsWith("-")) {
            deactivate = true
            remaining = remaining.substring(1).trimStart()
        }

        // 解析目标和消息: "Target: message" 或 "Target :message"
        val colonIdx = remaining.indexOf(':')
        val toPart: String
        val message: String

        if (colonIdx >= 0) {
            toPart = remaining.substring(0, colonIdx).trim()
            message = remaining.substring(colonIdx + 1).trim()
        } else {
            toPart = remaining.trim()
            message = ""
        }

        val from = fromPart.removeSurrounding("\"").removeSurrounding("'")
        val to = toPart.removeSurrounding("\"").removeSurrounding("'")

        if (from.isEmpty() || to.isEmpty()) return false

        db.addSignal(from, to, message, lineType, activate)

        // 处理去激活
        if (deactivate) {
            db.activeEnd(from)
        }

        return true
    }

    private fun parseParticipantType(typeStr: String): ParticipantType {
        return when (typeStr.lowercase()) {
            "actor" -> ParticipantType.ACTOR
            "boundary" -> ParticipantType.BOUNDARY
            "control" -> ParticipantType.CONTROL
            "entity" -> ParticipantType.ENTITY
            "database" -> ParticipantType.DATABASE
            "collections" -> ParticipantType.COLLECTIONS
            "queue" -> ParticipantType.QUEUE
            else -> ParticipantType.PARTICIPANT
        }
    }
}
