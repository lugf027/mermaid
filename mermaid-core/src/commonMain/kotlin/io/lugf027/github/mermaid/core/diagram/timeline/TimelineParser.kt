package io.lugf027.github.mermaid.core.diagram.timeline

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * Timeline 解析器 - 对标 mermaid-js timeline.jison
 *
 * 支持的语法：
 * ```
 * timeline
 *     title My Timeline
 *     section Section Name
 *     2023-Q1 : Event 1
 *              : Event 2
 *     2023-Q2 : Event 3
 * ```
 */
class TimelineParser : DiagramParser {

    companion object {
        // timeline 关键字
        private val RE_TIMELINE = Regex("^\\s*timeline\\s*$", RegexOption.IGNORE_CASE)

        // title 行
        private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)

        // section 行
        private val RE_SECTION = Regex("^\\s*section\\s+(.+)$", RegexOption.IGNORE_CASE)

        // accTitle / accDescr
        private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
        private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
        private val RE_ACC_DESCR_MULTI_START = Regex("^\\s*accDescr\\s*\\{\\s*$", RegexOption.IGNORE_CASE)
        private val RE_ACC_DESCR_MULTI_END = Regex("^\\s*\\}\\s*$")

        // 事件行: ": event text"
        private val RE_EVENT = Regex("^\\s*:\\s+(.+)$")

        // 时间段行: 非 ":" 开头的文本
        private val RE_PERIOD = Regex("^\\s*([^#:\\n;]+)\\s*$")

        // 带事件的时间段行: "2023-Q1 : Event"
        private val RE_PERIOD_WITH_EVENT = Regex("^\\s*([^#:\\n;]+?)\\s*:\\s+(.+)$")

        // 注释
        private val RE_COMMENT = Regex("^\\s*%%")
        private val RE_HASH_COMMENT = Regex("^\\s*#")
    }

    override fun parse(text: String, db: DiagramDB) {
        val timelineDb = db as? TimelineDb ?: throw IllegalArgumentException("Expected TimelineDb")
        timelineDb.clear()

        val lines = text.lines()
        var inMultiDescr = false
        val multiDescrLines = mutableListOf<String>()

        for (line in lines) {
            // 跳过空行
            if (line.isBlank()) continue

            // 跳过注释
            if (RE_COMMENT.containsMatchIn(line) || RE_HASH_COMMENT.containsMatchIn(line)) continue

            // 多行 accDescr 处理
            if (inMultiDescr) {
                if (RE_ACC_DESCR_MULTI_END.containsMatchIn(line)) {
                    inMultiDescr = false
                    timelineDb.setAccDescription(multiDescrLines.joinToString("\n").trim())
                    multiDescrLines.clear()
                } else {
                    multiDescrLines.add(line)
                }
                continue
            }

            // timeline 关键字（跳过）
            if (RE_TIMELINE.containsMatchIn(line)) continue

            // title
            RE_TITLE.find(line)?.let {
                timelineDb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            } ?: run {
                // accTitle
                RE_ACC_TITLE.find(line)?.let {
                    timelineDb.setAccTitle(it.groupValues[1].trim())
                    return@run
                }

                // accDescr single line
                RE_ACC_DESCR.find(line)?.let {
                    timelineDb.setAccDescription(it.groupValues[1].trim())
                    return@run
                }

                // accDescr multi-line start
                if (RE_ACC_DESCR_MULTI_START.containsMatchIn(line)) {
                    inMultiDescr = true
                    return@run
                }

                // section
                RE_SECTION.find(line)?.let {
                    timelineDb.addSection(it.groupValues[1].trim())
                    return@run
                }

                // event line (": event text")
                RE_EVENT.find(line)?.let {
                    timelineDb.addEvent(it.groupValues[1].trim())
                    return@run
                }

                // period with event ("2023-Q1 : Event")
                RE_PERIOD_WITH_EVENT.find(line)?.let {
                    val period = it.groupValues[1].trim()
                    val event = it.groupValues[2].trim()
                    timelineDb.addTask(period, 0, event)
                    return@run
                }

                // plain period line
                RE_PERIOD.find(line)?.let {
                    timelineDb.addTask(it.groupValues[1].trim())
                    return@run
                }
            }
        }
    }
}
