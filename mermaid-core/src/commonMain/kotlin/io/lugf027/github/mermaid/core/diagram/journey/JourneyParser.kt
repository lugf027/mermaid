package io.lugf027.github.mermaid.core.diagram.journey

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * Journey 图表解析器 - 对标 mermaid-js journey.jison
 *
 * 支持的语法：
 * ```
 * journey
 *     title My Journey
 *     section Section Name
 *     Task Name: 5: Actor1, Actor2
 * ```
 */
class JourneyParser : DiagramParser {

    companion object {
        // journey 关键字
        private val RE_JOURNEY = Regex("^\\s*journey\\s*$", RegexOption.IGNORE_CASE)

        // title 行
        private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)

        // section 行
        private val RE_SECTION = Regex("^\\s*section\\s+(.+)$", RegexOption.IGNORE_CASE)

        // accTitle / accDescr
        private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
        private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
        private val RE_ACC_DESCR_MULTI_START = Regex("^\\s*accDescr\\s*\\{\\s*$", RegexOption.IGNORE_CASE)
        private val RE_ACC_DESCR_MULTI_END = Regex("^\\s*\\}\\s*$")

        // 任务行: "Task Name: score: actor1, actor2" 或 "Task Name: score"
        private val RE_TASK = Regex("^\\s*([^#:\\n;]+)\\s*:\\s*(.+)$")

        // 注释
        private val RE_COMMENT = Regex("^\\s*%%")
        private val RE_HASH_COMMENT = Regex("^\\s*#")
    }

    override fun parse(text: String, db: DiagramDB) {
        val journeyDb = db as? JourneyDb ?: throw IllegalArgumentException("Expected JourneyDb")
        journeyDb.clear()

        val lines = text.lines()
        var inMultiDescr = false
        val multiDescrLines = mutableListOf<String>()

        for (line in lines) {
            // 跳过空行
            if (line.isBlank()) continue

            // 处理注释
            if (RE_COMMENT.containsMatchIn(line) || RE_HASH_COMMENT.containsMatchIn(line)) continue

            // 多行 accDescr 处理
            if (inMultiDescr) {
                if (RE_ACC_DESCR_MULTI_END.containsMatchIn(line)) {
                    inMultiDescr = false
                    journeyDb.setAccDescription(multiDescrLines.joinToString("\n").trim())
                    multiDescrLines.clear()
                } else {
                    multiDescrLines.add(line)
                }
                continue
            }

            // journey 关键字（跳过）
            if (RE_JOURNEY.containsMatchIn(line)) continue

            // title
            RE_TITLE.find(line)?.let {
                journeyDb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            } ?: run {
                // accTitle
                RE_ACC_TITLE.find(line)?.let {
                    journeyDb.setAccTitle(it.groupValues[1].trim())
                    return@run
                }

                // accDescr single line
                RE_ACC_DESCR.find(line)?.let {
                    journeyDb.setAccDescription(it.groupValues[1].trim())
                    return@run
                }

                // accDescr multi-line start
                if (RE_ACC_DESCR_MULTI_START.containsMatchIn(line)) {
                    inMultiDescr = true
                    return@run
                }

                // section
                RE_SECTION.find(line)?.let {
                    journeyDb.addSection(it.groupValues[1].trim())
                    return@run
                }

                // task line
                RE_TASK.find(line)?.let {
                    val taskName = it.groupValues[1].trim()
                    val taskData = ":" + it.groupValues[2].trim()
                    journeyDb.addTask(taskName, taskData)
                    return@run
                }
            }
        }
    }
}
