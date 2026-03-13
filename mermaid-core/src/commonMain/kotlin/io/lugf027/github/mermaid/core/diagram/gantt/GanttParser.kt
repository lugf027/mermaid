package io.lugf027.github.mermaid.core.diagram.gantt

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 甘特图解析器 - 对标 mermaid-js gantt.jison
 *
 * 手写递归下降解析器，逐行扫描解析甘特图语法。
 *
 * 支持的语法:
 * - gantt (图表声明)
 * - dateFormat FORMAT
 * - axisFormat FORMAT
 * - tickInterval INTERVAL
 * - todayMarker STYLE|off
 * - inclusiveEndDates
 * - topAxis
 * - weekday monday|tuesday|...
 * - weekend friday|saturday
 * - excludes dates
 * - includes dates
 * - section NAME
 * - title TEXT
 * - accTitle: TEXT
 * - accDescr: TEXT / accDescr { ... }
 * - taskName : status, id, startDate, duration
 * - %% comment
 */
class GanttParser : DiagramParser {

    private val log = Logger("GanttParser")

    override fun parse(text: String, db: DiagramDB) {
        val ganttDb = db as? GanttDb ?: throw IllegalArgumentException("Expected GanttDb")
        ganttDb.clear()

        val lines = text.lines()
        var i = 0
        var ganttStarted = false

        while (i < lines.size) {
            val rawLine = lines[i]
            val line = rawLine.trim()
            i++

            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("%%")) continue

            // gantt 声明
            if (!ganttStarted) {
                if (line.lowercase() == "gantt") {
                    ganttStarted = true
                }
                continue
            }

            // 解析指令（大小写不敏感匹配关键字）
            val lineLower = line.lowercase()

            // dateFormat
            if (lineLower.startsWith("dateformat")) {
                val value = line.substring("dateformat".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.setDateFormat(value)
                }
                continue
            }

            // axisFormat
            if (lineLower.startsWith("axisformat")) {
                val value = line.substring("axisformat".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.setAxisFormat(value)
                }
                continue
            }

            // tickInterval
            if (lineLower.startsWith("tickinterval")) {
                val value = line.substring("tickinterval".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.setTickInterval(value)
                }
                continue
            }

            // todayMarker
            if (lineLower.startsWith("todaymarker")) {
                val value = line.substring("todaymarker".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.setTodayMarker(value)
                }
                continue
            }

            // inclusiveEndDates
            if (lineLower.startsWith("inclusiveenddates")) {
                ganttDb.enableInclusiveEndDates()
                continue
            }

            // topAxis
            if (lineLower.startsWith("topaxis")) {
                ganttDb.enableTopAxis()
                continue
            }

            // weekday
            val weekdayMatch = RE_WEEKDAY.find(line)
            if (weekdayMatch != null) {
                ganttDb.setWeekday(weekdayMatch.groupValues[1])
                continue
            }

            // weekend
            val weekendMatch = RE_WEEKEND.find(line)
            if (weekendMatch != null) {
                ganttDb.setWeekend(weekendMatch.groupValues[1])
                continue
            }

            // excludes
            if (lineLower.startsWith("excludes")) {
                val value = line.substring("excludes".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.setExcludes(value)
                }
                continue
            }

            // includes
            if (lineLower.startsWith("includes")) {
                val value = line.substring("includes".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.setIncludes(value)
                }
                continue
            }

            // displayMode
            if (lineLower.startsWith("displaymode")) {
                val value = line.substring("displaymode".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.setDisplayMode(value)
                }
                continue
            }

            // title
            if (lineLower.startsWith("title")) {
                val value = line.substring("title".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.setDiagramTitle(value)
                }
                continue
            }

            // accTitle
            val accTitleMatch = RE_ACC_TITLE.find(line)
            if (accTitleMatch != null) {
                ganttDb.setAccTitle(accTitleMatch.groupValues[1].trim())
                continue
            }

            // accDescr (单行)
            val accDescrMatch = RE_ACC_DESCR.find(line)
            if (accDescrMatch != null) {
                ganttDb.setAccDescription(accDescrMatch.groupValues[1].trim())
                continue
            }

            // accDescr { ... } (多行)
            if (lineLower.startsWith("accdescr") && line.contains("{")) {
                val sb = StringBuilder()
                while (i < lines.size) {
                    val nextLine = lines[i].trim()
                    i++
                    if (nextLine.contains("}")) break
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(nextLine)
                }
                ganttDb.setAccDescription(sb.toString())
                continue
            }

            // section
            if (lineLower.startsWith("section")) {
                val value = line.substring("section".length).trim()
                if (value.isNotEmpty()) {
                    ganttDb.addSection(value)
                }
                continue
            }

            // click (忽略，KMP 不支持交互)
            if (lineLower.startsWith("click")) continue

            // 任务: taskName : taskData
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val taskName = line.substring(0, colonIdx).trim()
                val taskData = line.substring(colonIdx) // 包含冒号
                if (taskName.isNotEmpty()) {
                    ganttDb.addTask(taskName, taskData)
                }
                continue
            }

            // 无法识别的行，记录日志
            log.debug("Skipping unrecognized line: $line")
        }
    }

    companion object {
        private val RE_WEEKDAY = Regex(
            "^weekday\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)$",
            RegexOption.IGNORE_CASE
        )
        private val RE_WEEKEND = Regex(
            "^weekend\\s+(friday|saturday)$",
            RegexOption.IGNORE_CASE
        )
        private val RE_ACC_TITLE = Regex("^accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
        private val RE_ACC_DESCR = Regex("^accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    }
}
