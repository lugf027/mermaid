package io.lugf027.github.mermaid.core.diagram.stateDiagram

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 状态图解析器 - 对标 mermaid-js stateDiagram.jison
 *
 * 手写递归下降解析器，支持:
 * - 状态声明 (stateId, stateId : description)
 * - 转换 (stateA --> stateB : label)
 * - [*] 起始/终止状态
 * - 复合状态 (state stateId { ... })
 * - 特殊状态 (<<fork>>, <<join>>, <<choice>>, [[fork]], [[join]], [[choice]])
 * - 别名 (state "alias" as stateId)
 * - 注释 (note left/right of stateId : text)
 * - 并发分隔符 (--)
 * - 样式 (classDef, class, style, :::)
 * - direction
 */
class StateParser : DiagramParser {

    private val log = Logger("StateParser")

    // ── 正则 ─────────────────────────────────────
    private val RE_TRANSITION = Regex(
        """^(\[\*\]|[\w-]+)\s*-->\s*(\[\*\]|[\w-]+)(?:\s*:\s*(.+?))?\s*$"""
    )

    private val RE_STATE_ALIAS = Regex(
        """^state\s+"(.+?)"\s+as\s+([\w-]+)\s*$""", RegexOption.IGNORE_CASE
    )

    private val RE_STATE_TYPE = Regex(
        """^state\s+([\w-]+)\s+<<(fork|join|choice)>>\s*$""", RegexOption.IGNORE_CASE
    )

    private val RE_STATE_TYPE_BRACKET = Regex(
        """^state\s+([\w-]+)\s+\[\[(fork|join|choice)\]\]\s*$""", RegexOption.IGNORE_CASE
    )

    private val RE_STATE_COMPOSITE = Regex(
        """^state\s+([\w-]+)\s*\{\s*$"""
    )

    private val RE_STATE_COMPOSITE_ALIAS = Regex(
        """^state\s+"(.+?)"\s+as\s+([\w-]+)\s*\{\s*$""", RegexOption.IGNORE_CASE
    )

    private val RE_STATE_COMPOSITE_BARE = Regex(
        """^([\w-]+)\s*\{\s*$"""
    )

    private val RE_STATE_DESC = Regex(
        """^([\w-]+)\s*:\s*(.+?)\s*$"""
    )

    private val RE_NOTE = Regex(
        """^note\s+(left|right)\s+of\s+([\w-]+)\s*:\s*(.+?)\s*$""", RegexOption.IGNORE_CASE
    )

    private val RE_NOTE_MULTILINE = Regex(
        """^note\s+(left|right)\s+of\s+([\w-]+)\s*$""", RegexOption.IGNORE_CASE
    )

    private val RE_CLASSDEF = Regex(
        """^classDef\s+(\w+)\s+(.+)$""", RegexOption.IGNORE_CASE
    )

    private val RE_CLASS_APPLY = Regex(
        """^class\s+(.+?)\s+(\w+)\s*$""", RegexOption.IGNORE_CASE
    )

    private val RE_STYLE = Regex(
        """^style\s+(.+?)\s+(.+)$""", RegexOption.IGNORE_CASE
    )

    private val RE_STATE_STYLE = Regex(
        """^([\w-]+):::(\w+)\s*$"""
    )

    override fun parse(text: String, db: DiagramDB) {
        val stateDb = db as? StateDb ?: throw IllegalArgumentException("Expected StateDb")
        stateDb.clear()

        val lines = text.lines()
        var i = 0

        // 跳过空行
        while (i < lines.size && lines[i].trim().isEmpty()) i++
        if (i >= lines.size) return

        // 第一行必须包含 "stateDiagram"
        val headerLine = lines[i].trim()
        if (!headerLine.startsWith("stateDiagram", ignoreCase = false)) {
            throw IllegalArgumentException("State diagram must start with 'stateDiagram'")
        }
        i++

        i = parseBlock(lines, i, stateDb, null)
    }

    /**
     * 解析一个代码块（顶层或复合状态内部）
     */
    private fun parseBlock(lines: List<String>, startI: Int, db: StateDb, parentId: String?): Int {
        var i = startI

        while (i < lines.size) {
            val line = lines[i].trim()
            i++

            if (line.isEmpty() || line.startsWith("%%")) continue
            if (line == "}") return i  // 结束当前块

            // 跳过 "hide empty description"
            if (line.startsWith("hide ", ignoreCase = true)) continue

            // 优先解析需要多行的语法
            val noteResult = parseNote(line, lines, i, db)
            if (noteResult != null) { i = noteResult; continue }

            val compositeResult = parseCompositeState(line, lines, i, db, parentId)
            if (compositeResult != null) { i = compositeResult; continue }

            when {
                parseDirection(line, db) -> continue
                parseTitle(line, db) -> continue
                parseAccTitle(line, db) -> continue
                parseAccDescr(line, db) -> continue
                parseStateAlias(line, db, parentId) -> continue
                parseStateType(line, db) -> continue
                parseTransition(line, db, parentId) -> continue
                parseStateDesc(line, db, parentId) -> continue
                parseConcurrentDivider(line) -> continue
                parseClassDef(line, db) -> continue
                parseClassApply(line, db) -> continue
                parseStyleApply(line, db) -> continue
                parseStateStyle(line, db) -> continue
                parseSimpleState(line, db, parentId) -> continue
                else -> {
                    log.debug("Skipping unrecognized line: $line")
                }
            }
        }

        return i
    }

    // ════════════════════════════════════════════════════
    //  解析方法
    // ════════════════════════════════════════════════════

    private fun parseDirection(line: String, db: StateDb): Boolean {
        val match = Regex("""^direction\s+(TB|BT|LR|RL)\s*$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.setDirection(match.groupValues[1].uppercase())
        return true
    }

    private fun parseTitle(line: String, db: StateDb): Boolean {
        if (!line.startsWith("title", ignoreCase = true)) return false
        if (line.length > 5 && !line[5].isWhitespace() && line[5] != ':') return false
        val text = line.substring(5).trim().removePrefix(":").trim()
        if (text.isNotEmpty()) db.setDiagramTitle(text)
        return true
    }

    private fun parseAccTitle(line: String, db: StateDb): Boolean {
        if (!line.startsWith("accTitle", ignoreCase = true)) return false
        val colonIdx = line.indexOf(':')
        if (colonIdx >= 0) db.setAccTitle(line.substring(colonIdx + 1).trim())
        return true
    }

    private fun parseAccDescr(line: String, db: StateDb): Boolean {
        if (!line.startsWith("accDescr", ignoreCase = true)) return false
        val colonIdx = line.indexOf(':')
        if (colonIdx >= 0) db.setAccDescription(line.substring(colonIdx + 1).trim())
        return true
    }

    /**
     * 解析注释（单行和多行）
     */
    private fun parseNote(line: String, lines: List<String>, currentI: Int, db: StateDb): Int? {
        // 单行 note
        val singleMatch = RE_NOTE.matchEntire(line)
        if (singleMatch != null) {
            val position = "${singleMatch.groupValues[1]} of"
            val stateId = singleMatch.groupValues[2]
            val text = singleMatch.groupValues[3]
            db.addNote(stateId, position, text)
            return currentI
        }

        // 多行 note
        val multiMatch = RE_NOTE_MULTILINE.matchEntire(line)
        if (multiMatch != null) {
            val position = "${multiMatch.groupValues[1]} of"
            val stateId = multiMatch.groupValues[2]
            val textLines = mutableListOf<String>()
            var j = currentI
            while (j < lines.size) {
                val noteLine = lines[j].trim()
                j++
                if (noteLine.equals("end note", ignoreCase = true)) break
                textLines.add(noteLine)
            }
            db.addNote(stateId, position, textLines.joinToString("\\n"))
            return j
        }

        return null
    }

    /**
     * 解析复合状态
     */
    private fun parseCompositeState(line: String, lines: List<String>, currentI: Int, db: StateDb, parentId: String?): Int? {
        // state "alias" as stateId {
        val aliasMatch = RE_STATE_COMPOSITE_ALIAS.matchEntire(line)
        if (aliasMatch != null) {
            val alias = aliasMatch.groupValues[1]
            val stateId = aliasMatch.groupValues[2]
            db.addCompositeState(stateId, alias, parentId)
            return parseBlock(lines, currentI, db, stateId)
        }

        // state stateId {
        val stateMatch = RE_STATE_COMPOSITE.matchEntire(line)
        if (stateMatch != null) {
            val stateId = stateMatch.groupValues[1]
            db.addCompositeState(stateId, parentId = parentId)
            return parseBlock(lines, currentI, db, stateId)
        }

        // stateId {  (无 state 关键字)
        val bareMatch = RE_STATE_COMPOSITE_BARE.matchEntire(line)
        if (bareMatch != null) {
            val stateId = bareMatch.groupValues[1]
            // 排除关键字
            if (stateId.lowercase() in setOf("statediagram", "direction", "title", "note", "class", "classdef", "style")) return null
            db.addCompositeState(stateId, parentId = parentId)
            return parseBlock(lines, currentI, db, stateId)
        }

        return null
    }

    /**
     * 解析状态别名: state "alias" as stateId
     */
    private fun parseStateAlias(line: String, db: StateDb, parentId: String?): Boolean {
        val match = RE_STATE_ALIAS.matchEntire(line) ?: return false
        val alias = match.groupValues[1]
        val stateId = match.groupValues[2]
        db.addStateWithAlias(stateId, alias, parentId)
        return true
    }

    /**
     * 解析特殊状态类型: state stateId <<fork>> 或 state stateId [[fork]]
     */
    private fun parseStateType(line: String, db: StateDb): Boolean {
        val match = RE_STATE_TYPE.matchEntire(line) ?: RE_STATE_TYPE_BRACKET.matchEntire(line) ?: return false
        val stateId = match.groupValues[1]
        val typeStr = match.groupValues[2].lowercase()
        val type = when (typeStr) {
            "fork" -> StateType.FORK
            "join" -> StateType.JOIN
            "choice" -> StateType.CHOICE
            else -> StateType.DEFAULT
        }
        db.setStateType(stateId, type)
        return true
    }

    /**
     * 解析转换: stateA --> stateB : label
     */
    private fun parseTransition(line: String, db: StateDb, parentId: String?): Boolean {
        val match = RE_TRANSITION.matchEntire(line) ?: return false
        val from = match.groupValues[1]
        val to = match.groupValues[2]
        val label = match.groupValues[3].trim()
        db.addTransition(from, to, label, parentId)
        return true
    }

    /**
     * 解析状态描述: stateId : description
     */
    private fun parseStateDesc(line: String, db: StateDb, parentId: String?): Boolean {
        val match = RE_STATE_DESC.matchEntire(line) ?: return false
        val stateId = match.groupValues[1]
        val desc = match.groupValues[2]
        // 排除关键字
        if (stateId.lowercase() in setOf("note", "state", "direction", "title", "class", "classdef", "style", "hide")) return false
        db.addState(stateId, description = desc, parentId = parentId)
        return true
    }

    /**
     * 解析并发分隔符: --
     */
    private fun parseConcurrentDivider(line: String): Boolean {
        return line == "--"
    }

    /**
     * 解析 classDef
     */
    private fun parseClassDef(line: String, db: StateDb): Boolean {
        val match = RE_CLASSDEF.matchEntire(line) ?: return false
        val styleName = match.groupValues[1]
        val attrs = match.groupValues[2].split(",").map { it.trim() }
        db.addStyleClass(styleName, attrs)
        return true
    }

    /**
     * 解析 class 应用: class stateId1,stateId2 styleName
     */
    private fun parseClassApply(line: String, db: StateDb): Boolean {
        val match = RE_CLASS_APPLY.matchEntire(line) ?: return false
        val stateIds = match.groupValues[1].split(",").map { it.trim() }
        val styleId = match.groupValues[2]
        db.applyStyleClass(stateIds, styleId)
        return true
    }

    /**
     * 解析 style 应用
     */
    private fun parseStyleApply(line: String, db: StateDb): Boolean {
        val match = RE_STYLE.matchEntire(line) ?: return false
        val stateIds = match.groupValues[1].split(",").map { it.trim() }
        val styles = match.groupValues[2].split(",").map { it.trim() }
        db.applyInlineStyle(stateIds, styles)
        return true
    }

    /**
     * 解析 stateId:::styleName
     */
    private fun parseStateStyle(line: String, db: StateDb): Boolean {
        val match = RE_STATE_STYLE.matchEntire(line) ?: return false
        val stateId = match.groupValues[1]
        val styleId = match.groupValues[2]
        db.addState(stateId)
        db.applyStyleClass(listOf(stateId), styleId)
        return true
    }

    /**
     * 解析简单状态声明（单独一行的 stateId）
     */
    private fun parseSimpleState(line: String, db: StateDb, parentId: String?): Boolean {
        val match = Regex("""^([\w-]+)\s*$""").matchEntire(line) ?: return false
        val stateId = match.groupValues[1]
        // 排除关键字
        if (stateId.lowercase() in setOf(
                "statediagram", "statediagram-v2", "direction", "title", "acctitle",
                "accdescr", "note", "state", "class", "classdef", "style", "hide"
            )) return false
        db.addState(stateId, parentId = parentId)
        return true
    }
}
