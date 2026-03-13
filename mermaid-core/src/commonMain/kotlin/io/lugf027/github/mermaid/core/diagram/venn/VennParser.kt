package io.lugf027.github.mermaid.core.diagram.venn

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 韦恩图解析器 - 对标 mermaid-js venn.jison
 *
 * 语法：
 *   venn-beta
 *   set A [Set A] : 10
 *   set B [Set B] : 10
 *   union A, B [Overlap] : 2.5
 *   text A, B shared
 *   style A fill: #ff0000
 */
class VennParser : DiagramParser {

    private val RE_START = Regex("^\\s*venn(-beta)?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_SET = Regex("^\\s*set\\s+(\\w+)(?:\\s*\\[([^\\]]+)])?\\s*:\\s*(\\d+\\.?\\d*)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_UNION = Regex("^\\s*union\\s+(.+?)\\s*(?:\\[([^\\]]+)])?\\s*:\\s*(\\d+\\.?\\d*)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_TEXT = Regex("^\\s*text\\s+(.+?)\\s+(\\w+)(?:\\s+(.+))?$", RegexOption.IGNORE_CASE)
    private val RE_STYLE = Regex("^\\s*style\\s+(\\w+)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val vdb = db as VennDb
        vdb.clear()

        val lines = text.lines()
        var started = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            if (!started) {
                if (RE_START.matches(trimmed)) started = true
                continue
            }

            RE_TITLE.find(trimmed)?.let {
                vdb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_TITLE.find(trimmed)?.let {
                vdb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                vdb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_SET.find(trimmed)?.let { m ->
                val id = m.groupValues[1]
                val label = m.groupValues[2].ifEmpty { id }
                val size = m.groupValues[3].toDoubleOrNull() ?: 10.0
                vdb.addSet(id, label, size)
                return@let
            }?.also { continue }

            RE_UNION.find(trimmed)?.let { m ->
                val idsStr = m.groupValues[1]
                val label = m.groupValues[2]
                val size = m.groupValues[3].toDoubleOrNull() ?: 0.0
                val ids = idsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                vdb.addUnion(ids, label, size)
                return@let
            }?.also { continue }

            RE_TEXT.find(trimmed)?.let { m ->
                val setsStr = m.groupValues[1]
                val id = m.groupValues[2]
                val label = m.groupValues[3]
                val sets = setsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                vdb.addTextNode(sets, id, label)
                return@let
            }?.also { continue }

            RE_STYLE.find(trimmed)?.let { m ->
                val target = m.groupValues[1]
                val styleStr = m.groupValues[2]
                val styles = mutableMapOf<String, String>()
                for (part in styleStr.split(",")) {
                    val kv = part.trim().split(":", limit = 2)
                    if (kv.size == 2) {
                        styles[kv[0].trim()] = kv[1].trim()
                    }
                }
                vdb.addStyle(listOf(target), styles)
                return@let
            }
        }
    }
}
