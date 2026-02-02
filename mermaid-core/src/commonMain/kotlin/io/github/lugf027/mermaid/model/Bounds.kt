/**
 * Bounds - Rectangle bounds for diagram elements.
 */
package io.github.lugf027.mermaid.model

/**
 * Represents rectangular bounds for a diagram element.
 */
public data class Bounds(
    /**
     * X coordinate of the top-left corner.
     */
    val x: Float = 0f,
    
    /**
     * Y coordinate of the top-left corner.
     */
    val y: Float = 0f,
    
    /**
     * Width of the bounds.
     */
    val width: Float = 0f,
    
    /**
     * Height of the bounds.
     */
    val height: Float = 0f
) {
    /**
     * Center X coordinate.
     */
    val centerX: Float get() = x + width / 2f

    /**
     * Center Y coordinate.
     */
    val centerY: Float get() = y + height / 2f

    /**
     * Right edge X coordinate.
     */
    val right: Float get() = x + width

    /**
     * Bottom edge Y coordinate.
     */
    val bottom: Float get() = y + height

    /**
     * Check if this bounds contains a point.
     */
    public fun contains(px: Float, py: Float): Boolean {
        return px >= x && px <= right && py >= y && py <= bottom
    }

    /**
     * Create a new bounds expanded by the given amount.
     */
    public fun expand(amount: Float): Bounds {
        return Bounds(
            x = x - amount,
            y = y - amount,
            width = width + amount * 2,
            height = height + amount * 2
        )
    }

    /**
     * Create a new bounds that contains both this and other.
     */
    public fun union(other: Bounds): Bounds {
        if (width == 0f && height == 0f) return other
        if (other.width == 0f && other.height == 0f) return this
        
        val minX = minOf(x, other.x)
        val minY = minOf(y, other.y)
        val maxX = maxOf(right, other.right)
        val maxY = maxOf(bottom, other.bottom)
        
        return Bounds(
            x = minX,
            y = minY,
            width = maxX - minX,
            height = maxY - minY
        )
    }

    public companion object {
        /**
         * Empty bounds.
         */
        public val EMPTY: Bounds = Bounds()
    }
}
