package io.lugf027.github.mermaid.core.diagrams.requirement

import io.lugf027.github.mermaid.core.types.ParserDefinition

class RequirementParser(private val db: RequirementDb) : ParserDefinition {
    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0
        if (i < lines.size && lines[i].trim().lowercase().startsWith("requirement")) i++

        while (i < lines.size) {
            val line = lines[i].trim(); i++
            if (line.isEmpty() || line.startsWith("%%")) continue
            val lower = line.lowercase()

            when {
                lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())
                lower.startsWith("accdescr:") -> db.setAccDescription(line.substringAfter(":").trim())

                // requirement/functionalRequirement/etc "Name" {
                (lower.contains("requirement ") || lower.contains("interfacerequirement ") || lower.contains("performancerequirement ")) && line.contains("{") -> {
                    val type = line.substringBefore("\"").trim().lowercase()
                    val name = line.substringAfter("\"").substringBefore("\"")
                    var id = ""; var text = ""; var risk = ""; var verify = ""
                    while (i < lines.size) {
                        val bl = lines[i].trim(); i++
                        if (bl == "}") break
                        when {
                            bl.lowercase().startsWith("id:") -> id = bl.substringAfter(":").trim()
                            bl.lowercase().startsWith("text:") -> text = bl.substringAfter(":").trim().removeSurrounding("\"")
                            bl.lowercase().startsWith("risk:") -> risk = bl.substringAfter(":").trim()
                            bl.lowercase().startsWith("verifymethod:") -> verify = bl.substringAfter(":").trim()
                        }
                    }
                    db.addRequirement(Requirement(name, type, id, text, risk, verify))
                }

                lower.startsWith("element ") && line.contains("{") -> {
                    val name = line.substringAfter("\"").substringBefore("\"")
                    var type = ""; var docRef = ""
                    while (i < lines.size) {
                        val bl = lines[i].trim(); i++
                        if (bl == "}") break
                        when {
                            bl.lowercase().startsWith("type:") -> type = bl.substringAfter(":").trim().removeSurrounding("\"")
                            bl.lowercase().startsWith("docref:") -> docRef = bl.substringAfter(":").trim().removeSurrounding("\"")
                        }
                    }
                    db.addElement(Element(name, type, docRef))
                }

                line.contains("- ") && line.contains(" ->") -> {
                    val match = Regex("""(\S+)\s+-\s+(\w+)\s+->\s+(\S+)""").find(line)
                    if (match != null) {
                        db.addRelation(RequirementRelation(match.groupValues[2], match.groupValues[1], match.groupValues[3]))
                    }
                }
            }
        }
    }
}
