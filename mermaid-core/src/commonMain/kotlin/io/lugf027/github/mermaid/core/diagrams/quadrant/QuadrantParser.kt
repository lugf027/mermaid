package io.lugf027.github.mermaid.core.diagrams.quadrant

import io.lugf027.github.mermaid.core.types.ParserDefinition

class QuadrantParser(private val db: QuadrantDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("quadrant")) i++

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
                lower.startsWith("x-axis ") -> {
                    val parts = line.substringAfter("x-axis ").split("-->").map { it.trim() }
                    db.xAxisLeft = parts.getOrElse(0) { "Low" }
                    db.xAxisRight = parts.getOrElse(1) { "High" }
                }
                lower.startsWith("y-axis ") -> {
                    val parts = line.substringAfter("y-axis ").split("-->").map { it.trim() }
                    db.yAxisBottom = parts.getOrElse(0) { "Low" }
                    db.yAxisTop = parts.getOrElse(1) { "High" }
                }
                lower.startsWith("quadrant-1 ") -> db.q1Text = line.substringAfter("quadrant-1 ").trim()
                lower.startsWith("quadrant-2 ") -> db.q2Text = line.substringAfter("quadrant-2 ").trim()
                lower.startsWith("quadrant-3 ") -> db.q3Text = line.substringAfter("quadrant-3 ").trim()
                lower.startsWith("quadrant-4 ") -> db.q4Text = line.substringAfter("quadrant-4 ").trim()
                line.contains("[") && line.contains("]") -> {
                    // "Label": [0.3, 0.6] or Label:::class: [0.3, 0.6]
                    val label = line.substringBefore(":").trim().removeSurrounding("\"")
                    val coords = line.substringAfter("[").substringBefore("]").split(",").map { it.trim().toFloatOrNull() ?: 0f }
                    if (coords.size >= 2) db.addPoint(QuadrantPoint(label, coords[0], coords[1]))
                }
            }
        }
    }
}
