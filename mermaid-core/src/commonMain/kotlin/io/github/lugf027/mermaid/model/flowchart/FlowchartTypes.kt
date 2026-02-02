/**
 * Flowchart-specific data models.
 */
package io.github.lugf027.mermaid.model.flowchart

import io.github.lugf027.mermaid.model.*

/**
 * Node shape types for flowchart vertices.
 */
public enum class NodeShape {
    /**
     * Rectangle with square corners: [text]
     */
    RECTANGLE,

    /**
     * Rectangle with rounded corners: (text)
     */
    ROUNDED,

    /**
     * Stadium/pill shape: ([text])
     */
    STADIUM,

    /**
     * Subroutine/process: [[text]]
     */
    SUBROUTINE,

    /**
     * Cylindrical database shape: [(text)]
     */
    CYLINDER,

    /**
     * Circle: ((text))
     */
    CIRCLE,

    /**
     * Asymmetric/flag shape: >text]
     */
    ASYMMETRIC,

    /**
     * Diamond/rhombus decision: {text}
     */
    DIAMOND,

    /**
     * Hexagon: {{text}}
     */
    HEXAGON,

    /**
     * Parallelogram (slant right): [/text/]
     */
    PARALLELOGRAM,

    /**
     * Parallelogram (slant left): [\text\]
     */
    PARALLELOGRAM_ALT,

    /**
     * Trapezoid: [/text\]
     */
    TRAPEZOID,

    /**
     * Trapezoid (inverted): [\text/]
     */
    TRAPEZOID_ALT,

    /**
     * Double circle: (((text)))
     */
    DOUBLE_CIRCLE;

    public companion object {
        /**
         * Default node shape.
         */
        public val DEFAULT: NodeShape = RECTANGLE
    }
}

/**
 * Edge/link types for flowchart connections.
 */
public enum class LinkType {
    /**
     * Solid line with arrow: -->
     */
    ARROW,

    /**
     * Solid line without arrow: ---
     */
    LINE,

    /**
     * Dotted line with arrow: -.->
     */
    DOTTED_ARROW,

    /**
     * Dotted line without arrow: -.-
     */
    DOTTED,

    /**
     * Thick line with arrow: ==>
     */
    THICK_ARROW,

    /**
     * Thick line without arrow: ===
     */
    THICK,

    /**
     * Invisible link: ~~~
     */
    INVISIBLE;

    /**
     * Whether this link type has an arrow.
     */
    public val hasArrow: Boolean
        get() = this == ARROW || this == DOTTED_ARROW || this == THICK_ARROW

    /**
     * Whether this link type is dotted.
     */
    public val isDotted: Boolean
        get() = this == DOTTED || this == DOTTED_ARROW

    /**
     * Whether this link type is thick.
     */
    public val isThick: Boolean
        get() = this == THICK || this == THICK_ARROW

    public companion object {
        /**
         * Default link type.
         */
        public val DEFAULT: LinkType = ARROW
    }
}

/**
 * Arrow head types.
 */
public enum class ArrowHead {
    /**
     * Standard arrow: >
     */
    ARROW,

    /**
     * Circle end: o
     */
    CIRCLE,

    /**
     * Cross end: x
     */
    CROSS,

    /**
     * No arrow head
     */
    NONE
}
