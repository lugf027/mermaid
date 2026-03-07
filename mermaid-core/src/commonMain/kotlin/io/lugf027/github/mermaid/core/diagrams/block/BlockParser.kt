package io.lugf027.github.mermaid.core.diagrams.block

import io.lugf027.github.mermaid.core.types.ParserDefinition

class BlockParser(private val db: BlockDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("block")) i++

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("columns ") -> db.columns = line.substringAfter(" ").trim().toIntOrNull() ?: 0
                line.contains("-->") -> {
                    val parts = line.split("-->").map { it.trim() }
                    if (parts.size >= 2) db.addEdge(BlockEdge(parts[0], parts[1]))
                }
                else -> {
                    val id = line.substringBefore("[").substringBefore("(").substringBefore("{").trim()
                    val label = when {
                        line.contains("[\"") -> line.substringAfter("[\"").substringBefore("\"]")
                        line.contains("(\"") -> line.substringAfter("(\"").substringBefore("\")")
                        line.contains("{{\"") -> line.substringAfter("{{\"").substringBefore("\"}}")
                        else -> id
                    }
                    if (id.isNotEmpty()) db.addBlock(BlockNode(id, label))
                }
            }
        }
    }
}
