package io.lugf027.github.mermaid.core.diagrams.ishikawa

import io.lugf027.github.mermaid.core.types.ParserDefinition

class IshikawaParser(private val db: IshikawaDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("ishikawa")) i++

        val stack = mutableListOf<IshikawaNode>()

        while (i < lines.size) {
            val rawLine = lines[i]; i++
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            val indent = rawLine.length - rawLine.trimStart().length
            val level = indent / 2
            val node = IshikawaNode(trimmed)

            if (stack.isEmpty()) {
                db.root = node
                stack.add(node)
            } else {
                while (stack.size > level + 1 && stack.size > 1) stack.removeAt(stack.lastIndex)
                stack.last().children.add(node)
                stack.add(node)
            }
        }
    }
}
