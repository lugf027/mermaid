package io.lugf027.github.mermaid.core.diagrams.xychart

import io.lugf027.github.mermaid.core.types.ParserDefinition

class XyChartParser(private val db: XyChartDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("xychart")) i++

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
                lower.startsWith("x-axis ") -> {
                    val rest = line.substringAfter("x-axis ").trim()
                    if (rest.startsWith("[")) {
                        val cats = rest.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }
                        db.setXAxisCategories(cats)
                    }
                }
                lower.startsWith("y-axis ") -> {
                    val rest = line.substringAfter("y-axis ").trim()
                    val parts = rest.split("-->").map { it.trim() }
                    if (parts.size >= 2) {
                        val label = parts[0].removeSurrounding("\"")
                        val minMax = parts.last().split(Regex("\\s+"))
                        if (minMax.size >= 1) {
                            db.setYAxisRange(label, parts[0].toFloatOrNull() ?: 0f, minMax.last().toFloatOrNull() ?: 100f)
                        }
                    }
                }
                lower.startsWith("bar ") || lower.startsWith("line ") -> {
                    val type = if (lower.startsWith("bar")) PlotType.BAR else PlotType.LINE
                    val data = line.substringAfter("[").substringBefore("]").split(",").mapNotNull { it.trim().toFloatOrNull() }
                    db.addPlot(PlotData(type, data))
                }
            }
        }
    }
}
