package io.lugf027.github.mermaid.core.diagrams.flowchart

import io.lugf027.github.mermaid.core.types.ShapeId

/**
 * 流程图节点类型参数（来自解析器的括号语法）。
 * 对应 mermaid-js FlowVertexTypeParam。
 */
enum class FlowVertexType(val id: String) {
    SQUARE("square"),           // A[text]
    ROUND("round"),             // A(text)
    STADIUM("stadium"),         // A([text])
    SUBROUTINE("subroutine"),   // A[[text]]
    CYLINDER("cylinder"),       // A[(text)]
    CIRCLE("circle"),           // A((text))
    DOUBLE_CIRCLE("doublecircle"), // A(((text)))
    DIAMOND("diamond"),         // A{text}
    HEXAGON("hexagon"),         // A{{text}}
    ODD("odd"),                 // A>text]
    TRAPEZOID("trapezoid"),     // A[/text\]
    INV_TRAPEZOID("inv_trapezoid"), // A[\text/]
    LEAN_RIGHT("lean_right"),   // A[/text/]
    LEAN_LEFT("lean_left"),     // A[\text\]
    ELLIPSE("ellipse"),         // A(-text-)
    ;

    companion object {
        /**
         * 将 FlowVertexType 映射为渲染用 ShapeId。
         * 对应 mermaid-js flowDb.ts getTypeFromVertex()。
         */
        fun toShapeId(type: FlowVertexType?): ShapeId = when (type) {
            null, SQUARE -> ShapeId.SQUARE_RECT
            ROUND -> ShapeId.ROUNDED_RECT
            STADIUM -> ShapeId.STADIUM
            SUBROUTINE -> ShapeId.SUBROUTINE
            CYLINDER -> ShapeId.CYLINDER
            CIRCLE -> ShapeId.CIRCLE
            DOUBLE_CIRCLE -> ShapeId.DOUBLE_CIRCLE
            DIAMOND -> ShapeId.DIAMOND
            HEXAGON -> ShapeId.HEXAGON
            ODD -> ShapeId.ODD
            TRAPEZOID -> ShapeId.TRAPEZOID
            INV_TRAPEZOID -> ShapeId.INV_TRAPEZOID
            LEAN_RIGHT -> ShapeId.LEAN_RIGHT
            LEAN_LEFT -> ShapeId.LEAN_LEFT
            ELLIPSE -> ShapeId.ELLIPSE
        }
    }
}

/**
 * 流程图节点数据结构。
 * 对应 mermaid-js FlowVertex。
 */
data class FlowVertex(
    val id: String,
    var text: String = id,
    var labelType: LabelType = LabelType.TEXT,
    var type: FlowVertexType? = null,
    val classes: MutableList<String> = mutableListOf(),
    val styles: MutableList<String> = mutableListOf(),
    var dir: String? = null,
    var haveCallback: Boolean = false,
    var link: String? = null,
    var linkTarget: String? = null,
    var props: Map<String, String>? = null,
)

/**
 * 文本标签类型。
 */
enum class LabelType {
    TEXT, STRING, MARKDOWN
}

/**
 * 箭头类型。
 * 对应 mermaid-js 的 arrow_point/arrow_circle/arrow_cross/arrow_open 等。
 */
enum class ArrowType(val id: String) {
    ARROW_POINT("arrow_point"),       // --> 箭头
    ARROW_CIRCLE("arrow_circle"),     // --o 圆圈
    ARROW_CROSS("arrow_cross"),       // --x 叉号
    ARROW_OPEN("arrow_open"),         // --- 无箭头
    DOUBLE_ARROW_POINT("double_arrow_point"),   // <-->
    DOUBLE_ARROW_CIRCLE("double_arrow_circle"), // o--o
    DOUBLE_ARROW_CROSS("double_arrow_cross"),   // x--x
    ;

    companion object {
        fun fromString(s: String): ArrowType? = entries.find { it.id == s }
    }
}

/**
 * 线条样式。
 */
enum class StrokeStyle {
    NORMAL,     // 实线
    THICK,      // 粗线
    DOTTED,     // 虚线
    INVISIBLE   // 隐形
}

/**
 * 流程图边数据结构。
 * 对应 mermaid-js FlowEdge。
 */
data class FlowEdge(
    val start: String,
    val end: String,
    var type: ArrowType = ArrowType.ARROW_OPEN,
    var stroke: StrokeStyle = StrokeStyle.NORMAL,
    var text: String = "",
    var labelType: LabelType = LabelType.TEXT,
    var length: Int = 1,
    val classes: MutableList<String> = mutableListOf(),
    val style: MutableList<String> = mutableListOf(),
    var interpolate: String? = null,
)

/**
 * 样式类定义。
 * 对应 mermaid-js FlowClass。
 */
data class FlowClass(
    val id: String,
    val styles: MutableList<String> = mutableListOf(),
    val textStyles: MutableList<String> = mutableListOf(),
)

/**
 * 子图。
 * 对应 mermaid-js FlowSubGraph。
 */
data class FlowSubGraph(
    val id: String,
    val nodes: MutableList<String> = mutableListOf(),
    var title: String = "",
    val classes: MutableList<String> = mutableListOf(),
    var dir: String? = null,
    var labelType: LabelType = LabelType.TEXT,
)

/**
 * 解析出的边链接信息。
 * 对应 mermaid-js destructLink 返回值。
 */
data class LinkInfo(
    val type: ArrowType,
    val stroke: StrokeStyle,
    val length: Int,
)
