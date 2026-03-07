package io.lugf027.github.mermaid.core.diagrams.classdiagram

import io.lugf027.github.mermaid.core.types.ParserDefinition
import io.lugf027.github.mermaid.core.utils.Logger

/**
 * 类图递归下降解析器。
 * 解析 classDiagram 语法，填充 ClassDb。
 */
class ClassParser(private val db: ClassDb) : ParserDefinition {

    private val tag = "ClassParser"

    override fun parse(input: String) {
        val lines = input.lines()
        var i = 0

        // 跳过 classDiagram 关键字
        if (i < lines.size) {
            val first = lines[i].trim()
            if (first.startsWith("classDiagram", ignoreCase = true)) i++
        }

        var currentNamespace: String? = null
        var currentClassId: String? = null
        var braceDepth = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            i++

            if (line.isEmpty() || line.startsWith("%%")) continue

            val lower = line.lowercase()

            when {
                lower.startsWith("direction ") -> db.setDirection(line.substringAfter(" ").trim())
                lower.startsWith("acctitle:") -> db.setAccTitle(line.substringAfter(":").trim())
                lower.startsWith("accdescr:") -> db.setAccDescription(line.substringAfter(":").trim())
                lower.startsWith("title ") -> db.setDiagramTitle(line.substringAfter("title ").trim())

                // namespace 块
                lower.startsWith("namespace ") && line.contains("{") -> {
                    val nsName = line.substringAfter("namespace ").substringBefore("{").trim()
                    db.addNamespace(nsName)
                    currentNamespace = nsName
                    braceDepth++
                }

                // class 定义带花括号
                lower.startsWith("class ") && line.contains("{") -> {
                    val rest = line.substringAfter("class ").substringBefore("{").trim()
                    val classId = parseClassHeader(rest)
                    currentClassId = classId
                    braceDepth++
                }

                // class 单行定义
                lower.startsWith("class ") && !line.contains("{") -> {
                    val rest = line.substringAfter("class ").trim()
                    parseClassHeader(rest)
                    if (currentNamespace != null) {
                        val cid = rest.split(" ", "~", ":::").first().trim()
                        db.addClassToNamespace(currentNamespace, cid)
                    }
                }

                // 花括号关闭
                line == "}" -> {
                    braceDepth--
                    if (currentClassId != null && braceDepth <= (if (currentNamespace != null) 1 else 0)) {
                        if (currentNamespace != null) {
                            db.addClassToNamespace(currentNamespace, currentClassId)
                        }
                        currentClassId = null
                    }
                    if (braceDepth == 0) {
                        currentNamespace = null
                    }
                }

                // 注解
                line.startsWith("<<") && line.endsWith(">>") && currentClassId != null -> {
                    db.addAnnotation(currentClassId, line.removeSurrounding("<<", ">>"))
                }

                // 类体内的成员
                currentClassId != null && braceDepth > 0 -> {
                    if (line != "{" && line != "}") {
                        db.addMember(currentClassId, line)
                    }
                }

                // classDef
                lower.startsWith("classdef ") -> { /* 忽略样式定义 */ }

                // 注解独立行
                lower.startsWith("<<") && lower.contains(">>") -> {
                    val annotation = line.substringAfter("<<").substringBefore(">>")
                    val className = line.substringAfter(">>").trim()
                    if (className.isNotEmpty()) {
                        db.addClass(className)
                        db.addAnnotation(className, annotation)
                    }
                }

                // 关系行
                else -> tryParseRelation(line)
            }
        }
    }

    /**
     * 解析 class 头部，返回 classId。
     */
    private fun parseClassHeader(rest: String): String {
        // 处理泛型 class ClassName~T~
        val genericMatch = Regex("""(\w+)~(\w+)~""").find(rest)
        if (genericMatch != null) {
            val id = genericMatch.groupValues[1]
            db.addClass(id)
            return id
        }

        // 处理 class ClassName["label"]
        val labelMatch = Regex("""(\w+)\["([^"]+)"]""").find(rest)
        if (labelMatch != null) {
            val id = labelMatch.groupValues[1]
            val label = labelMatch.groupValues[2]
            db.addClass(id, label)
            return id
        }

        // 简单: class ClassName
        val id = rest.split(" ", ":::").first().trim()
        db.addClass(id)

        // ::: 样式
        if (rest.contains(":::")) {
            val style = rest.substringAfter(":::").trim()
            // 样式应用暂忽略
        }

        return id
    }

    /**
     * 尝试解析关系行。
     * 格式: ClassA <|-- ClassB : label
     */
    private fun tryParseRelation(line: String) {
        // 关系线正则：两端类名 + 关系操作符
        val relRegex = Regex(
            """^(\S+)\s+(?:"([^"]*)")?\s*(<\||\*|o|<|>|\(\))?(--|\.\.)(>|\|>|\*|o|\(\))?\s*(?:"([^"]*)")?\s+(\S+)\s*(?::\s*(.*))?$"""
        )
        val match = relRegex.find(line) ?: return

        val id1 = match.groupValues[1].trim()
        val card1 = match.groupValues[2].trim()
        val rel1Str = match.groupValues[3].trim()
        val lineStr = match.groupValues[4].trim()
        val rel2Str = match.groupValues[5].trim()
        val card2 = match.groupValues[6].trim()
        val id2 = match.groupValues[7].trim()
        val title = match.groupValues[8].trim()

        val lineType = if (lineStr == "..") ClassLineType.DOTTED_LINE else ClassLineType.LINE
        val type1 = parseRelEndType(rel1Str)
        val type2 = parseRelEndType(rel2Str)

        db.addRelation(ClassRelation(
            id1 = id1, id2 = id2,
            relationTitle1 = card1, relationTitle2 = card2,
            title = title,
            relation = RelationDetail(type1 = type1, type2 = type2, lineType = lineType),
        ))
    }

    private fun parseRelEndType(s: String): RelationType? = when (s) {
        "<|", "|>" -> RelationType.EXTENSION
        "*" -> RelationType.COMPOSITION
        "o" -> RelationType.AGGREGATION
        "<", ">" -> RelationType.DEPENDENCY
        "()" -> RelationType.LOLLIPOP
        else -> null
    }
}
