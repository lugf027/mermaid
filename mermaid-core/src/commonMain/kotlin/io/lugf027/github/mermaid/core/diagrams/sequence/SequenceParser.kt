package io.lugf027.github.mermaid.core.diagrams.sequence

import io.lugf027.github.mermaid.core.types.ParserDefinition
import io.lugf027.github.mermaid.core.utils.Logger

/**
 * 时序图递归下降解析器。
 * 解析 sequenceDiagram 语法，填充 SequenceDb。
 *
 * 支持的语法：
 * - participant/actor 定义
 * - 消息: A ->> B : text, A -->> B : text 等
 * - activate/deactivate
 * - Note left of/right of/over
 * - loop/alt/else/opt/par/and/critical/option/break/rect...end
 * - autonumber, title, accTitle, accDescr
 */
class SequenceParser(private val db: SequenceDb) : ParserDefinition {

    private val tag = "SequenceParser"
    private var source = ""
    private var pos = 0

    override fun parse(input: String) {
        source = input
        pos = 0

        skipWhitespace()
        // 跳过 "sequenceDiagram" 关键字
        val firstLine = readToEndOfLine().trim()
        if (!firstLine.startsWith("sequenceDiagram", ignoreCase = true)) {
            Logger.warn(tag, "Expected 'sequenceDiagram', got '$firstLine'")
        }
        advance() // skip newline

        while (!isAtEnd()) {
            skipWhitespace()
            if (isAtEnd()) break
            parseLine()
        }
    }

    private fun parseLine() {
        val line = readToEndOfLine().trim()
        advance() // skip newline
        if (line.isEmpty() || line.startsWith("%%")) return

        val lower = line.lowercase()

        when {
            lower.startsWith("participant ") -> parseParticipant(line, ActorType.PARTICIPANT)
            lower.startsWith("actor ") -> parseParticipant(line, ActorType.ACTOR)
            lower.startsWith("activate ") -> db.addActivation(line.substringAfter(" ").trim(), true)
            lower.startsWith("deactivate ") -> db.addActivation(line.substringAfter(" ").trim(), false)
            lower.startsWith("note ") -> parseNote(line)
            lower.startsWith("loop ") -> db.addBlockMessage(LineType.LOOP_START, line.substringAfter(" ").trim())
            lower.startsWith("alt ") -> db.addBlockMessage(LineType.ALT_START, line.substringAfter(" ").trim())
            lower.startsWith("else") -> db.addBlockMessage(LineType.ALT_ELSE, line.substringAfter("else").trim())
            lower.startsWith("opt ") -> db.addBlockMessage(LineType.OPT_START, line.substringAfter(" ").trim())
            lower.startsWith("par ") -> db.addBlockMessage(LineType.PAR_START, line.substringAfter(" ").trim())
            lower.startsWith("and ") -> db.addBlockMessage(LineType.PAR_AND, line.substringAfter(" ").trim())
            lower.startsWith("critical ") -> db.addBlockMessage(LineType.CRITICAL_START, line.substringAfter(" ").trim())
            lower.startsWith("option ") -> db.addBlockMessage(LineType.CRITICAL_OPTION, line.substringAfter(" ").trim())
            lower.startsWith("break ") -> db.addBlockMessage(LineType.BREAK_START, line.substringAfter(" ").trim())
            lower.startsWith("rect ") -> db.addBlockMessage(LineType.RECT_START, line.substringAfter(" ").trim())
            lower == "end" -> parseEnd()
            lower == "autonumber" -> db.enableAutoNumber()
            lower.startsWith("autonumber") -> db.enableAutoNumber()
            lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
            lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())
            lower.startsWith("accdescr:") -> db.setAccDescription(line.substringAfter(":").trim())
            else -> tryParseSignal(line)
        }
    }

    private fun parseParticipant(line: String, type: ActorType) {
        val rest = line.substringAfter(" ").trim()
        // 支持 "Name as Alias" 语法
        val asIdx = rest.indexOf(" as ", ignoreCase = true)
        if (asIdx >= 0) {
            val name = rest.substring(0, asIdx).trim()
            val alias = rest.substring(asIdx + 4).trim()
            db.addActor(name, alias, type)
        } else {
            db.addActor(rest, null, type)
        }
    }

    /** 大小写不敏感的 substringAfter */
    private fun String.substringAfterIC(delimiter: String): String {
        val idx = this.lowercase().indexOf(delimiter.lowercase())
        return if (idx < 0) this else this.substring(idx + delimiter.length)
    }

    private fun parseNote(line: String) {
        val rest = line.substringAfterIC("note ").trim()
        val placement: NotePlacement
        val afterPlacement: String

        when {
            rest.startsWith("left of ", ignoreCase = true) -> {
                placement = NotePlacement.LEFT_OF
                afterPlacement = rest.substringAfterIC("left of ")
            }
            rest.startsWith("right of ", ignoreCase = true) -> {
                placement = NotePlacement.RIGHT_OF
                afterPlacement = rest.substringAfterIC("right of ")
            }
            rest.startsWith("over ", ignoreCase = true) -> {
                placement = NotePlacement.OVER
                afterPlacement = rest.substringAfterIC("over ")
            }
            else -> return
        }

        val colonIdx = afterPlacement.indexOf(':')
        if (colonIdx >= 0) {
            val actor = afterPlacement.substring(0, colonIdx).trim().split(",").first().trim()
            val message = afterPlacement.substring(colonIdx + 1).trim()
            db.addNote(actor, message, placement)
        }
    }

    /**
     * 解析 end 关键字，关闭最近打开的块。
     */
    private fun parseEnd() {
        // 根据 messages 栈找到最近的未关闭块
        val msgs = db.getMessages()
        val lastOpen = msgs.lastOrNull {
            it.type == LineType.LOOP_START || it.type == LineType.ALT_START ||
                it.type == LineType.OPT_START || it.type == LineType.PAR_START ||
                it.type == LineType.CRITICAL_START || it.type == LineType.BREAK_START ||
                it.type == LineType.RECT_START
        }
        val endType = when (lastOpen?.type) {
            LineType.LOOP_START -> LineType.LOOP_END
            LineType.ALT_START -> LineType.ALT_END
            LineType.OPT_START -> LineType.OPT_END
            LineType.PAR_START -> LineType.PAR_END
            LineType.CRITICAL_START -> LineType.CRITICAL_END
            LineType.BREAK_START -> LineType.BREAK_END
            LineType.RECT_START -> LineType.RECT_END
            else -> LineType.LOOP_END
        }
        db.addBlockMessage(endType)
    }

    /**
     * 尝试解析消息（信号）行。
     * 格式：A ->> B : text
     */
    private fun tryParseSignal(line: String) {
        // 正则匹配消息行
        val signalRegex = Regex(
            """^(\S+)\s*(--?>?>|--?>|->?>|->|-x|--x|-\)|--\)|<<--?>>|<<->>)\s*(\+|-)?(\S+)\s*(?::\s*(.*))?$"""
        )
        val match = signalRegex.find(line)
        if (match != null) {
            val from = match.groupValues[1].trim()
            val arrow = match.groupValues[2].trim()
            val activateFlag = match.groupValues[3].trim()
            val to = match.groupValues[4].trim()
            val text = match.groupValues[5].trim()

            val type = parseArrowType(arrow)
            db.addSignal(from, to, text, type)

            // 处理 +/- 激活标记
            if (activateFlag == "+") db.addActivation(to, true)
            if (activateFlag == "-") db.addActivation(from, false)
        }
    }

    private fun parseArrowType(arrow: String): LineType = when (arrow) {
        "->>" -> LineType.SOLID
        "-->>" -> LineType.DOTTED
        "->" -> LineType.SOLID_OPEN
        "-->" -> LineType.DOTTED_OPEN
        "-x" -> LineType.SOLID_CROSS
        "--x" -> LineType.DOTTED_CROSS
        "-)" -> LineType.SOLID_POINT
        "--)" -> LineType.DOTTED_POINT
        "<<->>" -> LineType.SOLID // bidirectional as solid
        "<<-->>" -> LineType.DOTTED // bidirectional as dotted
        else -> LineType.SOLID_OPEN
    }

    // 辅助方法
    private fun isAtEnd() = pos >= source.length
    private fun advance() { if (pos < source.length) pos++ }
    private fun skipWhitespace() { while (!isAtEnd() && source[pos].isWhitespace()) pos++ }
    private fun readToEndOfLine(): String {
        val start = pos
        while (!isAtEnd() && source[pos] != '\n') pos++
        return source.substring(start, pos)
    }
}
