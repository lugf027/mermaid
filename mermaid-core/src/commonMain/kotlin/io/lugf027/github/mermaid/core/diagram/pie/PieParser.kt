package io.lugf027.github.mermaid.core.diagram.pie

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 饼图解析器 - 对标 mermaid-js pieParser.ts + pie.langium 语法
 *
 * 手写递归下降解析器，解析以下语法：
 * ```
 * pie [showData]
 *   [title <text>]
 *   [accTitle: <text>]
 *   [accDescr: <text>]
 *   [accDescr { <multiline text> }]
 *   "<label>" : <number>
 *   ...
 * ```
 */
class PieParser : DiagramParser {

    private val log = Logger("PieParser")

    override fun parse(text: String, db: DiagramDB) {
        val pieDb = db as? PieDb ?: throw IllegalArgumentException("Expected PieDb")
        pieDb.clear()

        val lines = text.lines()
        var i = 0

        // 跳过空行
        while (i < lines.size && lines[i].trim().isEmpty()) i++

        if (i >= lines.size) {
            log.warn("Empty pie chart input")
            return
        }

        // 第一行应包含 "pie" 关键字
        val headerLine = lines[i].trim()
        if (!headerLine.startsWith("pie", ignoreCase = true)) {
            log.error("Expected 'pie' keyword, got: $headerLine")
            throw IllegalArgumentException("Pie chart must start with 'pie' keyword")
        }

        // 检查 showData 标志
        val afterPie = headerLine.substring(3).trim()
        if (afterPie.equals("showData", ignoreCase = true) ||
            afterPie.startsWith("showData", ignoreCase = true)
        ) {
            pieDb.setShowData(true)
        }
        i++

        // 解析后续行
        while (i < lines.size) {
            val line = lines[i].trim()
            i++

            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("%%")) continue

            // title 指令
            if (line.startsWith("title ", ignoreCase = true) || line.startsWith("title\t")) {
                val titleText = line.substring(5).trim()
                pieDb.setDiagramTitle(titleText)
                continue
            }

            // accTitle 指令
            if (line.startsWith("accTitle", ignoreCase = true)) {
                val colonIdx = line.indexOf(':')
                if (colonIdx >= 0) {
                    pieDb.setAccTitle(line.substring(colonIdx + 1).trim())
                }
                continue
            }

            // accDescr 多行指令
            if (line.startsWith("accDescr", ignoreCase = true) && line.contains("{")) {
                val descBuilder = StringBuilder()
                while (i < lines.size) {
                    val descLine = lines[i]
                    i++
                    if (descLine.trim().contains("}")) break
                    descBuilder.appendLine(descLine)
                }
                pieDb.setAccDescription(descBuilder.toString().trim())
                continue
            }

            // accDescr 单行指令
            if (line.startsWith("accDescr", ignoreCase = true)) {
                val colonIdx = line.indexOf(':')
                if (colonIdx >= 0) {
                    pieDb.setAccDescription(line.substring(colonIdx + 1).trim())
                }
                continue
            }

            // 扇区解析: "label" : number 或 'label' : number
            val sectionResult = parseSection(line)
            if (sectionResult != null) {
                pieDb.addSection(sectionResult.first, sectionResult.second)
                continue
            }

            // 未识别的行，跳过（可能是 showData 在单独行等）
            if (line.equals("showData", ignoreCase = true)) {
                pieDb.setShowData(true)
                continue
            }

            log.debug("Skipping unrecognized line: $line")
        }
    }

    /**
     * 解析扇区行: "label" : number 或 'label' : number
     *
     * @return (label, value) 对，如果不匹配则返回 null
     */
    private fun parseSection(line: String): Pair<String, Double>? {
        // 匹配 "label" : number 或 'label' : number
        val regex = Regex("""^\s*("[^"]*"|'[^']*')\s*:\s*(-?\d+(?:\.\d+)?)\s*$""")
        val match = regex.matchEntire(line) ?: return null

        val rawLabel = match.groupValues[1]
        // 去掉引号
        val label = rawLabel.substring(1, rawLabel.length - 1)
        val value = match.groupValues[2].toDoubleOrNull() ?: return null

        return Pair(label, value)
    }
}
