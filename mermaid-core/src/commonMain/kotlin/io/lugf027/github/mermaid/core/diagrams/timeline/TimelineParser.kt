package io.lugf027.github.mermaid.core.diagrams.timeline

import io.lugf027.github.mermaid.core.types.ParserDefinition

class TimelineParser(private val db: TimelineDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("timeline")) i++

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
                lower.startsWith("section ") -> db.addSection(line.substringAfter("section ").trim())
                lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())
                lower.startsWith("accdescr:") -> db.setAccDescription(line.substringAfter(":").trim())
                line.contains(":") -> {
                    val parts = line.split(":").map { it.trim() }
                    val period = parts[0]
                    val events = parts.drop(1).filter { it.isNotEmpty() }
                    db.addEvent(period, events)
                }
                else -> db.addEvent(line, emptyList())
            }
        }
    }
}
