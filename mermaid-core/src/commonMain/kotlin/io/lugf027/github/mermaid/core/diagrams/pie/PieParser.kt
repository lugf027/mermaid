package io.lugf027.github.mermaid.core.diagrams.pie

import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * 饼图解析器。
 * 解析 pie 图语法，填充 PieDb。
 *
 * 语法格式：
 * ```
 * pie [showData] [title <标题>]
 *     "标签1" : 数值1
 *     "标签2" : 数值2
 * ```
 */
class PieParser(private val db: PieDb) : ParserDefinition {

    override fun parse(input: String) {
        val lines = input.lines()
        var lineIndex = 0

        // 解析第一行：pie [showData] [title ...]
        if (lineIndex < lines.size) {
            val firstLine = lines[lineIndex].trim()
            lineIndex++

            // 去掉 "pie" 关键字
            var remaining = firstLine.removePrefix("pie").trim()

            // 检查 showData
            if (remaining.startsWith("showData", ignoreCase = true)) {
                db.setShowData(true)
                remaining = remaining.removePrefix("showData").trim()
            }

            // 检查 title
            if (remaining.startsWith("title", ignoreCase = true)) {
                val title = remaining.removePrefix("title").trim()
                if (title.isNotEmpty()) {
                    db.setDiagramTitle(title)
                }
            }
        }

        // 解析后续行
        while (lineIndex < lines.size) {
            val line = lines[lineIndex].trim()
            lineIndex++

            if (line.isEmpty() || line.startsWith("%%")) continue

            // accTitle: ...
            if (line.startsWith("accTitle:", ignoreCase = true)) {
                db.setAccTitle(line.removePrefix("accTitle:").trim())
                continue
            }

            // accDescr: ...
            if (line.startsWith("accDescr:", ignoreCase = true)) {
                db.setAccDescription(line.removePrefix("accDescr:").trim())
                continue
            }

            // accDescr { ... } 多行描述
            if (line.startsWith("accDescr", ignoreCase = true) && line.contains("{")) {
                val sb = StringBuilder()
                while (lineIndex < lines.size) {
                    val descLine = lines[lineIndex].trim()
                    lineIndex++
                    if (descLine.contains("}")) break
                    sb.appendLine(descLine)
                }
                db.setAccDescription(sb.toString().trim())
                continue
            }

            // title ...（后续行中的标题）
            if (line.startsWith("title", ignoreCase = true) && !line.contains(":")) {
                val title = line.removePrefix("title").trim()
                if (title.isNotEmpty()) {
                    db.setDiagramTitle(title)
                }
                continue
            }

            // 数据行："标签" : 数值
            parseSectionLine(line)
        }
    }

    /**
     * 解析数据行。
     * 格式："标签" : 数值
     */
    private fun parseSectionLine(line: String) {
        // 匹配 "label" : value 模式
        val regex = Regex("""^\s*"([^"]+)"\s*:\s*([\d.]+)\s*$""")
        val match = regex.find(line)
        if (match != null) {
            val label = match.groupValues[1]
            val value = match.groupValues[2].toDoubleOrNull() ?: return
            db.addSection(label, value)
        }
    }
}
