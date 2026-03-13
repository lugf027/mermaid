package io.lugf027.github.mermaid.core.diagram.ishikawa

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 鱼骨图解析器 - 对标 mermaid-js ishikawa.jison
 *
 * 语法（缩进感知）：
 *   ishikawa-beta
 *     Effect Text
 *       Category 1
 *         Cause A
 *         Cause B
 *       Category 2
 *         Cause C
 *
 * 每行的前导空格数量决定层级深度。
 */
class IshikawaParser : DiagramParser {

    private val RE_START = Regex("^\\s*ishikawa(-beta)?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val idb = db as IshikawaDb
        idb.clear()

        val lines = text.lines()
        var started = false
        var baseIndent = -1 // 第一个内容行的缩进基准

        for (line in lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("%%")) continue

            if (!started) {
                if (RE_START.matches(line.trim())) started = true
                continue
            }

            val trimmed = line.trim()

            RE_ACC_TITLE.find(trimmed)?.let {
                idb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                idb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            // 计算缩进空格数
            val spaces = line.length - line.trimStart().length
            if (baseIndent < 0) baseIndent = spaces

            // 层级 = (spaces - baseIndent) / indentUnit
            // 使用 4 空格为一个层级单位（如果检测到首行子节点比根节点多的空格数）
            val level = if (baseIndent >= 0 && spaces > baseIndent) {
                ((spaces - baseIndent) + 1) / 2  // 每2个空格一个层级
            } else {
                0
            }

            idb.addNode(level, trimmed)
        }
    }
}
