package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.gitGraph.GitGraphDb
import io.lugf027.github.mermaid.core.diagram.gitGraph.GitGraphParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Git 图解析器单元测试 - 对标 mermaid-js gitGraph.spec.ts
 */
class GitGraphParserTest {

    private lateinit var parser: GitGraphParser
    private lateinit var db: GitGraphDb

    @BeforeTest
    fun setup() {
        parser = GitGraphParser()
        db = GitGraphDb()
    }

    @Test
    fun testBasicGitGraph() {
        val input = """
            gitGraph
               commit
               commit
               commit
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(3, commits.size)
        assertEquals("main", commits[0].branch)
    }

    @Test
    fun testCommitWithId() {
        val input = """
            gitGraph
               commit id: "a1b2c3"
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(1, commits.size)
        assertEquals("a1b2c3", commits[0].id)
        assertTrue(commits[0].customId)
    }

    @Test
    fun testCommitWithMsg() {
        val input = """
            gitGraph
               commit msg: "Initial commit"
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(1, commits.size)
        assertEquals("Initial commit", commits[0].message)
    }

    @Test
    fun testCommitWithTag() {
        val input = """
            gitGraph
               commit tag: "v1.0"
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(1, commits.size)
        assertEquals(listOf("v1.0"), commits[0].tags)
    }

    @Test
    fun testCommitWithType() {
        val input = """
            gitGraph
               commit type: HIGHLIGHT
               commit type: REVERSE
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(2, commits.size)
        assertEquals(GitGraphDb.CommitType.HIGHLIGHT, commits[0].type)
        assertEquals(GitGraphDb.CommitType.REVERSE, commits[1].type)
    }

    @Test
    fun testBranch() {
        val input = """
            gitGraph
               commit
               branch develop
               commit
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(2, commits.size)
        assertEquals("main", commits[0].branch)
        assertEquals("develop", commits[1].branch)
    }

    @Test
    fun testBranchWithOrder() {
        val input = """
            gitGraph
               commit
               branch develop order: 2
               branch feature order: 1
        """.trimIndent()
        parser.parse(input, db)

        val sorted = db.getBranchesAsObjArray()
        assertEquals("main", sorted[0])
        assertEquals("feature", sorted[1])
        assertEquals("develop", sorted[2])
    }

    @Test
    fun testCheckout() {
        val input = """
            gitGraph
               commit
               branch develop
               commit
               checkout main
               commit
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(3, commits.size)
        assertEquals("main", commits[0].branch)
        assertEquals("develop", commits[1].branch)
        assertEquals("main", commits[2].branch)
    }

    @Test
    fun testMerge() {
        val input = """
            gitGraph
               commit
               branch develop
               commit
               checkout main
               merge develop
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(3, commits.size)
        assertEquals(GitGraphDb.CommitType.MERGE, commits[2].type)
        assertEquals(2, commits[2].parents.size)
    }

    @Test
    fun testMergeWithIdAndTag() {
        val input = """
            gitGraph
               commit
               branch develop
               commit
               checkout main
               merge develop id: "merge1" tag: "v2.0"
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        val mergeCommit = commits.last()
        assertEquals("merge1", mergeCommit.id)
        assertEquals(listOf("v2.0"), mergeCommit.tags)
    }

    @Test
    fun testCherryPick() {
        val input = """
            gitGraph
               commit id: "cp1"
               branch develop
               commit
               checkout main
               cherry-pick id: "cp1"
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        // 3 commits: cp1 on main, one on develop, cherry-pick on main
        assertEquals(3, commits.size)
        assertEquals(GitGraphDb.CommitType.CHERRY_PICK, commits[2].type)
    }

    @Test
    fun testDirectionLR() {
        val input = """
            gitGraph LR:
               commit
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(GitGraphDb.Direction.LR, db.getDirectionEnum())
    }

    @Test
    fun testDirectionTB() {
        val input = """
            gitGraph TB:
               commit
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(GitGraphDb.Direction.TB, db.getDirectionEnum())
    }

    @Test
    fun testDirectionBT() {
        val input = """
            gitGraph BT:
               commit
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(GitGraphDb.Direction.BT, db.getDirectionEnum())
    }

    @Test
    fun testSwitchKeyword() {
        val input = """
            gitGraph
               commit
               branch develop
               commit
               switch main
               commit
        """.trimIndent()
        parser.parse(input, db)

        val commits = db.getCommitsArray()
        assertEquals(3, commits.size)
        assertEquals("main", commits[2].branch)
    }

    @Test
    fun testCommentsSkipped() {
        val input = """
            gitGraph
               %% This is a comment
               commit
               %% Another comment
               commit
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(2, db.getCommitsArray().size)
    }

    @Test
    fun testEmptyGitGraph() {
        val input = """
            gitGraph
        """.trimIndent()
        parser.parse(input, db)

        assertEquals(0, db.getCommitsArray().size)
    }

    @Test
    fun testClearOnParse() {
        val input1 = """
            gitGraph
               commit
               commit
        """.trimIndent()
        parser.parse(input1, db)
        assertEquals(2, db.getCommitsArray().size)

        val input2 = """
            gitGraph
               commit
        """.trimIndent()
        parser.parse(input2, db)
        assertEquals(1, db.getCommitsArray().size)
    }
}
