package io.lugf027.github.mermaid.core.diagram.quadrantChart

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 象限图解析器 - 对标 mermaid-js quadrant.jison
 *
 * 语法：
 *   quadrantChart
 *   title <text>
 *   x-axis <left> --> <right>
 *   y-axis <bottom> --> <top>
 *   quadrant-1 <text>
 *   quadrant-2 <text>
 *   quadrant-3 <text>
 *   quadrant-4 <text>
 *   <text>: [x, y]
 *   <text>:::className: [x, y]
 *   classDef className color:#hex, radius:5
 */
class QuadrantParser : DiagramParser {

    private val RE_START = Regex("^\\s*quadrantChart\\s*$", RegexOption.IGNORE_CASE)
    private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_X_AXIS = Regex("^\\s*x-axis\\s+(.+?)(?:\\s*-->\\s*(.+))?$", RegexOption.IGNORE_CASE)
    private val RE_Y_AXIS = Regex("^\\s*y-axis\\s+(.+?)(?:\\s*-->\\s*(.+))?$", RegexOption.IGNORE_CASE)
    private val RE_QUADRANT = Regex("^\\s*quadrant-([1-4])\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_POINT = Regex("^\\s*(.+?)(?::::(\\S+))?\\s*:\\s*\\[\\s*([\\d.]+)\\s*,\\s*([\\d.]+)\\s*]\\s*$")
    private val RE_CLASSDEF = Regex("^\\s*classDef\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val qdb = db as QuadrantDb
        qdb.clear()

        val lines = text.lines()
        var started = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            if (!started) {
                if (RE_START.matches(trimmed)) started = true
                continue
            }

            RE_TITLE.find(trimmed)?.let {
                qdb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_TITLE.find(trimmed)?.let {
                qdb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                qdb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_X_AXIS.find(trimmed)?.let { m ->
                qdb.xAxisLeftText = m.groupValues[1].trim()
                if (m.groupValues[2].isNotEmpty()) {
                    qdb.xAxisRightText = m.groupValues[2].trim()
                }
                return@let
            }?.also { continue }

            RE_Y_AXIS.find(trimmed)?.let { m ->
                qdb.yAxisBottomText = m.groupValues[1].trim()
                if (m.groupValues[2].isNotEmpty()) {
                    qdb.yAxisTopText = m.groupValues[2].trim()
                }
                return@let
            }?.also { continue }

            RE_QUADRANT.find(trimmed)?.let { m ->
                val num = m.groupValues[1].toInt()
                val label = m.groupValues[2].trim()
                when (num) {
                    1 -> qdb.quadrant1Text = label
                    2 -> qdb.quadrant2Text = label
                    3 -> qdb.quadrant3Text = label
                    4 -> qdb.quadrant4Text = label
                }
                return@let
            }?.also { continue }

            RE_CLASSDEF.find(trimmed)?.let { m ->
                val name = m.groupValues[1]
                val stylesStr = m.groupValues[2]
                val styles = mutableMapOf<String, String>()
                for (part in stylesStr.split(",")) {
                    val kv = part.trim().split(":", limit = 2)
                    if (kv.size == 2) styles[kv[0].trim()] = kv[1].trim()
                }
                qdb.addClassDef(name, styles)
                return@let
            }?.also { continue }

            RE_POINT.find(trimmed)?.let { m ->
                val label = m.groupValues[1].trim()
                val className = m.groupValues[2]
                val x = m.groupValues[3].toDoubleOrNull() ?: 0.0
                val y = m.groupValues[4].toDoubleOrNull() ?: 0.0
                qdb.addPoint(x, y, label, className)
                return@let
            }
        }
    }
}
