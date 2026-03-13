package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.preprocess.Preprocessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 预处理器单元测试 - 测试文本清理、frontmatter、指令、注释处理
 */
class PreprocessorTest {

    @Test
    fun testBasicPreprocessing() {
        val input = """
            flowchart LR
                A --> B
        """.trimIndent()
        val result = Preprocessor.process(input)
        assertTrue(result.code.contains("flowchart LR"))
        assertTrue(result.code.contains("A --> B"))
    }

    @Test
    fun testFrontmatterExtraction() {
        val input = """
            ---
            title: My Chart
            ---
            pie
                "Dogs" : 30
                "Cats" : 70
        """.trimIndent()
        val result = Preprocessor.process(input)
        assertEquals("My Chart", result.title)
        assertTrue(result.code.contains("pie"))
        assertTrue(!result.code.contains("---"))
    }

    @Test
    fun testDirectiveExtraction() {
        val input = """
            %%{init: {"theme": "dark"}}%%
            flowchart LR
                A --> B
        """.trimIndent()
        val result = Preprocessor.process(input)
        assertTrue(result.directives.isNotEmpty())
        assertTrue(result.code.contains("flowchart LR"))
        assertTrue(!result.code.contains("%%{init"))
    }

    @Test
    fun testCommentRemoval() {
        val input = """
            flowchart LR
                %% This is a comment
                A --> B
        """.trimIndent()
        val result = Preprocessor.process(input)
        assertTrue(result.code.contains("A --> B"))
        assertTrue(!result.code.contains("This is a comment"))
    }

    @Test
    fun testEmptyInput() {
        val result = Preprocessor.process("")
        assertTrue(result.code.isEmpty())
        assertNull(result.title)
    }

    @Test
    fun testCRLFConversion() {
        val input = "flowchart LR\r\n    A --> B\r\n"
        val result = Preprocessor.process(input)
        assertTrue(!result.code.contains("\r"))
        assertTrue(result.code.contains("A --> B"))
    }
}
