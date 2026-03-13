package io.lugf027.github.mermaid.core.diagram.treemap

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 树形图解析器 - 对标 mermaid-js treemap parser (Langium)
 *
 * 语法（缩进感知）：
 *   treemap-beta
 *   "Company"
 *     "Department A"
 *       "Team 1" : 100
 *       "Team 2" : 200
 *     "Department B"
 *       "Team 3" : 150
 *
 * - 有值(: value 或 , value)的节点是 Leaf
 * - 没有值的节点是 Section (容器)
 * - 支持 :::className 样式
 * - 支持 classDef 定义
 */
class TreemapParser : DiagramParser {

    private val RE_START = Regex("^\\s*treemap(-beta)?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_CLASSDEF = Regex("^\\s*classDef\\s+(\\w+)\\s+(.+)$", RegexOption.IGNORE_CASE)
    // Leaf: "name" : value or "name" , value (with optional :::class)
    private val RE_LEAF = Regex("^\\s*[\"']([^\"']+)[\"']\\s*[,:]\\s*(\\d+\\.?\\d*)\\s*(?::::(\\w+))?\\s*$")
    // Section: "name" (with optional :::class)
    private val RE_SECTION = Regex("^\\s*[\"']([^\"']+)[\"']\\s*(?:::(\\w+))?\\s*$")
    // Unquoted leaf: name : value
    private val RE_LEAF_UNQUOTED = Regex("^\\s*(\\S+(?:\\s+\\S+)*)\\s*[,:]\\s*(\\d+\\.?\\d*)\\s*(?::::(\\w+))?\\s*$")

    override fun parse(text: String, db: DiagramDB) {
        val tdb = db as TreemapDb
        tdb.clear()

        val lines = text.lines()
        var started = false
        var baseIndent = -1

        for (line in lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("%%")) continue

            if (!started) {
                if (RE_START.matches(line.trim())) started = true
                continue
            }

            val trimmed = line.trim()

            RE_ACC_TITLE.find(trimmed)?.let {
                tdb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                tdb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_CLASSDEF.find(trimmed)?.let { m ->
                val name = m.groupValues[1]
                val styleStr = m.groupValues[2]
                val styles = mutableMapOf<String, String>()
                for (part in styleStr.split(",")) {
                    val kv = part.trim().split(":", limit = 2)
                    if (kv.size == 2) {
                        styles[kv[0].trim()] = kv[1].trim()
                    }
                }
                tdb.addClassDef(name, styles)
                return@let
            }?.also { continue }

            // 计算缩进层级
            val spaces = line.length - line.trimStart().length
            if (baseIndent < 0) baseIndent = spaces
            val level = if (spaces > baseIndent) ((spaces - baseIndent + 1) / 2) else 0

            // 叶子节点(有值)
            RE_LEAF.find(trimmed)?.let { m ->
                val name = m.groupValues[1]
                val value = m.groupValues[2].toDoubleOrNull() ?: 0.0
                val cssClass = m.groupValues[3]
                tdb.addLeaf(name, value, level, cssClass)
                return@let
            }?.also { continue }

            // 容器节点
            RE_SECTION.find(trimmed)?.let { m ->
                val name = m.groupValues[1]
                val cssClass = m.groupValues[2]
                tdb.addSection(name, level, cssClass)
                return@let
            }?.also { continue }

            // 无引号叶子
            RE_LEAF_UNQUOTED.find(trimmed)?.let { m ->
                val name = m.groupValues[1]
                val value = m.groupValues[2].toDoubleOrNull() ?: 0.0
                val cssClass = m.groupValues[3]
                tdb.addLeaf(name, value, level, cssClass)
                return@let
            }
        }
    }
}
