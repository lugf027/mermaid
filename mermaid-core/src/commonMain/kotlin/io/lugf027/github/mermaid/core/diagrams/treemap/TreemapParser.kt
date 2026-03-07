package io.lugf027.github.mermaid.core.diagrams.treemap

import io.lugf027.github.mermaid.core.types.ParserDefinition

class TreemapParser(private val db: TreemapDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("treemap")) i++
        val stack = mutableListOf<TreemapNode>()

        while (i < lines.size) {
            val rawLine = lines[i]; i++
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue
            val indent = rawLine.length - rawLine.trimStart().length
            val level = indent / 2
            val parts = trimmed.split(":").map { it.trim() }
            val label = parts[0]; val value = parts.getOrElse(1) { "1" }.toFloatOrNull() ?: 1f
            val node = TreemapNode(label, value)
            if (stack.isEmpty()) { db.root = node; stack.add(node) }
            else {
                while (stack.size > level + 1 && stack.size > 1) stack.removeAt(stack.lastIndex)
                stack.last().children.add(node); stack.add(node)
            }
        }
    }
}
