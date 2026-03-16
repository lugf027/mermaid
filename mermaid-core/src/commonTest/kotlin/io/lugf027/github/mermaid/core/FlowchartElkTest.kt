package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.flowchartElk.FlowchartElkDiagram
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartDb
import io.lugf027.github.mermaid.core.diagram.flowchart.FlowchartParser
import io.lugf027.github.mermaid.core.layout.elk.*
import io.lugf027.github.mermaid.core.layout.Point
import io.lugf027.github.mermaid.core.rendering.shapes.ShapeRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class FlowchartElkTest {

    @Test
    fun testElkKeywordDetection() {
        val def = FlowchartElkDiagram.definition()
        assertTrue(def.detector("flowchart-elk LR\n  A --> B"))
    }

    @Test
    fun testElkDirectiveDetection() {
        val def = FlowchartElkDiagram.definition()
        val text = """
            %%{init: {'flowchart': {'defaultRenderer': 'elk'}}}%%
            flowchart LR
              A --> B
        """.trimIndent()
        assertTrue(def.detector(text))
    }

    @Test
    fun testNonElkFlowchartNotDetected() {
        val def = FlowchartElkDiagram.definition()
        assertFalse(def.detector("flowchart LR\n  A --> B"))
    }

    @Test
    fun testElkReusesFlowchartParsing() {
        val db = FlowchartDb()
        FlowchartParser().parse("""
            flowchart-elk LR
              A[Node A] --> B[Node B]
              B --> C[Node C]
        """.trimIndent(), db)

        val vertices = db.getVertices()
        assertTrue(vertices.isNotEmpty())
    }

    @Test
    fun testDefinitionId() {
        val def = FlowchartElkDiagram.definition()
        assertEquals("flowchart-elk", def.id)
    }

    // ====== ELK Layout Tests ======

    @Test
    fun testElkLayoutBasicFlow() {
        val db = FlowchartDb()
        FlowchartParser().parse("""
            flowchart-elk TD
              A[Start] --> B[Process]
              B --> C[End]
        """.trimIndent(), db)

        val config = MermaidConfig.DEFAULT
        val layoutData = db.getData(config)

        val elkLayout = ElkLayout()
        val result = elkLayout.layout(layoutData)

        // 验证节点有坐标
        for (node in result.nodes) {
            if (!node.isGroup) {
                assertTrue(node.width > 0, "Node ${node.id} should have width > 0")
                assertTrue(node.height > 0, "Node ${node.id} should have height > 0")
            }
        }

        // 验证边有点
        for (edge in result.edges) {
            assertTrue(edge.points.isNotEmpty(), "Edge ${edge.id} should have points")
        }
    }

    @Test
    fun testElkLayoutProducesDistinctNodePositions() {
        val db = FlowchartDb()
        FlowchartParser().parse("""
            flowchart-elk TD
              A[Start] --> B[End]
        """.trimIndent(), db)

        val config = MermaidConfig.DEFAULT
        val layoutData = db.getData(config)

        val elkLayout = ElkLayout()
        val result = elkLayout.layout(layoutData)

        val nonGroupNodes = result.nodes.filter { !it.isGroup }
        assertTrue(nonGroupNodes.size >= 2, "Should have at least 2 non-group nodes")

        // 验证节点位置不同
        val nodeA = nonGroupNodes.find { it.id == "A" }
        val nodeB = nonGroupNodes.find { it.id == "B" }
        assertNotNull(nodeA, "Node A should exist")
        assertNotNull(nodeB, "Node B should exist")

        // TD 方向下，A 应在 B 的上方
        assertTrue(nodeA.y < nodeB.y, "In TD direction, A should be above B")
    }

    // ====== ELK Geometry Tests ======

    @Test
    fun testElkGeometryOutsideNode() {
        val bounds = ElkGeometry.RectBounds(50.0, 50.0, 100.0, 60.0)
        // 在矩形外部
        assertTrue(ElkGeometry.outsideNode(bounds, Point(0.0, 0.0)))
        assertTrue(ElkGeometry.outsideNode(bounds, Point(150.0, 50.0)))
        // 在矩形内部
        assertFalse(ElkGeometry.outsideNode(bounds, Point(50.0, 50.0)))
        assertFalse(ElkGeometry.outsideNode(bounds, Point(60.0, 60.0)))
    }

    @Test
    fun testElkGeometryOnBorder() {
        val bounds = ElkGeometry.RectBounds(50.0, 50.0, 100.0, 60.0)
        // 在左边框上
        assertTrue(ElkGeometry.onBorder(bounds, Point(0.0, 50.0)))
        // 在右边框上
        assertTrue(ElkGeometry.onBorder(bounds, Point(100.0, 50.0)))
        // 在中心（不在边框上）
        assertFalse(ElkGeometry.onBorder(bounds, Point(50.0, 50.0)))
    }

    @Test
    fun testElkGeometryIntersection() {
        val bounds = ElkGeometry.RectBounds(50.0, 50.0, 100.0, 60.0)
        val outside = Point(200.0, 50.0)
        val inside = Point(50.0, 50.0)
        val result = ElkGeometry.intersection(bounds, outside, inside)
        // 交点应在右边框上
        assertEquals(100.0, result.x, 0.1)
        assertEquals(50.0, result.y, 0.1)
    }

    // ====== ELK Ancestor Tests ======

    @Test
    fun testFindCommonAncestorSameParent() {
        val treeData = TreeData(
            parentById = mutableMapOf("A" to "sg1", "B" to "sg1"),
            childrenById = mutableMapOf("sg1" to mutableListOf("A", "B")),
        )
        assertEquals("sg1", findCommonAncestor("A", "B", treeData))
    }

    @Test
    fun testFindCommonAncestorDifferentParents() {
        val treeData = TreeData(
            parentById = mutableMapOf("A" to "sg1", "B" to "sg2"),
            childrenById = mutableMapOf(
                "sg1" to mutableListOf("A"),
                "sg2" to mutableListOf("B"),
            ),
        )
        assertEquals("root", findCommonAncestor("A", "B", treeData))
    }

    @Test
    fun testFindCommonAncestorSelfEdge() {
        val treeData = TreeData(
            parentById = mutableMapOf("A" to "sg1"),
            childrenById = mutableMapOf("sg1" to mutableListOf("A")),
        )
        assertEquals("sg1", findCommonAncestor("A", "A", treeData))
    }

    // ====== Full Render Test ======

    @Test
    fun testSubgraphNodeTracking() {
        // 验证子图栈机制正确追踪节点
        val db = FlowchartDb()
        FlowchartParser().parse("""
            flowchart LR
                subgraph Frontend
                    A[React] --> B[Redux]
                end
                subgraph Backend
                    C[API] --> D[DB]
                end
                B --> C
        """.trimIndent(), db)

        val subGraphs = db.getSubGraphs()
        assertEquals(2, subGraphs.size, "Should have 2 subgraphs")

        val frontend = subGraphs.find { it.id == "Frontend" }
        val backend = subGraphs.find { it.id == "Backend" }
        assertNotNull(frontend, "Frontend subgraph should exist")
        assertNotNull(backend, "Backend subgraph should exist")

        // 验证 Frontend 包含 A 和 B
        assertTrue("A" in frontend.nodes, "Frontend should contain A, but has: ${frontend.nodes}")
        assertTrue("B" in frontend.nodes, "Frontend should contain B, but has: ${frontend.nodes}")

        // 验证 Backend 包含 C 和 D
        assertTrue("C" in backend.nodes, "Backend should contain C, but has: ${backend.nodes}")
        assertTrue("D" in backend.nodes, "Backend should contain D, but has: ${backend.nodes}")

        // 验证 getData 正确设置 parentId
        val config = MermaidConfig.DEFAULT
        val data = db.getData(config)
        val nodeA = data.nodes.find { it.id == "A" }
        val nodeC = data.nodes.find { it.id == "C" }
        assertNotNull(nodeA)
        assertNotNull(nodeC)
        assertEquals("Frontend", nodeA.parentId, "A should have Frontend as parent")
        assertEquals("Backend", nodeC.parentId, "C should have Backend as parent")
    }

    @Test
    fun testFlowchartElkRendererProducesSvg() {
        // 渲染测试需要先注册内置形状（正常使用时由 MermaidApi.initialize() 完成）
        ShapeRegistry.registerBuiltinShapes()

        val def = FlowchartElkDiagram.definition()
        val db = def.dbFactory()
        def.parser.parse("""
            flowchart-elk TD
              A[Start] --> B[Process]
              B --> C[End]
        """.trimIndent(), db)

        val config = MermaidConfig.DEFAULT
        val themeVars = io.lugf027.github.mermaid.core.themes.ThemeManager.getThemeVariables("default")
        val svg = def.renderer.draw(db, config, themeVars, "test-elk")

        // SVG 应该有内容
        val serialized = io.lugf027.github.mermaid.core.rendering.svg.SvgSerializer.serialize(svg)
        assertTrue(serialized.isNotEmpty(), "SVG should not be empty")
        assertTrue(serialized.contains("flowchart"), "SVG should contain flowchart class")
        assertTrue(serialized.contains("<g"), "SVG should contain group elements")
    }
}
