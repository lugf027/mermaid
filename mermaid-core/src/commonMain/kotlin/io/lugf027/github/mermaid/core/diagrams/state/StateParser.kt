package io.lugf027.github.mermaid.core.diagrams.state

import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * 状态图递归下降解析器。
 * 解析 stateDiagram-v2 语法，填充 StateDb。
 */
class StateParser(private val db: StateDb) : ParserDefinition {

    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0

        // 跳过 stateDiagram/stateDiagram-v2 关键字
        if (i < lines.size) {
            val first = lines[i].trim().lowercase()
            if (first.startsWith("statediagram")) i++
        }

        i = parseBlock(lines, i, null)
    }

    /**
     * 解析语句块。
     * @return 处理到的行号
     */
    private fun parseBlock(lines: List<String>, startIdx: Int, parentState: StateNode?): Int {
        var i = startIdx
        while (i < lines.size) {
            val line = lines[i].trim()
            i++

            if (line.isEmpty() || line.startsWith("%%")) continue

            val lower = line.lowercase()

            when {
                lower.startsWith("direction ") -> db.setDirection(line.substringAfter(" ").trim())
                lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())
                lower.startsWith("accdescr:") -> db.setAccDescription(line.substringAfter(":").trim())
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())

                // 关闭块
                line == "}" || lower == "end" -> return i

                // note
                lower.startsWith("note ") -> parseNote(line)

                // state 定义带花括号
                lower.startsWith("state ") && line.contains("{") -> {
                    val stateInfo = parseStateHeader(line)
                    val compositeState = db.addCompositeState(stateInfo.first, stateInfo.second)
                    i = parseBlock(lines, i, compositeState)
                }

                // state 带 <<fork>> / <<join>> / <<choice>> 类型
                lower.startsWith("state ") && (line.contains("<<") || line.contains("[[")) -> {
                    parseStateWithType(line)
                }

                // state "desc" as id
                lower.startsWith("state ") -> {
                    parseStateDef(line)
                }

                // 分隔符 --
                line == "--" -> {
                    db.addState("divider_${i}", type = StateType.DIVIDER)
                }

                // classDef/class
                lower.startsWith("classdef ") || lower.startsWith("class ") -> { /* 忽略样式 */ }

                // 转换或状态引用
                line.contains("-->") -> parseTransition(line)

                // 状态引用（仅 ID）
                else -> {
                    val id = line.split(" ", ":::").first().trim()
                    if (id.isNotEmpty() && id != "{" && id != "}") {
                        db.addState(id)
                    }
                }
            }
        }
        return i
    }

    private fun parseStateHeader(line: String): Pair<String, String?> {
        val rest = line.substringAfter("state ").substringBefore("{").trim()

        // state "description" as id
        val asMatch = Regex(""""([^"]+)"\s+as\s+(\S+)""").find(rest)
        if (asMatch != null) {
            return asMatch.groupValues[2] to asMatch.groupValues[1]
        }

        // state id
        return rest.split(" ").first().trim() to null
    }

    private fun parseStateDef(line: String) {
        val rest = line.substringAfter("state ").trim()

        // state "description" as id : extra
        val asMatch = Regex(""""([^"]+)"\s+as\s+(\S+)(\s*:\s*(.*))?""").find(rest)
        if (asMatch != null) {
            val desc = asMatch.groupValues[1]
            val id = asMatch.groupValues[2]
            val extra = asMatch.groupValues[4].trim()
            db.addState(id, if (extra.isNotEmpty()) "$desc\n$extra" else desc)
            return
        }

        // state id : description
        val colonIdx = rest.indexOf(':')
        if (colonIdx >= 0) {
            val id = rest.substring(0, colonIdx).trim()
            val desc = rest.substring(colonIdx + 1).trim()
            db.addState(id, desc)
        } else {
            val id = rest.split(" ", ":::").first().trim()
            db.addState(id)
        }
    }

    private fun parseStateWithType(line: String) {
        val rest = line.substringAfter("state ").trim()
        val id = rest.split(" ", "<<", "[[").first().trim()

        val type = when {
            rest.contains("<<fork>>") || rest.contains("[[fork]]") -> StateType.FORK
            rest.contains("<<join>>") || rest.contains("[[join]]") -> StateType.JOIN
            rest.contains("<<choice>>") || rest.contains("[[choice]]") -> StateType.CHOICE
            else -> StateType.DEFAULT
        }

        db.addState(id)
        db.setStateType(id, type)
    }

    /** 大小写不敏感的 substringAfter */
    private fun String.substringAfterIC(delimiter: String): String {
        val idx = this.lowercase().indexOf(delimiter.lowercase())
        return if (idx < 0) this else this.substring(idx + delimiter.length)
    }

    private fun parseNote(line: String) {
        val rest = line.substringAfterIC("note ").trim()
        val placement: NotePosition
        val afterPlacement: String

        when {
            rest.startsWith("left of ", ignoreCase = true) -> {
                placement = NotePosition.LEFT_OF
                afterPlacement = rest.substringAfterIC("left of ")
            }
            rest.startsWith("right of ", ignoreCase = true) -> {
                placement = NotePosition.RIGHT_OF
                afterPlacement = rest.substringAfterIC("right of ")
            }
            else -> return
        }

        val colonIdx = afterPlacement.indexOf(':')
        if (colonIdx >= 0) {
            val stateId = afterPlacement.substring(0, colonIdx).trim()
            val text = afterPlacement.substring(colonIdx + 1).trim()
            db.addNote(stateId, text, placement)
        }
    }

    private fun parseTransition(line: String) {
        val parts = line.split("-->")
        if (parts.size >= 2) {
            val from = parts[0].trim().split(":::").first().trim()
            val rest = parts[1].trim()
            val colonIdx = rest.indexOf(':')
            val to: String
            val desc: String
            if (colonIdx >= 0) {
                to = rest.substring(0, colonIdx).trim().split(":::").first().trim()
                desc = rest.substring(colonIdx + 1).trim()
            } else {
                to = rest.split(":::").first().trim()
                desc = ""
            }
            db.addTransition(from, to, desc)
        }
    }
}
