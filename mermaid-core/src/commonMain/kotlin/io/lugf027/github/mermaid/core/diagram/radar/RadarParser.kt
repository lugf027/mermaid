package io.lugf027.github.mermaid.core.diagram.radar

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 雷达图解析器 - 对标 mermaid-js radar grammar
 *
 * 语法：
 *   radar-beta
 *   title <text>
 *   axis <label1>, <label2>, ...
 *   max <number>
 *   showLegend [true|false]
 *   <dataset_name> --> <v1>, <v2>, ...
 *   "dataset name" --> <v1>, <v2>, ...
 */
class RadarParser : DiagramParser {

    private val RE_START = Regex("^\\s*radar(-beta)?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_AXIS = Regex("^\\s*axis\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_MAX = Regex("^\\s*max\\s+(\\d+(?:\\.\\d+)?)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_SHOW_LEGEND = Regex("^\\s*showLegend\\s+(true|false)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_DATASET = Regex("^\\s*(?:\"(.+?)\"|([^\"\\s]\\S*))\\s*-->\\s*(.+)$")
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val rdb = db as RadarDb
        rdb.clear()

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
                rdb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_TITLE.find(trimmed)?.let {
                rdb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                rdb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_AXIS.find(trimmed)?.let { m ->
                val axisLabels = m.groupValues[1].split(",").map { it.trim().removeSurrounding("\"") }
                rdb.setAxes(axisLabels)
                return@let
            }?.also { continue }

            RE_MAX.find(trimmed)?.let { m ->
                val maxVal = m.groupValues[1].toDoubleOrNull()
                if (maxVal != null) rdb.setMaxValue(maxVal)
                return@let
            }?.also { continue }

            RE_SHOW_LEGEND.find(trimmed)?.let { m ->
                rdb.setShowLegend(m.groupValues[1].equals("true", ignoreCase = true))
                return@let
            }?.also { continue }

            RE_DATASET.find(trimmed)?.let { m ->
                val name = m.groupValues[1].ifEmpty { m.groupValues[2] }
                val values = m.groupValues[3].split(",").mapNotNull { it.trim().toDoubleOrNull() }
                if (name.isNotEmpty() && values.isNotEmpty()) {
                    rdb.addDataset(name, values)
                }
                return@let
            }
        }
    }
}
