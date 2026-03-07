package io.lugf027.github.mermaid.core.diagrams.mindmap

/**
 * 思维导图类型定义。
 */

enum class MindmapNodeType {
    DEFAULT, RECT, ROUNDED_RECT, CIRCLE, CLOUD, BANG, HEXAGON
}

data class MindmapNode(
    val id: String,
    val level: Int,
    val text: String,
    val type: MindmapNodeType = MindmapNodeType.DEFAULT,
    val children: MutableList<MindmapNode> = mutableListOf(),
    val icon: String? = null,
    val cssClass: String? = null,
)
