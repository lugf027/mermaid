package io.lugf027.github.mermaid.core.diagrams.gantt

import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * 甘特图解析器。
 */
class GanttParser(private val db: GanttDb) : ParserDefinition {

    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0

        if (i < lines.size && lines[i].trim().lowercase().startsWith("gantt")) i++

        while (i < lines.size) {
            val line = lines[i].trim()
            i++
            if (line.isEmpty() || line.startsWith("%%")) continue

            val lower = line.lowercase()
            when {
                lower.startsWith("dateformat ") -> db.setDateFormat(line.substringAfter(" ").trim())
                lower.startsWith("axisformat ") -> db.setAxisFormat(line.substringAfter(" ").trim())
                lower.startsWith("todaymarker ") -> db.setTodayMarker(line.substringAfter(" ").trim())
                lower.startsWith("excludes ") -> db.addExclude(line.substringAfter(" ").trim())
                lower.startsWith("topaxis") -> db.setTopAxis(true)
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
                lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())
                lower.startsWith("accdescr:") -> db.setAccDescription(line.substringAfter(":").trim())
                lower.startsWith("section ") -> db.addSection(line.substringAfter("section ").trim())
                else -> db.addTask(line)
            }
        }
    }
}
