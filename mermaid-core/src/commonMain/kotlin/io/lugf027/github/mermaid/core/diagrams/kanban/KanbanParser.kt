package io.lugf027.github.mermaid.core.diagrams.kanban

import io.lugf027.github.mermaid.core.types.ParserDefinition

class KanbanParser(private val db: KanbanDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("kanban")) i++

        var inMetadata = false
        val metadata = mutableMapOf<String, String>()

        while (i < lines.size) {
            val rawLine = lines[i]; i++
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) { inMetadata = false; continue }

            val indent = rawLine.length - rawLine.trimStart().length

            when {
                indent == 0 || (indent <= 2 && !trimmed.contains(":")) -> {
                    // Column
                    db.addColumn(trimmed)
                    inMetadata = false
                }
                trimmed.contains(":") && inMetadata -> {
                    val parts = trimmed.split(":", limit = 2)
                    metadata[parts[0].trim().lowercase()] = parts[1].trim()
                }
                indent >= 4 && indent < 8 -> {
                    // Item
                    db.addItem(trimmed, metadata.toMap())
                    metadata.clear()
                    inMetadata = false
                }
                indent >= 8 -> {
                    // Metadata line
                    if (trimmed.contains(":")) {
                        val parts = trimmed.split(":", limit = 2)
                        metadata[parts[0].trim().lowercase()] = parts[1].trim()
                        inMetadata = true
                    }
                }
                else -> {
                    db.addItem(trimmed)
                    inMetadata = false
                }
            }
        }
    }
}
