package io.lugf027.github.mermaid.core.diagrams.c4

import io.lugf027.github.mermaid.core.types.ParserDefinition

class C4Parser(private val db: C4Db) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size) {
            val first = lines[i].trim()
            if (first.startsWith("C4")) { db.setC4Type(first); i++ }
        }

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()

            when {
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())
                lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())

                line.startsWith("Person(") || line.startsWith("Person_Ext(") -> parseShape(line, if (line.startsWith("Person_Ext")) C4ShapeType.PERSON_EXT else C4ShapeType.PERSON)
                line.startsWith("System(") || line.startsWith("System_Ext(") -> parseShape(line, if (line.startsWith("System_Ext")) C4ShapeType.SYSTEM_EXT else C4ShapeType.SYSTEM)
                line.startsWith("Container(") -> parseShape(line, C4ShapeType.CONTAINER)
                line.startsWith("ContainerDb(") -> parseShape(line, C4ShapeType.CONTAINER_DB)
                line.startsWith("ContainerQueue(") -> parseShape(line, C4ShapeType.CONTAINER_QUEUE)
                line.startsWith("Component(") -> parseShape(line, C4ShapeType.COMPONENT)
                line.startsWith("SystemDb(") -> parseShape(line, C4ShapeType.SYSTEM_DB)
                line.startsWith("SystemQueue(") -> parseShape(line, C4ShapeType.SYSTEM_QUEUE)

                line.startsWith("Rel(") || line.startsWith("Rel_D(") || line.startsWith("Rel_U(") ||
                    line.startsWith("Rel_L(") || line.startsWith("Rel_R(") || line.startsWith("BiRel(") -> parseRel(line)

                (line.contains("_Boundary(") || line.contains("Boundary(")) && line.contains("{") -> {
                    parseBoundary(line)
                }

                line == "}" -> db.popBoundary()
            }
        }
    }

    private fun parseShape(line: String, type: C4ShapeType) {
        val args = extractArgs(line)
        if (args.size >= 2) {
            db.addShape(C4Shape(
                alias = args[0],
                label = args[1],
                description = args.getOrElse(2) { "" },
                technology = args.getOrElse(3) { "" },
                type = type,
            ))
        }
    }

    private fun parseRel(line: String) {
        val args = extractArgs(line)
        if (args.size >= 3) {
            db.addRel(C4Rel(from = args[0], to = args[1], label = args[2], technology = args.getOrElse(3) { "" }))
        }
    }

    private fun parseBoundary(line: String) {
        val beforeBrace = line.substringBefore("{").trim()
        val args = extractArgs(beforeBrace)
        val alias = args.getOrElse(0) { "boundary_${line.hashCode()}" }
        val label = args.getOrElse(1) { alias }
        db.addBoundary(C4Boundary(alias = alias, label = label))
        db.pushBoundary(alias)
    }

    private fun extractArgs(line: String): List<String> {
        val start = line.indexOf('(')
        val end = line.lastIndexOf(')')
        if (start < 0 || end <= start) return emptyList()
        return line.substring(start + 1, end).split(",").map { it.trim().removeSurrounding("\"") }
    }
}
