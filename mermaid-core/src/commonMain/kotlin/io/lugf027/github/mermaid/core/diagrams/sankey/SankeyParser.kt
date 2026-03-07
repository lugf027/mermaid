package io.lugf027.github.mermaid.core.diagrams.sankey

import io.lugf027.github.mermaid.core.types.ParserDefinition

class SankeyParser(private val db: SankeyDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("sankey")) i++
        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            // Format: source,target,value
            val parts = line.split(",").map { it.trim() }
            if (parts.size >= 3) {
                val value = parts[2].toFloatOrNull() ?: continue
                db.addLink(parts[0], parts[1], value)
            }
        }
    }
}
