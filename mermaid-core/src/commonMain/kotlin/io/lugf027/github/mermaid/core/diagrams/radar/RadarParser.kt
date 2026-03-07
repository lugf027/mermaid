package io.lugf027.github.mermaid.core.diagrams.radar

import io.lugf027.github.mermaid.core.types.ParserDefinition

class RadarParser(private val db: RadarDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("radar")) i++

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
                lower.startsWith("axis ") -> {
                    val axes = line.substringAfter("axis ").split(",").map { it.trim() }
                    axes.forEach { db.addAxis(RadarAxis(it)) }
                }
                lower.startsWith("curve ") -> {
                    val rest = line.substringAfter("curve ").trim()
                    val name = rest.substringBefore("{").trim()
                    val data = rest.substringAfter("{").substringBefore("}").split(",").mapNotNull {
                        val v = it.trim()
                        if (v.contains(":")) v.substringAfter(":").trim().toFloatOrNull()
                        else v.toFloatOrNull()
                    }
                    db.addCurve(RadarCurve(name, name, data))
                }
                lower.startsWith("showlegend ") -> db.showLegend = line.substringAfter(" ").trim().lowercase() == "true"
                lower.startsWith("ticks ") -> db.ticks = line.substringAfter(" ").trim().toIntOrNull() ?: 5
                lower.startsWith("graticule ") -> db.graticule = line.substringAfter(" ").trim()
            }
        }
    }
}
