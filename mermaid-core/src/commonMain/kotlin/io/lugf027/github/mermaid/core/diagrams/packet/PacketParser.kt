package io.lugf027.github.mermaid.core.diagrams.packet

import io.lugf027.github.mermaid.core.types.ParserDefinition

class PacketParser(private val db: PacketDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("packet")) i++
        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            // Format: 0-15 : "Label" or 0-15 : Label
            val match = Regex("""(\d+)-(\d+)\s*:\s*(.+)""").find(line)
            if (match != null) {
                val start = match.groupValues[1].toInt()
                val end = match.groupValues[2].toInt()
                val label = match.groupValues[3].trim().removeSurrounding("\"")
                db.addBlock(PacketBlock(start, end, label))
            }
        }
    }
}
