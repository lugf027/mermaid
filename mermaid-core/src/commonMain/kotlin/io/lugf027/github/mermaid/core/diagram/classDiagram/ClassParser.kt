package io.lugf027.github.mermaid.core.diagram.classDiagram

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser
import io.lugf027.github.mermaid.core.util.Logger

/**
 * 类图解析器 - 对标 mermaid-js classDiagram.jison
 *
 * 手写递归下降解析器，支持:
 * - 类定义 (class ClassName)
 * - 类体 { members }
 * - 泛型 (ClassName~GenericType~)
 * - 关系 (A <|-- B, A *-- B 等)
 * - 基数 ("1" -- "*")
 * - 注解 (<<interface>>, <<abstract>>)
 * - 命名空间
 * - 样式 (classDef, cssClass, style)
 * - 注释 (note)
 * - direction
 */
class ClassParser : DiagramParser {

    private val log = Logger("ClassParser")

    // ── 关系语法正则 ─────────────────────────────────
    // 匹配: ClassName1 "cardinality1" [arrowLeft]lineType[arrowRight] "cardinality2" ClassName2 : label
    private val RE_RELATION = Regex(
        """^(\S+?)(?:\s+"([^"]*)")?\s+(<\||\|>|<|>|\*|o|\(\))?(-{2}|\.{2})((?:<\||>|\||o|\*|\(\))?)(?:\s+"([^"]*)")?\s+(\S+?)(?:\s*:\s*(.+?))?\s*$"""
    )

    // 类定义: class ClassName
    private val RE_CLASS_DEF = Regex(
        """^class\s+([\w~<>]+?)(?:~(.+?)~)?(?:\["(.+?)"\])?\s*$"""
    )

    // 类 + 样式: ClassName:::styleName
    private val RE_CLASS_STYLE = Regex(
        """^([\w]+):::(\w+)\s*$"""
    )

    // 注解: <<annotation>> ClassName
    private val RE_ANNOTATION = Regex(
        """^<<(.+?)>>\s+(\w+)\s*$"""
    )

    // note: note for ClassName "text" 或 note "text"
    private val RE_NOTE_FOR = Regex(
        """^note\s+(?:for\s+)?(\w+)\s+"(.+?)"\s*$""", RegexOption.IGNORE_CASE
    )
    private val RE_NOTE = Regex(
        """^note\s+"(.+?)"\s*$""", RegexOption.IGNORE_CASE
    )

    // classDef: classDef styleName attrs
    private val RE_CLASSDEF = Regex(
        """^classDef\s+(\w+)\s+(.+)$""", RegexOption.IGNORE_CASE
    )

    // cssClass: cssClass "class1,class2" styleName
    private val RE_CSSCLASS = Regex(
        """^cssClass\s+"(.+?)"\s+(\w+)\s*$""", RegexOption.IGNORE_CASE
    )

    // namespace
    private val RE_NAMESPACE = Regex(
        """^namespace\s+(\w+)\s*\{?\s*$""", RegexOption.IGNORE_CASE
    )

    override fun parse(text: String, db: DiagramDB) {
        val classDb = db as? ClassDb ?: throw IllegalArgumentException("Expected ClassDb")
        classDb.clear()

        val lines = text.lines()
        var i = 0

        // 跳过空行
        while (i < lines.size && lines[i].trim().isEmpty()) i++
        if (i >= lines.size) return

        // 第一行必须包含 "classDiagram"
        val headerLine = lines[i].trim()
        if (!headerLine.startsWith("classDiagram", ignoreCase = false)) {
            throw IllegalArgumentException("Class diagram must start with 'classDiagram'")
        }
        i++

        i = parseBlock(lines, i, classDb, null)
    }

    /**
     * 解析一个代码块（顶层或命名空间内部）
     */
    private fun parseBlock(lines: List<String>, startI: Int, db: ClassDb, namespaceId: String?): Int {
        var i = startI

        while (i < lines.size) {
            val line = lines[i].trim()
            i++

            if (line.isEmpty() || line.startsWith("%%")) continue
            if (line == "}") return i  // 结束当前块

            // 优先解析需要多行的语法（返回新的行号）
            val nsResult = parseNamespace(line, lines, i, db)
            if (nsResult != null) { i = nsResult; continue }

            val classBodyResult = parseClassWithBody(line, lines, i, db, namespaceId)
            if (classBodyResult != null) { i = classBodyResult; continue }

            when {
                parseDirection(line, db) -> continue
                parseTitle(line, db) -> continue
                parseAccTitle(line, db) -> continue
                parseAccDescr(line, db) -> continue
                parseClassDef(line, db, namespaceId) -> continue
                parseAnnotation(line, db) -> continue
                parseNoteFor(line, db) -> continue
                parseRelation(line, db) -> continue
                parseClassDefStyle(line, db) -> continue
                parseCssClass(line, db) -> continue
                parseClassStyle(line, db) -> continue
                else -> {
                    log.debug("Skipping unrecognized line: $line")
                }
            }
        }

        return i
    }

    // ════════════════════════════════════════════════════
    //  解析方法
    // ════════════════════════════════════════════════════

    private fun parseDirection(line: String, db: ClassDb): Boolean {
        val match = Regex("""^direction\s+(TB|BT|LR|RL)\s*$""", RegexOption.IGNORE_CASE).matchEntire(line) ?: return false
        db.setDirection(match.groupValues[1].uppercase())
        return true
    }

    private fun parseTitle(line: String, db: ClassDb): Boolean {
        if (!line.startsWith("title", ignoreCase = true)) return false
        if (line.length > 5 && !line[5].isWhitespace() && line[5] != ':') return false
        val text = line.substring(5).trim().removePrefix(":").trim()
        if (text.isNotEmpty()) db.setDiagramTitle(text)
        return true
    }

    private fun parseAccTitle(line: String, db: ClassDb): Boolean {
        if (!line.startsWith("accTitle", ignoreCase = true)) return false
        val colonIdx = line.indexOf(':')
        if (colonIdx >= 0) db.setAccTitle(line.substring(colonIdx + 1).trim())
        return true
    }

    private fun parseAccDescr(line: String, db: ClassDb): Boolean {
        if (!line.startsWith("accDescr", ignoreCase = true)) return false
        val colonIdx = line.indexOf(':')
        if (colonIdx >= 0) db.setAccDescription(line.substring(colonIdx + 1).trim())
        return true
    }

    /**
     * 解析命名空间块
     */
    private fun parseNamespace(line: String, lines: List<String>, currentI: Int, db: ClassDb): Int? {
        val match = RE_NAMESPACE.matchEntire(line) ?: return null
        val nsId = match.groupValues[1]
        db.addNamespace(nsId)
        // 如果当前行不含 {，看下一行
        var j = currentI
        if (!line.contains("{")) {
            while (j < lines.size && lines[j].trim().isEmpty()) j++
            if (j < lines.size && lines[j].trim() == "{") j++
        }
        j = parseBlock(lines, j, db, nsId)
        return j
    }

    /**
     * 解析 class 定义（可能带类体 { ... }）
     */
    private fun parseClassWithBody(line: String, lines: List<String>, currentI: Int, db: ClassDb, namespaceId: String?): Int? {
        if (!line.startsWith("class ")) return null

        val rest = line.removePrefix("class ").trim()

        // 检查是否有 { 开始的类体
        val braceIdx = rest.indexOf('{')
        if (braceIdx >= 0) {
            // class ClassName {
            val classDefPart = rest.substring(0, braceIdx).trim()
            val (className, generic, label) = parseClassNameParts(classDefPart)
            db.addClass(className, label, generic)
            if (namespaceId != null) db.addClassToNamespace(namespaceId, className)

            // 解析类体
            var j = currentI
            while (j < lines.size) {
                val memberLine = lines[j].trim()
                j++
                if (memberLine == "}") break
                if (memberLine.isEmpty() || memberLine.startsWith("%%")) continue
                db.addMember(className, memberLine)
            }
            return j
        }

        // 不带类体: class ClassName 或 class ClassName["Label"]
        val match = RE_CLASS_DEF.matchEntire(line)
        if (match != null) {
            val className = match.groupValues[1].replace("~.*~".toRegex(), "")
            val generic = match.groupValues[2].ifEmpty { null }
            val label = match.groupValues[3].ifEmpty { null }
            db.addClass(className, label, generic)
            if (namespaceId != null) db.addClassToNamespace(namespaceId, className)

            // 检查下一行是否有 {
            var j = currentI
            while (j < lines.size && lines[j].trim().isEmpty()) j++
            if (j < lines.size && lines[j].trim() == "{") {
                j++ // 跳过 {
                while (j < lines.size) {
                    val memberLine = lines[j].trim()
                    j++
                    if (memberLine == "}") break
                    if (memberLine.isEmpty() || memberLine.startsWith("%%")) continue
                    db.addMember(className, memberLine)
                }
                return j
            }
            return null  // 不带类体，返回 null 表示 i 不变
        }

        // 简单 class 声明
        val simpleParts = rest.split("\\s+".toRegex(), 2)
        val className = simpleParts[0].replace("~.*~".toRegex(), "")
        db.addClass(className)
        if (namespaceId != null) db.addClassToNamespace(namespaceId, className)
        return null
    }

    /**
     * 解析类名部分：ClassName~GenericType~["Label"]
     */
    private fun parseClassNameParts(text: String): Triple<String, String?, String?> {
        val labelMatch = Regex("""^(.+?)\["(.+?)"\]\s*$""").matchEntire(text)
        val base = if (labelMatch != null) labelMatch.groupValues[1].trim() else text.trim()
        val label = if (labelMatch != null) labelMatch.groupValues[2] else null

        val genericMatch = Regex("""^(.+?)~(.+?)~\s*$""").matchEntire(base)
        val className = if (genericMatch != null) genericMatch.groupValues[1].trim() else base
        val generic = if (genericMatch != null) genericMatch.groupValues[2] else null

        return Triple(className, generic, label)
    }

    /**
     * 简单 class 定义（无 body）
     */
    private fun parseClassDef(line: String, db: ClassDb, namespaceId: String?): Boolean {
        val match = RE_CLASS_DEF.matchEntire(line) ?: return false
        val className = match.groupValues[1].replace("~.*~".toRegex(), "")
        val generic = match.groupValues[2].ifEmpty { null }
        val label = match.groupValues[3].ifEmpty { null }
        db.addClass(className, label, generic)
        if (namespaceId != null) db.addClassToNamespace(namespaceId, className)
        return true
    }

    /**
     * 解析关系行
     */
    private fun parseRelation(line: String, db: ClassDb): Boolean {
        val match = RE_RELATION.matchEntire(line) ?: return false

        val id1 = cleanClassName(match.groupValues[1])
        val card1 = match.groupValues[2]  // 源端基数
        val arrowLeft = match.groupValues[3]
        val lineTypeStr = match.groupValues[4]
        val arrowRight = match.groupValues[5]
        val card2 = match.groupValues[6]  // 目标端基数
        val id2 = cleanClassName(match.groupValues[7])
        val title = match.groupValues[8].trim()

        val type1 = parseRelationEndType(arrowLeft, isLeft = true)
        val type2 = parseRelationEndType(arrowRight, isLeft = false)
        val lineType = if (lineTypeStr == "..") LineType.DOTTED_LINE else LineType.LINE

        db.addRelation(ClassRelation(
            id1 = id1,
            id2 = id2,
            relation = RelationSpec(type1, type2, lineType),
            title = title,
            relationTitle1 = card1,
            relationTitle2 = card2
        ))
        return true
    }

    /**
     * 解析关系端点类型
     */
    private fun parseRelationEndType(symbol: String, isLeft: Boolean): Int = when (symbol) {
        "<|", "|>" -> RelationType.EXTENSION
        "*" -> RelationType.COMPOSITION
        "o" -> RelationType.AGGREGATION
        "<", ">" -> RelationType.DEPENDENCY
        "()" -> RelationType.LOLLIPOP
        "|" -> RelationType.NONE
        else -> RelationType.NONE
    }

    /**
     * 解析注解: <<interface>> ClassName
     */
    private fun parseAnnotation(line: String, db: ClassDb): Boolean {
        val match = RE_ANNOTATION.matchEntire(line) ?: return false
        val annotation = match.groupValues[1]
        val className = match.groupValues[2]
        db.addClass(className)
        db.addAnnotation(className, annotation)
        return true
    }

    /**
     * 解析注释
     */
    private fun parseNoteFor(line: String, db: ClassDb): Boolean {
        val matchFor = RE_NOTE_FOR.matchEntire(line)
        if (matchFor != null) {
            db.addNote(matchFor.groupValues[2], matchFor.groupValues[1])
            return true
        }
        val matchNote = RE_NOTE.matchEntire(line)
        if (matchNote != null) {
            db.addNote(matchNote.groupValues[1])
            return true
        }
        return false
    }

    /**
     * 解析 classDef
     */
    private fun parseClassDefStyle(line: String, db: ClassDb): Boolean {
        val match = RE_CLASSDEF.matchEntire(line) ?: return false
        val styleName = match.groupValues[1]
        val attrs = match.groupValues[2].split(",").map { it.trim() }
        db.addStyleClass(styleName, attrs)
        return true
    }

    /**
     * 解析 cssClass
     */
    private fun parseCssClass(line: String, db: ClassDb): Boolean {
        val match = RE_CSSCLASS.matchEntire(line) ?: return false
        val classIds = match.groupValues[1].split(",").map { it.trim() }
        val styleId = match.groupValues[2]
        db.applyStyleClass(classIds, styleId)
        return true
    }

    /**
     * 解析 ClassName:::styleName
     */
    private fun parseClassStyle(line: String, db: ClassDb): Boolean {
        val match = RE_CLASS_STYLE.matchEntire(line) ?: return false
        val classId = match.groupValues[1]
        val styleId = match.groupValues[2]
        db.addClass(classId)
        db.applyStyleClass(listOf(classId), styleId)
        return true
    }

    private fun cleanClassName(name: String): String {
        return name.trim()
            .removeSurrounding("\"")
            .replace("~.*~".toRegex(), "")
    }
}
