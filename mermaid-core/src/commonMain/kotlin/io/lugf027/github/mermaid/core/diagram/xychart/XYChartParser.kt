package io.lugf027.github.mermaid.core.diagram.xychart

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * XY 图表解析器 - 对标 mermaid-js xychart.jison
 *
 * 语法：
 *   xychart-beta [horizontal|vertical]
 *   title <text>
 *   x-axis <title> [cat1, cat2, ...]
 *   x-axis <title> min --> max
 *   y-axis <title> min --> max
 *   line [1, 2, 3, 4]
 *   line "title" [1, 2, 3, 4]
 *   bar [1, 2, 3, 4]
 *   bar "title" [1, 2, 3, 4]
 */
class XYChartParser : DiagramParser {

    private val RE_START = Regex("^\\s*xychart(-beta)?(?:\\s+(horizontal|vertical))?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_X_AXIS_BAND = Regex("^\\s*x-axis\\s+(.+?)\\s*\\[(.+)]\\s*$", RegexOption.IGNORE_CASE)
    private val RE_X_AXIS_LINEAR = Regex("^\\s*x-axis\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*-->\\s*(\\d+(?:\\.\\d+)?)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_Y_AXIS = Regex("^\\s*y-axis\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*-->\\s*(\\d+(?:\\.\\d+)?)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_LINE = Regex("^\\s*line(?:\\s+\"(.+?)\")?\\s*\\[(.+)]\\s*$", RegexOption.IGNORE_CASE)
    private val RE_BAR = Regex("^\\s*bar(?:\\s+\"(.+?)\")?\\s*\\[(.+)]\\s*$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val xyDb = db as XYChartDb
        xyDb.clear()

        val lines = text.lines()
        var started = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            if (!started) {
                RE_START.find(trimmed)?.let { m ->
                    val orient = m.groupValues[2]
                    if (orient.isNotEmpty()) xyDb.setOrientation(orient)
                    started = true
                }
                continue
            }

            RE_TITLE.find(trimmed)?.let {
                xyDb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_TITLE.find(trimmed)?.let {
                xyDb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                xyDb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_X_AXIS_BAND.find(trimmed)?.let { m ->
                val title = m.groupValues[1].trim().removeSurrounding("\"")
                val cats = m.groupValues[2].split(",").map { it.trim().removeSurrounding("\"") }
                xyDb.setXAxisBand(title, cats)
                return@let
            }?.also { continue }

            RE_X_AXIS_LINEAR.find(trimmed)?.let { m ->
                val title = m.groupValues[1].trim().removeSurrounding("\"")
                val min = m.groupValues[2].toDoubleOrNull() ?: 0.0
                val max = m.groupValues[3].toDoubleOrNull() ?: 100.0
                xyDb.setXAxisLinear(title, min, max)
                return@let
            }?.also { continue }

            RE_Y_AXIS.find(trimmed)?.let { m ->
                val title = m.groupValues[1].trim().removeSurrounding("\"")
                val min = m.groupValues[2].toDoubleOrNull() ?: 0.0
                val max = m.groupValues[3].toDoubleOrNull() ?: 100.0
                xyDb.setYAxisLinear(title, min, max)
                return@let
            }?.also { continue }

            RE_LINE.find(trimmed)?.let { m ->
                val title = m.groupValues[1]
                val data = m.groupValues[2].split(",").mapNotNull { it.trim().toDoubleOrNull() }
                xyDb.addLinePlot(title, data)
                return@let
            }?.also { continue }

            RE_BAR.find(trimmed)?.let { m ->
                val title = m.groupValues[1]
                val data = m.groupValues[2].split(",").mapNotNull { it.trim().toDoubleOrNull() }
                xyDb.addBarPlot(title, data)
                return@let
            }
        }
    }
}
