package io.lugf027.github.mermaid.core.diagrams.venn

import io.lugf027.github.mermaid.core.types.ParserDefinition

class VennParser(private val db: VennDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("venn")) i++
        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
                else -> {
                    val label = line.removeSurrounding("\"")
                    db.addSet(VennSet("set_${db.getSets().size}", label))
                }
            }
        }
    }
}
