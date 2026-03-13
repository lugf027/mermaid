package io.lugf027.github.mermaid.core.diagram.sankey

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 桑基图解析器 - 对标 mermaid-js sankey.jison
 *
 * 语法（CSV 格式）：
 *   sankey-beta
 *
 *   source,target,value
 *   source,target,value
 *   ...
 *
 * 每行是一条链接，格式为: source,target,value
 * 节点名称可以用双引号括起来。
 * 空行和注释行（%%）会被跳过。
 */
class SankeyParser : DiagramParser {

    private val RE_START = Regex("^\\s*sankey(-beta)?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val sdb = db as SankeyDb
        sdb.clear()

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
                sdb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                sdb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            // 解析 CSV 行: source,target,value
            val parts = parseCsvLine(trimmed)
            if (parts.size >= 3) {
                val source = parts[0].trim()
                val target = parts[1].trim()
                val value = parts[2].trim().toDoubleOrNull()
                if (source.isNotEmpty() && target.isNotEmpty() && value != null && value > 0) {
                    sdb.addLink(source, target, value)
                }
            }
        }
    }

    /**
     * 简单 CSV 解析，支持双引号括起的字段
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false

        for (ch in line) {
            when {
                ch == '"' -> inQuote = !inQuote
                ch == ',' && !inQuote -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString())
        return result
    }
}
