package io.lugf027.github.mermaid.core.layout

import io.lugf027.github.mermaid.core.config.MermaidConfig

/**
 * 布局数据模型 - 对标 mermaid-js rendering-util/types.ts
 *
 * 这是解析器和统一渲染器之间的桥梁数据结构。
 */

/** 2D 坐标点 */
data class Point(val x: Double, val y: Double)

/**
 * 布局节点 - 对标 mermaid-js Node interface
 */
data class LayoutNode(
    val id: String,
    var label: String? = null,
    var shape: String = "squareRect",
    var isGroup: Boolean = false,
    var width: Double = 0.0,
    var height: Double = 0.0,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var cssStyles: List<String> = emptyList(),
    var cssClasses: String = "",
    var dir: String? = null,
    var parentId: String? = null,
    var children: List<String> = emptyList(),
    var padding: Double = 15.0,
    var rx: Double = 0.0,
    var ry: Double = 0.0,
    var icon: String? = null,
    var img: String? = null,
    var look: String? = null,
    var labelStyle: String? = null,
    var domId: String? = null,
    var link: String? = null,
    var linkTarget: String? = null,
    var tooltip: String? = null,
)

/**
 * 布局边 - 对标 mermaid-js Edge interface
 */
data class LayoutEdge(
    val id: String,
    var start: String = "",
    var end: String = "",
    var label: String? = null,
    var type: String? = null,
    var arrowTypeStart: String? = null,
    var arrowTypeEnd: String? = "arrow_point",
    var stroke: String = "normal",
    var thickness: String = "normal",
    var animate: Boolean = false,
    var curve: String? = null,
    var points: MutableList<Point> = mutableListOf(),
    var labelpos: String = "c",
    var labelStyle: String? = null,
    var cssStyles: List<String> = emptyList(),
    var pattern: String? = null,
    var minLen: Int = 1,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0,
)

/**
 * 布局数据 - 传递给布局算法和渲染器
 */
data class LayoutData(
    val nodes: List<LayoutNode>,
    val edges: List<LayoutEdge>,
    val config: MermaidConfig,
    val layoutAlgorithm: String = "dagre",
    val direction: String = "TB",
    val markers: List<String> = listOf("arrow_point"),
    val nodeSpacing: Int = 50,
    val rankSpacing: Int = 50,
    val diagramPadding: Int = 8,
)
