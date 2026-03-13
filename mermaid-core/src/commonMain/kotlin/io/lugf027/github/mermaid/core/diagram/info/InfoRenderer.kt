package io.lugf027.github.mermaid.core.diagram.info

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables

/**
 * Info 图表渲染器 - 对标 mermaid-js infoRenderer.ts
 *
 * 极简渲染器，在 SVG 中显示版本信息文本。
 */
class InfoRenderer : DiagramRenderer {

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val infoDb = db as? InfoDb ?: throw IllegalArgumentException("Expected InfoDb")
        val version = infoDb.getVersion()

        return buildSvg {
            attr("id", diagramId)
            attr("width", "100%")
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
            attr("style", "max-width: 400px; background-color: white;")
            attr("role", "graphics-document document")
            attr("aria-roledescription", "info")

            // 空 group（对标 JS 结构）
            group {}

            // 版本文本
            group {
                text("v$version", 100.0, 40.0) {
                    addClass("version")
                    attr("font-size", "32")
                    attr("style", "text-anchor: middle;")
                }
            }
        }
    }
}
