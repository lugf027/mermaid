/**
 * Direction - Layout direction for diagrams.
 */
package io.github.lugf027.mermaid.model

/**
 * Direction of diagram layout.
 */
public enum class Direction(
    /**
     * Keywords that represent this direction.
     */
    public val keywords: List<String>
) {
    /**
     * Top to bottom (vertical, default).
     */
    TOP_TO_BOTTOM(listOf("TB", "TD")),

    /**
     * Bottom to top (vertical).
     */
    BOTTOM_TO_TOP(listOf("BT")),

    /**
     * Left to right (horizontal).
     */
    LEFT_TO_RIGHT(listOf("LR")),

    /**
     * Right to left (horizontal).
     */
    RIGHT_TO_LEFT(listOf("RL"));

    /**
     * Whether this is a horizontal layout.
     */
    public val isHorizontal: Boolean
        get() = this == LEFT_TO_RIGHT || this == RIGHT_TO_LEFT

    /**
     * Whether this is a vertical layout.
     */
    public val isVertical: Boolean
        get() = this == TOP_TO_BOTTOM || this == BOTTOM_TO_TOP

    public companion object {
        /**
         * Parse direction from keyword string.
         */
        public fun fromKeyword(keyword: String): Direction {
            val upper = keyword.uppercase()
            return entries.find { direction ->
                direction.keywords.any { it.equals(upper, ignoreCase = true) }
            } ?: TOP_TO_BOTTOM
        }

        /**
         * Default direction.
         */
        public val DEFAULT: Direction = TOP_TO_BOTTOM
    }
}
