package io.lugf027.github.mermaid.core.diagram.packet

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 数据包图解析器 - 对标 mermaid-js packet parser
 *
 * 语法：
 *   packet-beta
 *   0-15: "Source Port"
 *   16-31: "Destination Port"
 *   0-31: "Sequence Number"
 *   +1: "URG"
 *   +1: "ACK"
 */
class PacketParser : DiagramParser {

    private val RE_START = Regex("^\\s*packet(-beta)?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_RANGE = Regex("^\\s*(\\d+)(?:\\s*-\\s*(\\d+))?\\s*:\\s*\"?([^\"]+)\"?\\s*$")
    private val RE_RELATIVE = Regex("^\\s*\\+(\\d+)\\s*:\\s*\"?([^\"]+)\"?\\s*$")
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val pdb = db as PacketDb
        pdb.clear()

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
                pdb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_TITLE.find(trimmed)?.let {
                pdb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                pdb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_RELATIVE.find(trimmed)?.let { m ->
                val bits = m.groupValues[1].toIntOrNull() ?: 1
                val label = m.groupValues[2].trim()
                pdb.addRelativeBlock(bits, label)
                return@let
            }?.also { continue }

            RE_RANGE.find(trimmed)?.let { m ->
                val start = m.groupValues[1].toIntOrNull() ?: 0
                val end = if (m.groupValues[2].isNotEmpty()) m.groupValues[2].toIntOrNull() ?: start else start
                val label = m.groupValues[3].trim()
                pdb.addBlock(start, end, label)
                return@let
            }
        }
    }
}
