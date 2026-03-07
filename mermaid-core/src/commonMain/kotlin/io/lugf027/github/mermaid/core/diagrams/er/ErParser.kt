package io.lugf027.github.mermaid.core.diagrams.er

import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * ER 图递归下降解析器。
 * 语法示例：
 * ```
 * erDiagram
 *     CUSTOMER ||--o{ ORDER : places
 *     CUSTOMER { string name PK "comment" }
 * ```
 */
class ErParser(private val db: ErDb) : ParserDefinition {

    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0

        // 跳过 erDiagram 关键字
        if (i < lines.size && lines[i].trim().lowercase().startsWith("erdiagram")) i++

        while (i < lines.size) {
            val line = lines[i].trim()
            i++

            if (line.isEmpty() || line.startsWith("%%")) continue

            val lower = line.lowercase()
            when {
                lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())
                lower.startsWith("accdescr:") -> db.setAccDescription(line.substringAfter(":").trim())
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())

                // 实体属性块开始 "ENTITY {"
                line.endsWith("{") -> {
                    val entityId = line.substringBefore("{").trim()
                    db.addEntity(entityId)
                    // 读取属性直到 "}"
                    while (i < lines.size) {
                        val attrLine = lines[i].trim()
                        i++
                        if (attrLine == "}") break
                        if (attrLine.isEmpty()) continue
                        val parts = attrLine.split(Regex("\\s+"), limit = 4)
                        if (parts.size >= 2) {
                            val type = parts[0]
                            val name = parts[1]
                            val keys = mutableListOf<String>()
                            var comment = ""
                            for (j in 2 until parts.size) {
                                val p = parts[j].uppercase()
                                if (p == "PK" || p == "FK" || p == "UK") keys.add(p)
                                else if (parts[j].startsWith("\"")) comment = parts[j].removeSurrounding("\"")
                            }
                            db.addAttribute(entityId, ErAttribute(type, name, keys, comment))
                        }
                    }
                }

                // 关系行：ENTITY_A ||--o{ ENTITY_B : label
                line.contains("--") && line.contains(":") -> {
                    parseRelationship(line)
                }

                // 简单实体引用
                else -> {
                    val id = line.split(Regex("\\s+")).first()
                    if (id.isNotEmpty()) db.addEntity(id)
                }
            }
        }
    }

    private fun parseRelationship(line: String) {
        // 格式: ENTITY_A <card>--<card> ENTITY_B : label
        val relRegex = Regex(
            """(\S+)\s+(\|o|o\||o\{|\{o|\|\{|\{\||\|\||}\||\|})(-{1,2})(o\||o\{|\|o|\{o|\|\{|\{\||\|\||}\||\|})?\s+(\S+)\s*:\s*(.*)"""
        )
        val match = relRegex.find(line)
        if (match != null) {
            val entityA = match.groupValues[1]
            val entityB = match.groupValues[5]
            val label = match.groupValues[6].trim()
            val cardAStr = match.groupValues[2]
            val cardBStr = match.groupValues[4].ifEmpty { "||" }
            val dash = match.groupValues[3]

            db.addRelationship(
                ErRelationship(
                    entityA = entityA,
                    entityB = entityB,
                    roleLabel = label,
                    cardA = parseCardinality(cardAStr),
                    cardB = parseCardinality(cardBStr),
                    identification = if (dash == "--") Identification.IDENTIFYING else Identification.NON_IDENTIFYING,
                )
            )
        }
    }

    private fun parseCardinality(s: String): Cardinality = when (s) {
        "o|", "|o" -> Cardinality.ZERO_OR_ONE
        "o{", "{o" -> Cardinality.ZERO_OR_MORE
        "|{", "{|" -> Cardinality.ONE_OR_MORE
        "||" -> Cardinality.ONLY_ONE
        "}|", "|}" -> Cardinality.MD_PARENT
        else -> Cardinality.ONLY_ONE
    }
}
