package io.lugf027.github.mermaid.core.renderer.shapes

import io.lugf027.github.mermaid.core.types.ShapeId

/**
 * 形状工厂。
 * 根据 ShapeId 创建对应的 Shape 实例。
 */
object ShapeFactory {

    private val shapeCache = mutableMapOf<ShapeId, Shape>()

    /**
     * 获取指定 ShapeId 对应的 Shape 实例。
     * 使用缓存避免重复创建。
     */
    fun getShape(shapeId: ShapeId): Shape {
        return shapeCache.getOrPut(shapeId) {
            createShape(shapeId)
        }
    }

    private fun createShape(shapeId: ShapeId): Shape {
        return when (shapeId) {
            ShapeId.RECT, ShapeId.SQUARE -> RectShape()
            ShapeId.ROUNDED_RECT -> RoundedRectShape()
            ShapeId.CIRCLE, ShapeId.DOUBLE_CIRCLE -> CircleShape()
            ShapeId.ELLIPSE -> EllipseShape()
            ShapeId.DIAMOND, ShapeId.CHOICE -> DiamondShape()
            ShapeId.HEXAGON -> HexagonShape()
            ShapeId.STADIUM -> StadiumShape()
            ShapeId.CYLINDER, ShapeId.LINED_CYLINDER -> CylinderShape()
            ShapeId.SUBROUTINE -> SubroutineShape()
            ShapeId.PARALLELOGRAM, ShapeId.PARALLELOGRAM_ALT -> ParallelogramShape()
            ShapeId.TRAPEZOID, ShapeId.TRAPEZOID_ALT -> TrapezoidShape()
            // 所有其他形状暂时回退到矩形
            else -> RectShape()
        }
    }

    /**
     * 清除形状缓存。
     */
    fun clearCache() {
        shapeCache.clear()
    }
}
