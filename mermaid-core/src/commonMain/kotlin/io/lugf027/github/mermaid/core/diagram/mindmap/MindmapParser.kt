package io.lugf027.github.mermaid.core.diagram.mindmap

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * Mindmap 解析器 - 对标 mermaid-js mindmap.jison
 *
 * 使用缩进级别（空格数）确定父子关系。
 * 支持 7 种节点形状语法：
 * - `text` → DEFAULT（无边框）
 * - `[text]` → RECT
 * - `(text)` → ROUNDED_RECT
 * - `((text))` → CIRCLE
 * - `)text(` → CLOUD
 * - `))text((` → BANG
 * - `{{text}}` → HEXAGON
 */
class MindmapParser : DiagramParser {

    companion object {
        private val RE_MINDMAP = Regex("^\\s*mindmap\\s*$", RegexOption.IGNORE_CASE)
        private val RE_COMMENT = Regex("^\\s*%%")
        private val RE_ICON = Regex("::icon\\(([^)]+)\\)")
        private val RE_CLASS = Regex(":::([\\w-]+)")

        // 形状定界符对（优先匹配长的）
        private val SHAPE_PAIRS = listOf(
            "((" to "))" to MindmapNodeType.CIRCLE,
            "))" to "((" to MindmapNodeType.BANG,
            "{{" to "}}" to MindmapNodeType.HEXAGON,
            "(-" to "-)" to MindmapNodeType.CLOUD,
            "(" to ")" to MindmapNodeType.ROUNDED_RECT,
            ")" to "(" to MindmapNodeType.CLOUD,
            "[" to "]" to MindmapNodeType.RECT,
        )
    }

    override fun parse(text: String, db: DiagramDB) {
        val mindmapDb = db as? MindmapDb ?: throw IllegalArgumentException("Expected MindmapDb")
        mindmapDb.clear()

        val lines = text.lines()

        for (line in lines) {
            // 跳过空行和纯空白行
            if (line.isBlank()) continue

            // 跳过注释
            if (RE_COMMENT.containsMatchIn(line)) continue

            // 跳过 mindmap 关键字
            if (RE_MINDMAP.containsMatchIn(line)) continue

            // 计算缩进级别
            val indent = line.length - line.trimStart().length

            // 提取 icon 和 class 装饰器，并从行中移除
            var content = line.trimStart()
            val iconMatch = RE_ICON.find(content)
            val classMatch = RE_CLASS.find(content)

            // 移除装饰器
            content = content.replace(RE_ICON, "").replace(RE_CLASS, "").trim()

            // 跳过移除装饰器后的空内容（纯装饰器行作用于上一个节点）
            if (content.isEmpty()) {
                // 装饰器行 - 应用到最后一个节点
                if (iconMatch != null) {
                    mindmapDb.decorateNode(icon = iconMatch.groupValues[1].trim())
                }
                if (classMatch != null) {
                    mindmapDb.decorateNode(cssClass = classMatch.groupValues[1].trim())
                }
                continue
            }

            // 解析节点形状和文本
            val (nodeId, descr, type) = parseNodeShape(content)

            mindmapDb.addNode(indent, nodeId, descr, type)

            // 应用装饰器
            if (iconMatch != null) {
                mindmapDb.decorateNode(icon = iconMatch.groupValues[1].trim())
            }
            if (classMatch != null) {
                mindmapDb.decorateNode(cssClass = classMatch.groupValues[1].trim())
            }
        }

        // 分配 section 编号
        mindmapDb.assignSections()
    }

    /**
     * 解析节点形状和文本
     * @return Triple(nodeId, description, shapeType)
     */
    private fun parseNodeShape(content: String): Triple<String, String, MindmapNodeType> {
        for ((pair, type) in SHAPE_PAIRS) {
            val (start, end) = pair
            if (content.startsWith(start) && content.endsWith(end) && content.length > start.length + end.length) {
                val inner = content.substring(start.length, content.length - end.length).trim()
                // 处理引号包裹的描述
                val descr = inner.removeSurrounding("\"").removeSurrounding("\"`", "`\"")
                return Triple(descr, descr, type)
            }
        }
        // 默认形状（纯文本）
        val descr = content.removeSurrounding("\"").trim()
        return Triple(descr, descr, MindmapNodeType.DEFAULT)
    }
}
