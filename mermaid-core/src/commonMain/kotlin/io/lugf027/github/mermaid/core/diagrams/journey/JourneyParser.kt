package io.lugf027.github.mermaid.core.diagrams.journey

import io.lugf027.github.mermaid.core.types.ParserDefinition

class JourneyParser(private val db: JourneyDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("journey")) i++

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
                lower.startsWith("section ") -> db.addSection(line.substringAfter("section ").trim())
                line.contains(":") -> {
                    // Task : score : person1, person2
                    val parts = line.split(":").map { it.trim() }
                    val task = parts[0]
                    val score = parts.getOrElse(1) { "3" }.toIntOrNull() ?: 3
                    val people = if (parts.size >= 3) parts[2].split(",").map { it.trim() } else emptyList()
                    db.addTask(task, score, people)
                }
            }
        }
    }
}
