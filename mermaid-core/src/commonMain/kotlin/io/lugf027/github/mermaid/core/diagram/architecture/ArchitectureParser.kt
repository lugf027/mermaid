package io.lugf027.github.mermaid.core.diagram.architecture

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 架构图解析器 - 对标 mermaid-js architecture.langium
 *
 * 语法：
 *   architecture-beta
 *   group id (icon) [title] in parentId
 *   service id (icon) [title] in parentId
 *   junction id in parentId
 *   lhsId:L --> R:rhsId
 *   lhsId:T -- B:rhsId
 *   lhsId:L - [title] -> R:rhsId
 */
class ArchitectureParser : DiagramParser {

    private val RE_START = Regex("^\\s*architecture(-beta)?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_GROUP = Regex("^\\s*group\\s+(\\w+)(?:\\s*\\(([^)]+)\\))?(?:\\s*\\[([^\\]]+)])?(?:\\s+in\\s+(\\w+))?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_SERVICE = Regex("^\\s*service\\s+(\\w+)(?:\\s*\\(([^)]+)\\))?(?:\\s*\\[([^\\]]+)])?(?:\\s+in\\s+(\\w+))?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_JUNCTION = Regex("^\\s*junction\\s+(\\w+)(?:\\s+in\\s+(\\w+))?\\s*$", RegexOption.IGNORE_CASE)
    // edge: lhsId {group}? :dir arrow -- arrow dir: rhsId {group}?
    // Simplified: id:DIR <--> DIR:id or id:DIR -- DIR:id or id:DIR - [title] -> DIR:id
    private val RE_EDGE = Regex("^\\s*(\\w+)(?:\\s*\\{(\\w+)})?\\s*:(\\w)\\s*(<)?\\s*-(?:\\s*\\[([^\\]]+)]\\s*)?-\\s*(>)?\\s*(\\w)\\s*:\\s*(\\w+)(?:\\s*\\{(\\w+)})?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val adb = db as ArchitectureDb
        adb.clear()

        val lines = text.lines()
        var started = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            if (!started) {
                if (RE_START.matches(trimmed)) started = true
                continue
            }

            RE_ACC_TITLE.find(trimmed)?.let {
                adb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                adb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_GROUP.find(trimmed)?.let { m ->
                val id = m.groupValues[1]
                val icon = m.groupValues[2]
                val title = m.groupValues[3]
                val inGroup = m.groupValues[4]
                adb.addGroup(id, icon, title, inGroup)
                return@let
            }?.also { continue }

            RE_SERVICE.find(trimmed)?.let { m ->
                val id = m.groupValues[1]
                val icon = m.groupValues[2]
                val title = m.groupValues[3]
                val inGroup = m.groupValues[4]
                adb.addService(id, icon = icon, title = title, inGroup = inGroup)
                return@let
            }?.also { continue }

            RE_JUNCTION.find(trimmed)?.let { m ->
                val id = m.groupValues[1]
                val inGroup = m.groupValues[2]
                adb.addJunction(id, inGroup)
                return@let
            }?.also { continue }

            RE_EDGE.find(trimmed)?.let { m ->
                val lhsId = m.groupValues[1]
                val lhsGroup = m.groupValues[2].isNotEmpty()
                val lhsDir = parseDirection(m.groupValues[3])
                val lhsInto = m.groupValues[4] == "<"
                val title = m.groupValues[5]
                val rhsInto = m.groupValues[6] == ">"
                val rhsDir = parseDirection(m.groupValues[7])
                val rhsId = m.groupValues[8]
                val rhsGroup = m.groupValues[9].isNotEmpty()

                adb.addEdge(ArchitectureDb.Edge(
                    lhsId = lhsId, lhsDir = lhsDir, lhsInto = lhsInto, lhsGroup = lhsGroup,
                    rhsId = rhsId, rhsDir = rhsDir, rhsInto = rhsInto, rhsGroup = rhsGroup,
                    title = title
                ))
                return@let
            }
        }
    }

    private fun parseDirection(s: String): ArchitectureDb.Direction = when (s.uppercase()) {
        "L" -> ArchitectureDb.Direction.L
        "R" -> ArchitectureDb.Direction.R
        "T" -> ArchitectureDb.Direction.T
        "B" -> ArchitectureDb.Direction.B
        else -> ArchitectureDb.Direction.R
    }
}
