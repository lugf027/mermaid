package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.rendering.svg.*
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SVG 序列化器单元测试 - 测试 SvgElement 到 SVG XML 的转换
 */
class SvgSerializerTest {

    @Test
    fun testBasicSvgSerialization() {
        val svg = buildSvg {
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("width", "100")
            attr("height", "100")
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(result.contains("<svg"))
        assertTrue(result.contains("xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(result.contains("width=\"100\""))
        assertTrue(result.contains("</svg>"))
    }

    @Test
    fun testSerializeContent() {
        val svg = buildSvg {
            attr("width", "50")
        }

        val result = SvgSerializer.serializeContent(svg)
        assertTrue(!result.contains("<?xml"))
        assertTrue(result.contains("<svg"))
    }

    @Test
    fun testRectSerialization() {
        val svg = buildSvg {
            rect(10.0, 20.0, 100.0, 50.0) {
                attr("fill", "#f00")
            }
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("<rect"))
        assertTrue(result.contains("fill=\"#f00\""))
    }

    @Test
    fun testTextSerialization() {
        val svg = buildSvg {
            text("Hello World", 50.0, 30.0) {
                attr("text-anchor", "middle")
            }
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("<text"))
        assertTrue(result.contains("Hello World"))
        assertTrue(result.contains("</text>"))
    }

    @Test
    fun testGroupSerialization() {
        val svg = buildSvg {
            group {
                addClass("myGroup")
                rect(0.0, 0.0, 10.0, 10.0)
            }
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("<g"))
        assertTrue(result.contains("class=\"myGroup\""))
        assertTrue(result.contains("<rect"))
        assertTrue(result.contains("</g>"))
    }

    @Test
    fun testPathSerialization() {
        val svg = buildSvg {
            path("M0,0 L100,100") {
                attr("stroke", "black")
                attr("fill", "none")
            }
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("<path"))
        assertTrue(result.contains("d=\"M0,0 L100,100\""))
    }

    @Test
    fun testStyleSerialization() {
        val svg = buildSvg {
            defs {
                style(".node { fill: red; }")
            }
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("<style>"))
        assertTrue(result.contains(".node { fill: red; }"))
        assertTrue(result.contains("</style>"))
    }

    @Test
    fun testCircleSerialization() {
        val svg = buildSvg {
            circle(50.0, 50.0, 25.0) {
                attr("fill", "blue")
            }
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("<circle"))
        assertTrue(result.contains("fill=\"blue\""))
    }

    @Test
    fun testXmlAttributeEscaping() {
        val svg = buildSvg {
            text("a < b & c > d", 0.0, 0.0)
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("a &lt; b &amp; c &gt; d"))
    }

    @Test
    fun testSelfClosingElements() {
        val svg = buildSvg {
            rect(0.0, 0.0, 10.0, 10.0)
        }

        val result = SvgSerializer.serialize(svg)
        assertTrue(result.contains("<rect") && result.contains("/>"))
    }

    @Test
    fun testIndentedOutput() {
        val svg = buildSvg {
            group {
                rect(0.0, 0.0, 10.0, 10.0)
            }
        }

        val result = SvgSerializer.serialize(svg, indent = true)
        assertTrue(result.contains("  <g>"))
        assertTrue(result.contains("    <rect"))
    }

    @Test
    fun testSvgPathBuilder() {
        val path = SvgPathBuilder()
            .moveTo(0.0, 0.0)
            .lineTo(100.0, 0.0)
            .lineTo(100.0, 100.0)
            .closePath()
            .build()

        assertEquals("M0,0L100,0L100,100Z", path)
    }

    @Test
    fun testSvgPathBuilderArc() {
        val path = SvgPathBuilder.arc(
            innerRadius = 0.0,
            outerRadius = 100.0,
            startAngle = 0.0,
            endAngle = PI / 2
        )

        assertTrue(path.isNotEmpty())
        assertTrue(path.startsWith("M"))
        assertTrue(path.contains("A"))
    }
}
