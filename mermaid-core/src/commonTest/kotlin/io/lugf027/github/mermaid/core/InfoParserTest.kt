package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.info.InfoDb
import io.lugf027.github.mermaid.core.diagram.info.InfoParser
import kotlin.test.Test
import kotlin.test.assertEquals

class InfoParserTest {
    private val parser = InfoParser()

    @Test
    fun testParseInfoDiagram() {
        val db = InfoDb()
        val text = """
            info
        """.trimIndent()

        parser.parse(text, db)
        assertEquals("0.1.0", db.getVersion())
    }

    @Test
    fun testInfoClearsOnParse() {
        val db = InfoDb()
        db.setDiagramTitle("old title")
        parser.parse("info", db)
        assertEquals("", db.getDiagramTitle())
    }
}
