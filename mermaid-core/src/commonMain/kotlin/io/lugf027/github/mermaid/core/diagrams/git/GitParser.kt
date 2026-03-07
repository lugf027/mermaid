package io.lugf027.github.mermaid.core.diagrams.git

import io.lugf027.github.mermaid.core.types.ParserDefinition

/**
 * Git 图解析器。
 */
class GitParser(private val db: GitDb) : ParserDefinition {

    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0

        // 跳过 gitGraph 关键字行
        if (i < lines.size) {
            val first = lines[i].trim()
            val lower = first.lowercase()
            if (lower.startsWith("gitgraph")) {
                // 可能有方向 "gitGraph LR:"
                val parts = first.split(Regex("[:\\s]+")).filter { it.isNotEmpty() }
                if (parts.size >= 2) {
                    val dir = parts[1].uppercase()
                    if (dir in listOf("LR", "TB", "BT")) db.setOrientation(dir)
                }
                i++
            }
        }

        while (i < lines.size) {
            val line = lines[i].trim()
            i++
            if (line.isEmpty() || line.startsWith("%%")) continue

            val lower = line.lowercase()
            when {
                lower.startsWith("commit") -> parseCommit(line)
                lower.startsWith("branch ") -> db.branch(line.substringAfter(" ").trim())
                lower.startsWith("checkout ") -> db.checkout(line.substringAfter(" ").trim())
                lower.startsWith("switch ") -> db.checkout(line.substringAfter(" ").trim())
                lower.startsWith("merge ") -> parseMerge(line)
                lower.startsWith("cherry-pick") -> parseCherryPick(line)
                lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())
                lower.startsWith("accdescr:") -> db.setAccDescription(line.substringAfter(":").trim())
            }
        }
    }

    private fun parseCommit(line: String) {
        var id: String? = null
        var message = ""
        var type = CommitType.NORMAL
        var tag: String? = null

        val idMatch = Regex("""id:\s*"([^"]+)"""").find(line)
        if (idMatch != null) id = idMatch.groupValues[1]

        val msgMatch = Regex("""msg:\s*"([^"]+)"""").find(line)
        if (msgMatch != null) message = msgMatch.groupValues[1]

        val tagMatch = Regex("""tag:\s*"([^"]+)"""").find(line)
        if (tagMatch != null) tag = tagMatch.groupValues[1]

        val typeMatch = Regex("""type:\s*(\w+)""").find(line)
        if (typeMatch != null) {
            type = when (typeMatch.groupValues[1].uppercase()) {
                "REVERSE" -> CommitType.REVERSE
                "HIGHLIGHT" -> CommitType.HIGHLIGHT
                else -> CommitType.NORMAL
            }
        }

        db.commit(id, message, type, tag)
    }

    private fun parseMerge(line: String) {
        val rest = line.substringAfter("merge ").trim()
        val branchName = rest.split(Regex("\\s+")).first()
        var id: String? = null
        var tag: String? = null

        val idMatch = Regex("""id:\s*"([^"]+)"""").find(rest)
        if (idMatch != null) id = idMatch.groupValues[1]
        val tagMatch = Regex("""tag:\s*"([^"]+)"""").find(rest)
        if (tagMatch != null) tag = tagMatch.groupValues[1]

        db.merge(branchName, id, tag)
    }

    private fun parseCherryPick(line: String) {
        val idMatch = Regex("""id:\s*"([^"]+)"""").find(line)
        if (idMatch != null) {
            db.cherryPick(idMatch.groupValues[1])
        }
    }
}
