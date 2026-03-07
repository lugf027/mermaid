package io.lugf027.github.mermaid.core.diagrams.mindmap

import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * 思维导图解析器。
 * 基于缩进级别构建树形结构。
 */
class MindmapParser(private val db: MindmapDb) : ParserDefinition {

    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0

        // 跳过 mindmap 关键字
        if (i < lines.size && lines[i].trim().lowercase().startsWith("mindmap")) i++

        val stack = mutableListOf<MindmapNode>()

        while (i < lines.size) {
            val rawLine = lines[i]
            i++

            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            // 计算缩进级别（每2或4空格为一级）
            val indent = rawLine.length - rawLine.trimStart().length
            val level = indent / 2  // 简化：每2空格一级

            // 解析节点文本和形状
            val (text, nodeType) = parseNodeShape(trimmed)

            val node = db.createNode(level, text, nodeType)

            // 构建树结构
            if (stack.isEmpty()) {
                db.setRoot(node)
                stack.add(node)
            } else {
                // 找到父节点（level 比当前小的最后一个）
                while (stack.size > 1 && stack.last().level >= level) {
                    stack.removeAt(stack.lastIndex)
                }
                stack.last().children.add(node)
                stack.add(node)
            }
        }
    }

    private fun parseNodeShape(text: String): Pair<String, MindmapNodeType> {
        return when {
            // ((...)) = CIRCLE
            text.startsWith("((") && text.endsWith("))") ->
                text.removePrefix("((").removeSuffix("))") to MindmapNodeType.CIRCLE
            // (...) = ROUNDED_RECT
            text.startsWith("(") && text.endsWith(")") && !text.startsWith("((") ->
                text.removePrefix("(").removeSuffix(")") to MindmapNodeType.ROUNDED_RECT
            // [...] = RECT
            text.startsWith("[") && text.endsWith("]") ->
                text.removePrefix("[").removeSuffix("]") to MindmapNodeType.RECT
            // )...( = CLOUD
            text.startsWith(")") && text.endsWith("(") ->
                text.removePrefix(")").removeSuffix("(") to MindmapNodeType.CLOUD
            // ))...(( = BANG
            text.startsWith("))") && text.endsWith("((") ->
                text.removePrefix("))").removeSuffix("((") to MindmapNodeType.BANG
            // {{...}} = HEXAGON
            text.startsWith("{{") && text.endsWith("}}") ->
                text.removePrefix("{{").removeSuffix("}}") to MindmapNodeType.HEXAGON
            else -> text to MindmapNodeType.DEFAULT
        }
    }
}
