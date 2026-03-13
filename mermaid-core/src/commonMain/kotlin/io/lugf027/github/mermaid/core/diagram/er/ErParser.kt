package io.lugf027.github.mermaid.core.diagram.er

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser
import io.lugf027.github.mermaid.core.util.Logger

/**
 * ER 图解析器 - 对标 mermaid-js erDiagram.jison
 *
 * 手写递归下降解析器，支持:
 * - 实体定义（简单 / 带属性块 / 带别名）
 * - 属性（type name [PK|FK|UK] ["comment"]）
 * - 关系（||--o{ 等符号 + : role_label）
 * - direction / title / accTitle / accDescr
 */
class ErParser : DiagramParser {

    private val log = Logger("ErParser")

    // ── 关系符号正则 ─────────────────────────────────

    // 匹配完整关系行: Entity1 <cardA><relType><cardB> Entity2 : label
    private val RE_RELATIONSHIP = Regex(
        """^("?[\w\s-]+"?)\s+([|o}]{1,2})(--|\.\.|\.-)([|o{]{1,2})\s+("?[\w\s-]+"?)(?:\s*:\s*(.+?))?\s*$"""
    )

    override fun parse(text: String, db: DiagramDB) {
        val erDb = db as? ErDb ?: throw IllegalArgumentException("Expected ErDb")
        erDb.clear()

        val lines = text.lines()
        var i = 0

        // 跳过空行
        while (i < lines.size && lines[i].trim().isEmpty()) i++
        if (i >= lines.size) return

        // 第一行必须包含 "erDiagram"
        val headerLine = lines[i].trim()
        if (!headerLine.startsWith("erDiagram", ignoreCase = false)) {
            throw IllegalArgumentException("ER diagram must start with 'erDiagram'")
        }
        i++

        // 逐行解析
        while (i < lines.size) {
            val line = lines[i].trim()
            i++

            if (line.isEmpty() || line.startsWith("%%")) continue

            // 优先解析需要多行的语法
            val entityBlockResult = parseEntityBlock(line, lines, i, erDb)
            if (entityBlockResult != null) { i = entityBlockResult; continue }

            when {
                parseDirection(line, erDb) -> continue
                parseTitle(line, erDb) -> continue
                parseAccTitle(line, erDb) -> continue
                parseAccDescr(line, erDb) -> continue
                parseRelationship(line, erDb) -> continue
                parseEntityAlias(line, erDb) -> continue
                parseEntitySimple(line, erDb) -> continue
                else -> {
                    log.debug("Skipping unrecognized line: $line")
                }
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  解析方法
    // ════════════════════════════════════════════════════

    private fun parseDirection(line: String, db: ErDb): Boolean {
        val match = Regex("""^direction\s+(TB|BT|LR|RL)\s*$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.setDirection(match.groupValues[1].uppercase())
        return true
    }

    private fun parseTitle(line: String, db: ErDb): Boolean {
        if (!line.startsWith("title", ignoreCase = true)) return false
        if (line.length > 5 && !line[5].isWhitespace() && line[5] != ':') return false
        val text = line.substring(5).trim().removePrefix(":").trim()
        if (text.isNotEmpty()) db.setDiagramTitle(text)
        return true
    }

    private fun parseAccTitle(line: String, db: ErDb): Boolean {
        if (!line.startsWith("accTitle", ignoreCase = true)) return false
        val colonIdx = line.indexOf(':')
        if (colonIdx >= 0) db.setAccTitle(line.substring(colonIdx + 1).trim())
        return true
    }

    private fun parseAccDescr(line: String, db: ErDb): Boolean {
        if (!line.startsWith("accDescr", ignoreCase = true)) return false
        val colonIdx = line.indexOf(':')
        if (colonIdx >= 0) db.setAccDescription(line.substring(colonIdx + 1).trim())
        return true
    }

    /**
     * 解析关系行: CUSTOMER ||--o{ ORDER : places
     */
    private fun parseRelationship(line: String, db: ErDb): Boolean {
        val match = RE_RELATIONSHIP.matchEntire(line) ?: return false

        val entityA = match.groupValues[1].trim().removeSurrounding("\"")
        val leftSymbol = match.groupValues[2]
        val relTypeSymbol = match.groupValues[3]
        val rightSymbol = match.groupValues[4]
        val entityB = match.groupValues[5].trim().removeSurrounding("\"")
        val roleLabel = match.groupValues[6].trim()

        val cardA = parseCardinality(leftSymbol, isLeft = true)
        val cardB = parseCardinality(rightSymbol, isLeft = false)
        val relType = when (relTypeSymbol) {
            "--" -> Identification.IDENTIFYING
            ".." -> Identification.NON_IDENTIFYING
            else -> Identification.NON_IDENTIFYING
        }

        db.addRelationship(entityA, entityB, RelSpec(cardA, cardB, relType), roleLabel)
        return true
    }

    /**
     * 解析基数符号
     */
    private fun parseCardinality(symbol: String, isLeft: Boolean): String {
        return when (symbol) {
            "||" -> Cardinality.ONLY_ONE
            "|o", "o|" -> Cardinality.ZERO_OR_ONE
            "}|", "|{" -> Cardinality.ONE_OR_MORE
            "}o", "o{" -> Cardinality.ZERO_OR_MORE
            else -> Cardinality.ONLY_ONE
        }
    }

    /**
     * 解析带属性块的实体：
     * ENTITY_NAME {
     *     type name [PK|FK|UK] ["comment"]
     * }
     */
    private fun parseEntityBlock(line: String, lines: List<String>, currentI: Int, db: ErDb): Int? {
        // 检查是否是实体名 + {
        val blockMatch = Regex("""^("?[\w\s-]+"?)\s*\{""").matchEntire(line) ?: return null

        val entityName = blockMatch.groupValues[1].trim().removeSurrounding("\"")
        db.addEntity(entityName)

        var j = currentI
        while (j < lines.size) {
            val attrLine = lines[j].trim()
            j++

            if (attrLine == "}") break
            if (attrLine.isEmpty() || attrLine.startsWith("%%")) continue

            parseAttribute(attrLine, entityName, db)
        }
        return j
    }

    /**
     * 解析属性行: type name [PK|FK|UK] ["comment"]
     */
    private fun parseAttribute(line: String, entityName: String, db: ErDb) {
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    sb.append(ch)
                }
                ch.isWhitespace() && !inQuotes -> {
                    if (sb.isNotEmpty()) {
                        parts.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) parts.add(sb.toString())

        if (parts.size < 2) return

        val type = parts[0]
        val name = parts[1]
        val keys = mutableListOf<String>()
        var comment = ""

        for (idx in 2 until parts.size) {
            val part = parts[idx].uppercase()
            when (part) {
                "PK", "FK", "UK" -> keys.add(part)
                else -> {
                    // 可能是注释（带引号）
                    val raw = parts[idx].removeSurrounding("\"")
                    if (raw.isNotEmpty()) comment = raw
                }
            }
        }

        db.addAttribute(entityName, Attribute(type, name, keys, comment))
    }

    /**
     * 解析带别名的实体: ENTITY["Label"]
     */
    private fun parseEntityAlias(line: String, db: ErDb): Boolean {
        val match = Regex("""^([\w-]+)\["(.+)"\]\s*$""").matchEntire(line) ?: return false
        db.addEntity(match.groupValues[1], match.groupValues[2])
        return true
    }

    /**
     * 解析简单实体声明（单独一行的实体名）
     */
    private fun parseEntitySimple(line: String, db: ErDb): Boolean {
        val match = Regex("""^"?([\w-]+)"?\s*$""").matchEntire(line) ?: return false
        val name = match.groupValues[1]
        // 排除关键字
        if (name.lowercase() in setOf("erdiagram", "direction", "title", "acctitle", "accdescr")) return false
        db.addEntity(name)
        return true
    }
}
