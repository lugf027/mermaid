package io.lugf027.github.mermaid.core.rendering.shapes

import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * 形状渲染函数类型
 */
typealias ShapeRenderer = (node: LayoutNode, themeVariables: ThemeVariables) -> SvgGroup

/**
 * 形状注册表 - 对标 mermaid-js shapes.ts
 *
 * 管理所有 60+ 种节点形状的渲染函数。
 */
object ShapeRegistry {

    private val shapes = mutableMapOf<String, ShapeRenderer>()

    /**
     * 注册形状
     */
    fun register(name: String, renderer: ShapeRenderer) {
        shapes[name] = renderer
    }

    /**
     * 获取形状渲染函数
     */
    fun get(name: String): ShapeRenderer? = shapes[name]

    /**
     * 渲染节点形状
     */
    fun render(node: LayoutNode, themeVariables: ThemeVariables): SvgGroup {
        val renderer = shapes[node.shape] ?: shapes["squareRect"]
        ?: throw IllegalArgumentException("Shape not found: ${node.shape}")
        return renderer(node, themeVariables)
    }

    /**
     * 注册所有内置形状
     */
    fun registerBuiltinShapes() {
        // 基础形状
        register("squareRect", BasicShapes::squareRect)
        register("rect", BasicShapes::squareRect)
        register("roundedRect", BasicShapes::roundedRect)
        register("circle", BasicShapes::circle)
        register("doublecircle", BasicShapes::doubleCircle)
        register("diamond", BasicShapes::diamond)
        register("hexagon", BasicShapes::hexagon)
        register("stadium", BasicShapes::stadium)
        register("cylinder", BasicShapes::cylinder)
        register("ellipse", BasicShapes::ellipseShape)

        // 特殊形状
        register("subroutine", SpecialShapes::subroutine)
        register("trapezoid", SpecialShapes::trapezoid)
        register("inv_trapezoid", SpecialShapes::invertedTrapezoid)
        register("parallelogram", SpecialShapes::parallelogram)
        register("lean_right", SpecialShapes::parallelogram)
        register("lean_left", SpecialShapes::parallelogramLeft)
        register("odd", SpecialShapes::odd)
        register("cloud", SpecialShapes::cloud)
        register("bolt", SpecialShapes::bolt)

        // 别名
        register("rectangle", BasicShapes::squareRect)
        register("round", BasicShapes::roundedRect)
        register("rhombus", BasicShapes::diamond)
    }
}
