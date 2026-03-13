package io.lugf027.github.mermaid.core.diagram.kanban

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 看板图解析器 - 对标 mermaid-js kanban.jison
 *
 * 语法（缩进感知）：
 *   kanban
 *   Todo
 *     id1[Create JISON]
 *     id2[Update DB]
 *       @{ priority: high, assigned: knsv }
 *   Done
 *     id3[Review PR]
 */
class KanbanParser : DiagramParser {

    private val RE_START = Regex("^\\s*kanban\\s*$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    // 节点带 ID 和描述：id[label] 或 id(label) 等
    private val RE_NODE_WITH_ID = Regex("^(\\w+)([\\[(])(.+?)([\\])])$")
    // 元数据 @{ key: value, ... }
    private val RE_METADATA = Regex("^@\\{(.+)}$")

    override fun parse(text: String, db: DiagramDB) {
        val kdb = db as KanbanDb
        kdb.clear()

        val lines = text.lines()
        var started = false
        var idCounter = 0

        for (line in lines) {
            val raw = line
            if (raw.trim().isEmpty() || raw.trim().startsWith("%%")) continue

            if (!started) {
                if (RE_START.matches(raw.trim())) started = true
                continue
            }

            val trimmed = raw.trim()

            RE_ACC_TITLE.find(trimmed)?.let {
                kdb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                kdb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            // 跳过元数据行（已经附加到前一个节点）
            if (trimmed.startsWith("@{")) {
                // 解析元数据并应用到最后一个节点
                RE_METADATA.find(trimmed)?.let { m ->
                    val metadata = parseMetadata(m.groupValues[1])
                    val nodes = kdb.getNodes()
                    if (nodes.isNotEmpty()) {
                        val lastNode = nodes.last()
                        // 由于 KanbanNode 是 data class，需要通过 db 更新
                        // 简化：跳过元数据更新（KanbanDb 不支持直接修改）
                    }
                }
                continue
            }

            // 计算缩进层级
            val indent = raw.length - raw.trimStart().length
            val level = if (indent < 2) 0 else 1  // 简化：无缩进=section，有缩进=item

            // 解析节点
            RE_NODE_WITH_ID.find(trimmed)?.let { m ->
                val id = m.groupValues[1]
                val label = m.groupValues[3]
                kdb.addNode(id, label, level)
                return@let
            } ?: run {
                // 纯文本节点（section 标题或无括号的 item）
                val id = "node${idCounter++}"
                kdb.addNode(id, trimmed, level)
            }
        }
    }

    private fun parseMetadata(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (part in content.split(",")) {
            val kv = part.trim().split(":", limit = 2)
            if (kv.size == 2) {
                result[kv[0].trim()] = kv[1].trim().removeSurrounding("\"")
            }
        }
        return result
    }
}
