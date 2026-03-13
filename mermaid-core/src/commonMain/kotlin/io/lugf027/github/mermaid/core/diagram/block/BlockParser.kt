package io.lugf027.github.mermaid.core.diagram.block

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 块图解析器 - 对标 mermaid-js block.jison + blockDB.ts
 *
 * 支持语法：
 *   block-beta
 *   columns N
 *   id["Label"]:N         (节点声明，:N 跨列)
 *   id(("Label"))         (圆形)
 *   id{"Label"}           (菱形)
 *   id{{"Label"}}         (六边形)
 *   id>"Label"]           (信号形)
 *   id[/"Label"/]         (平行四边形)
 *   id[\"Label"\]         (反向平行四边形)
 *   id[/"Label"\]         (梯形)
 *   id[\"Label"/]         (反梯形)
 *   space[:N]             (空白)
 *   block:N               (复合块开始，:N=列数)
 *     ...
 *   end                   (复合块结束)
 *   id1 --> id2           (边)
 *   id1 -- "label" --> id2
 *   classDef className fill:#f00,stroke:#333
 *   class id1,id2 className
 *   style id fill:#f00
 */
class BlockParser : DiagramParser {

    // 图表类型声明
    private val RE_BLOCK_BETA = Regex("^\\s*block(-beta)?\\s*$", RegexOption.IGNORE_CASE)

    // columns
    private val RE_COLUMNS = Regex("^\\s*columns\\s+(\\d+|auto)\\s*$", RegexOption.IGNORE_CASE)

    // space[:N]
    private val RE_SPACE = Regex("^\\s*space(?::(\\d+))?\\s*$", RegexOption.IGNORE_CASE)

    // block[:N] (复合块开始)
    private val RE_BLOCK_START = Regex("^\\s*(\\S+)\\s+block(?::(\\d+))?\\s*$", RegexOption.IGNORE_CASE)

    // end
    private val RE_END = Regex("^\\s*end\\s*$", RegexOption.IGNORE_CASE)

    // 边：id1 --> id2  或  id1 -- "label" --> id2
    private val RE_EDGE = Regex(
        "^\\s*(\\S+)\\s+(-->|---|-\\.->|\\.->|==>|--[\\s\"]+(.+?)[\"\\s]*-->)\\s+(\\S+)\\s*$"
    )

    // classDef
    private val RE_CLASSDEF = Regex("^\\s*classDef\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE)

    // class
    private val RE_CLASS = Regex("^\\s*class\\s+(.+?)\\s+(\\S+)\\s*$", RegexOption.IGNORE_CASE)

    // style
    private val RE_STYLE = Regex("^\\s*style\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE)

    // 标题/无障碍
    private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    // 节点声明正则（各种形状）
    // 匹配 id["label"]:N  或  id:N  或  id  以及各种括号形状
    private val RE_NODE = Regex(
        """^\s*(\S+?)(?:\[\"(.+?)\"\]|\["(.+?)"\]|\(\"(.+?)\"\)|\("(.+?)"\)|\(\[\"?(.+?)\"?\]\)|\(\(\"?(.+?)\"?\)\)|\{\"?(.+?)\"?\}|\{\{\"?(.+?)\"?\}\}|>\"?(.+?)\"?\]|\[/\"?(.+?)\"?/\]|\[\\\"?(.+?)\"?\\]|\[/\"?(.+?)\"?\\\]|\[\\\"?(.+?)\"?/\])?(?::(\d+))?\s*$"""
    )

    override fun parse(text: String, db: DiagramDB) {
        val blockDb = db as BlockDb
        blockDb.clear()

        val lines = text.lines()
        var started = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            // 图表声明
            if (!started) {
                if (RE_BLOCK_BETA.matches(trimmed)) {
                    started = true
                }
                continue
            }

            // 标题/无障碍
            RE_TITLE.find(trimmed)?.let {
                blockDb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_TITLE.find(trimmed)?.let {
                blockDb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                blockDb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            // columns
            RE_COLUMNS.find(trimmed)?.let { m ->
                val colStr = m.groupValues[1]
                val cols = if (colStr.equals("auto", ignoreCase = true)) -1 else colStr.toIntOrNull() ?: -1
                blockDb.setColumns(cols)
                return@let
            }?.also { continue }

            // space
            RE_SPACE.find(trimmed)?.let { m ->
                val span = m.groupValues[1].toIntOrNull() ?: 1
                blockDb.addSpace(span)
                return@let
            }?.also { continue }

            // end
            if (RE_END.matches(trimmed)) {
                blockDb.endComposite()
                continue
            }

            // block start:  id block[:N]
            RE_BLOCK_START.find(trimmed)?.let { m ->
                val id = m.groupValues[1]
                val cols = m.groupValues[2].toIntOrNull() ?: -1
                blockDb.startComposite(id, id, cols)
                return@let
            }?.also { continue }

            // classDef
            RE_CLASSDEF.find(trimmed)?.let { m ->
                val className = m.groupValues[1]
                val stylesStr = m.groupValues[2]
                val styles = parseStyles(stylesStr)
                blockDb.addClassDef(className, styles)
                return@let
            }?.also { continue }

            // class
            RE_CLASS.find(trimmed)?.let { m ->
                val ids = m.groupValues[1].split(",").map { it.trim() }
                val className = m.groupValues[2]
                blockDb.applyClass(ids, className)
                return@let
            }?.also { continue }

            // style
            RE_STYLE.find(trimmed)?.let { m ->
                val id = m.groupValues[1]
                val stylesStr = m.groupValues[2]
                val styles = parseStyles(stylesStr)
                blockDb.getBlock(id)?.let { block ->
                    val existing = block.styles.toMutableMap()
                    existing.putAll(styles)
                    // 通过 copy 更新不可行因为 block 存在引用，直接加 class
                }
                return@let
            }?.also { continue }

            // 边
            RE_EDGE.find(trimmed)?.let { m ->
                val source = m.groupValues[1]
                val edgeType = m.groupValues[2]
                val label = m.groupValues[3]
                val target = m.groupValues[4]

                val (arrowStart, arrowEnd, lineType) = parseEdgeType(edgeType)
                blockDb.addEdge(source, target, label, arrowStart, arrowEnd, lineType)
                return@let
            }?.also { continue }

            // 节点声明 - 简化处理：提取 id, label, shape, span
            parseNodeLine(trimmed, blockDb)
        }
    }

    /**
     * 解析节点行，提取 id、标签、形状和跨列
     */
    private fun parseNodeLine(line: String, db: BlockDb) {
        // 简化解析：先提取 id 和可选的 :N 跨列
        val spanMatch = Regex("""^(.+?):(\d+)\s*$""").find(line)
        val mainPart = if (spanMatch != null) spanMatch.groupValues[1].trim() else line.trim()
        val span = spanMatch?.groupValues?.get(2)?.toIntOrNull() ?: 1

        // 提取 id 和形状 + 标签
        val result = parseShapeAndLabel(mainPart)
        if (result != null) {
            val (id, label, shape) = result
            db.addBlock(id, label, shape, span)
        }
    }

    /**
     * 解析形状和标签
     * 返回 Triple(id, label, shape) 或 null
     */
    private fun parseShapeAndLabel(text: String): Triple<String, String, String>? {
        // id["label"] — 矩形
        Regex("""^(\S+?)\["(.+?)"\]$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "rect")
        }
        // id("label") — 圆角矩形（stadium）
        Regex("""^(\S+?)\("(.+?)"\)$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "stadium")
        }
        // id(["label"]) — 子过程
        Regex("""^(\S+?)\(\["(.+?)"\]\)$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "subroutine")
        }
        // id(("label")) — 圆形
        Regex("""^(\S+?)\(\("(.+?)"\)\)$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "circle")
        }
        // id{"label"} — 菱形
        Regex("""^(\S+?)\{"(.+?)"\}$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "diamond")
        }
        // id{{"label"}} — 六边形
        Regex("""^(\S+?)\{\{"(.+?)"\}\}$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "hexagon")
        }
        // id>"label"] — 信号形（lean_right）
        Regex("""^(\S+?)>"(.+?)"\]$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "lean_right")
        }
        // id[/"label"/] — 平行四边形
        Regex("""^(\S+?)\[/"(.+?)"/\]$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "lean_right")
        }
        // id[\"label"\] — 反向平行四边形
        Regex("""^(\S+?)\[\\"(.+?)"\\\]$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "lean_left")
        }
        // id[/"label"\] — 梯形
        Regex("""^(\S+?)\[/"(.+?)"\\\]$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "trapezoid")
        }
        // id[\"label"/] — 反梯形
        Regex("""^(\S+?)\[\\"(.+?)"/\]$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], "inv_trapezoid")
        }

        // 纯 id (无标签、无形状修饰) — 只接受合法标识符
        Regex("""^([a-zA-Z_][\w]*)$""").find(text)?.let {
            return Triple(it.groupValues[1], it.groupValues[1], "rect")
        }

        return null
    }

    /**
     * 解析边类型
     * @return Triple(arrowTypeStart, arrowTypeEnd, lineType)
     */
    private fun parseEdgeType(edgeStr: String): Triple<String, String, String> {
        return when {
            edgeStr.contains("==>") -> Triple("none", "arrow_point", "thick")
            edgeStr.contains(".->") || edgeStr.contains("-.->") -> Triple("none", "arrow_point", "dotted")
            edgeStr.contains("---") -> Triple("none", "none", "normal")
            edgeStr.contains("-->") -> Triple("none", "arrow_point", "normal")
            else -> Triple("none", "arrow_point", "normal")
        }
    }

    /**
     * 解析样式字符串
     */
    private fun parseStyles(stylesStr: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (part in stylesStr.split(",")) {
            val kv = part.trim().split(":", limit = 2)
            if (kv.size == 2) {
                result[kv[0].trim()] = kv[1].trim()
            }
        }
        return result
    }
}
