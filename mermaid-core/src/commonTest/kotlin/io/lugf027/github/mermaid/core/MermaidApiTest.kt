package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.config.MermaidConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * MermaidApi 端到端测试 - 测试完整的 parse → render → SVG 管线
 */
class MermaidApiTest {

    @BeforeTest
    fun setup() {
        MermaidApi.reset()
        MermaidApi.initialize()
    }

    @AfterTest
    fun cleanup() {
        MermaidApi.reset()
    }

    @Test
    fun testInitialization() {
        val types = MermaidApi.getRegisteredDiagramTypes()
        assertTrue(types.isNotEmpty())
        assertTrue(types.contains("flowchart-v2"))
        assertTrue(types.contains("pie"))
        assertTrue(types.contains("error"))
    }

    @Test
    fun testParseFlowchart() {
        val diagram = MermaidApi.parse("""
            flowchart LR
                A[Start] --> B[End]
        """.trimIndent())

        assertEquals("flowchart-v2", diagram.type)
        assertNotNull(diagram.db)
        assertNotNull(diagram.renderer)
    }

    @Test
    fun testParsePie() {
        val diagram = MermaidApi.parse("""
            pie
                "Dogs" : 30
                "Cats" : 70
        """.trimIndent())

        assertEquals("pie", diagram.type)
    }

    @Test
    fun testParseUnknownFallsToError() {
        val diagram = MermaidApi.parse("unknownDiagram\n  something")
        assertEquals("error", diagram.type)
    }

    @Test
    fun testRenderFlowchartToSvg() {
        val svg = MermaidApi.renderToSvg("""
            flowchart LR
                A[Hello] --> B[World]
        """.trimIndent())

        assertTrue(svg.isNotEmpty())
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
    }

    @Test
    fun testRenderPieToSvg() {
        val svg = MermaidApi.renderToSvg("""
            pie
                "Dogs" : 30
                "Cats" : 70
        """.trimIndent())

        assertTrue(svg.isNotEmpty())
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
        assertTrue(svg.contains("pieCircle"))
    }

    @Test
    fun testRenderPieWithTitle() {
        val svg = MermaidApi.renderToSvg("""
            pie
                title My Pets
                "Dogs" : 30
                "Cats" : 70
        """.trimIndent())

        assertTrue(svg.contains("My Pets"))
    }

    @Test
    fun testRenderPieWithShowData() {
        val svg = MermaidApi.renderToSvg("""
            pie showData
                "Dogs" : 30
                "Cats" : 70
        """.trimIndent())

        assertTrue(svg.contains("[30]") || svg.contains("[30.0]") || svg.contains("Dogs"))
    }

    @Test
    fun testCustomDiagramId() {
        val svg = MermaidApi.renderToSvg(
            text = "pie\n  \"A\" : 50\n  \"B\" : 50",
            diagramId = "myChart42"
        )
        assertTrue(svg.contains("myChart42"))
    }

    @Test
    fun testRenderToSvgContent() {
        val svgContent = MermaidApi.renderToSvgContent("""
            pie
                "A" : 50
                "B" : 50
        """.trimIndent())

        assertTrue(svgContent.contains("<svg"))
        assertTrue(!svgContent.contains("<?xml"))
    }

    @Test
    fun testInitializeWithTheme() {
        MermaidApi.reset()
        MermaidApi.initialize(MermaidConfig(theme = "dark"))

        val config = MermaidApi.getConfig()
        assertEquals("dark", config.theme)
    }

    @Test
    fun testFrontmatterTitle() {
        val diagram = MermaidApi.parse("""
            ---
            title: Chart Title
            ---
            pie
                "A" : 50
                "B" : 50
        """.trimIndent())

        assertEquals("Chart Title", diagram.title)
    }

    @Test
    fun testDirectiveConfig() {
        val svg = MermaidApi.renderToSvg("""
            %%{init: {"theme": "dark"}}%%
            pie
                "A" : 50
                "B" : 50
        """.trimIndent())

        assertTrue(svg.contains("<svg"))
    }

    @Test
    fun testErrorDiagramRendering() {
        val svg = MermaidApi.renderToSvg("completely invalid input")
        assertTrue(svg.contains("<svg"))
        // Error diagram should render without crashing
    }
}
