package io.lugf027.github.mermaid.core.diagrams.architecture

import io.lugf027.github.mermaid.core.types.ParserDefinition

class ArchitectureParser(private val db: ArchitectureDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("architecture")) i++
        val groupStack = mutableListOf<String>()

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("group ") -> {
                    val parts = line.substringAfter("group ").trim()
                    val id = parts.substringBefore("(").substringBefore("[").trim()
                    val title = if (parts.contains("[")) parts.substringAfter("[").substringBefore("]") else ""
                    db.addGroup(ArchGroup(id, title, parentId = groupStack.lastOrNull()))
                    groupStack.add(id)
                }
                lower.startsWith("service ") -> {
                    val rest = line.substringAfter("service ").trim()
                    val id = rest.substringBefore("(").substringBefore("[").trim()
                    val icon = if (rest.contains("(")) rest.substringAfter("(").substringBefore(")") else null
                    val title = if (rest.contains("[")) rest.substringAfter("[").substringBefore("]") else ""
                    val groupId = if (rest.contains(" in ")) rest.substringAfter(" in ").trim() else groupStack.lastOrNull()
                    db.addNode(ArchNode(id, ArchNodeType.SERVICE, title, icon, groupId))
                }
                lower.startsWith("junction ") -> {
                    val rest = line.substringAfter("junction ").trim()
                    val id = rest.substringBefore(" ").trim()
                    val groupId = if (rest.contains(" in ")) rest.substringAfter(" in ").trim() else groupStack.lastOrNull()
                    db.addNode(ArchNode(id, ArchNodeType.JUNCTION, groupId = groupId))
                }
                line == "end" || line == "}" -> { if (groupStack.isNotEmpty()) groupStack.removeAt(groupStack.lastIndex) }
                line.contains(":") && (line.contains("--") || line.contains("-->")) -> {
                    val arrow = line.contains("-->")
                    val sep = if (arrow) "-->" else "--"
                    val parts = line.split(sep).map { it.trim() }
                    if (parts.size >= 2) {
                        val lhs = parts[0].split(":"); val rhs = parts[1].split(":")
                        db.addEdge(ArchEdge(
                            lhsId = lhs[0].trim(), lhsDir = lhs.getOrElse(1) { "R" }.trim(),
                            rhsId = rhs.getOrElse(1) { rhs[0] }.trim(), rhsDir = rhs.getOrElse(0) { "L" }.trim(),
                            arrow = arrow
                        ))
                    }
                }
            }
        }
    }
}
