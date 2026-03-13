package io.lugf027.github.mermaid.core.diagram.packet

import io.lugf027.github.mermaid.core.diagram.DiagramDefinition

/**
 * 数据包图 DiagramDefinition 组装
 */
object PacketDiagram {

    fun definition(): DiagramDefinition = DiagramDefinition(
        id = "packet",
        detector = { text ->
            Regex("^\\s*packet(-beta)?\\b").containsMatchIn(text)
        },
        dbFactory = { PacketDb() },
        parser = PacketParser(),
        renderer = PacketRenderer(),
        styles = { tv ->
            buildString {
                appendLine(".packetBlock { fill: ${tv.mainBkg}; stroke: ${tv.lineColor}; }")
                appendLine(".packetLabel { fill: ${tv.textColor}; font-size: 12px; }")
                appendLine(".packetByte { fill: ${tv.textColor}; font-size: 10px; }")
                appendLine(".packetTitle { fill: ${tv.textColor}; font-size: 14px; }")
            }
        }
    )
}
