package io.lugf027.github.mermaid.core.diagrams.git

/**
 * Git 图类型定义。
 */

enum class CommitType { NORMAL, REVERSE, HIGHLIGHT, MERGE, CHERRY_PICK }

data class GitCommit(
    val id: String,
    val message: String = "",
    val tags: MutableList<String> = mutableListOf(),
    val parents: MutableList<String> = mutableListOf(),
    val branch: String = "main",
    val type: CommitType = CommitType.NORMAL,
    val seq: Int = 0,
)
