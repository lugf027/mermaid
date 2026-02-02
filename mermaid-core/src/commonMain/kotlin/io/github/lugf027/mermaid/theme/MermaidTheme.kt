/**
 * MermaidTheme - Theme configuration for Mermaid diagrams.
 */
package io.github.lugf027.mermaid.theme

import androidx.compose.ui.graphics.Color

/**
 * Theme configuration for rendering Mermaid diagrams.
 */
public data class MermaidTheme(
    /**
     * Background color of the diagram.
     */
    val backgroundColor: Color = Color.White,

    /**
     * Default fill color for nodes.
     */
    val nodeFillColor: Color = Color(0xFFECECFF),

    /**
     * Default stroke color for nodes.
     */
    val nodeStrokeColor: Color = Color(0xFF9370DB),

    /**
     * Default text color.
     */
    val textColor: Color = Color(0xFF333333),

    /**
     * Default stroke color for edges.
     */
    val edgeStrokeColor: Color = Color(0xFF333333),

    /**
     * Label background color for edges.
     */
    val edgeLabelBackground: Color = Color(0xFFE8E8E8),

    /**
     * Subgraph background color.
     */
    val subgraphFillColor: Color = Color(0xFFFFFFDE),

    /**
     * Subgraph stroke color.
     */
    val subgraphStrokeColor: Color = Color(0xFFAAAA33),

    /**
     * Default stroke width for nodes.
     */
    val nodeStrokeWidth: Float = 2f,

    /**
     * Default stroke width for edges.
     */
    val edgeStrokeWidth: Float = 2f,

    /**
     * Font size for node text.
     */
    val fontSize: Float = 14f,

    /**
     * Font size for edge labels.
     */
    val edgeLabelFontSize: Float = 12f,

    /**
     * Corner radius for rounded rectangles.
     */
    val cornerRadius: Float = 8f
) {
    public companion object {
        /**
         * Default theme.
         */
        public val Default: MermaidTheme = MermaidTheme()

        /**
         * Dark theme.
         */
        public val Dark: MermaidTheme = MermaidTheme(
            backgroundColor = Color(0xFF1E1E1E),
            nodeFillColor = Color(0xFF2D2D2D),
            nodeStrokeColor = Color(0xFF6B8E23),
            textColor = Color(0xFFE0E0E0),
            edgeStrokeColor = Color(0xFFE0E0E0),
            edgeLabelBackground = Color(0xFF3D3D3D),
            subgraphFillColor = Color(0xFF2D3D2D),
            subgraphStrokeColor = Color(0xFF6B8E23)
        )

        /**
         * Forest theme (green tones).
         */
        public val Forest: MermaidTheme = MermaidTheme(
            nodeFillColor = Color(0xFFE8F5E9),
            nodeStrokeColor = Color(0xFF4CAF50),
            subgraphFillColor = Color(0xFFC8E6C9),
            subgraphStrokeColor = Color(0xFF388E3C)
        )

        /**
         * Neutral theme (gray tones).
         */
        public val Neutral: MermaidTheme = MermaidTheme(
            nodeFillColor = Color(0xFFF5F5F5),
            nodeStrokeColor = Color(0xFF9E9E9E),
            subgraphFillColor = Color(0xFFE0E0E0),
            subgraphStrokeColor = Color(0xFF757575)
        )
    }
}
