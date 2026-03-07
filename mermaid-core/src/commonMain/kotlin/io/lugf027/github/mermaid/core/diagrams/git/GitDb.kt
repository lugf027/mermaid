package io.lugf027.github.mermaid.core.diagrams.git

import io.lugf027.github.mermaid.core.db.CommonDb

/**
 * Git 图数据存储层。
 */
class GitDb : CommonDb() {

    private val commits = mutableMapOf<String, GitCommit>()
    private val branches = mutableMapOf<String, String?>() // branch -> head commit id
    private var currentBranch = "main"
    private var commitSeq = 0
    private val commitOrder = mutableListOf<String>()
    var orientation = "LR"
        private set

    init {
        branches["main"] = null
    }

    fun setOrientation(o: String) { orientation = o.uppercase() }

    fun commit(id: String? = null, message: String = "", type: CommitType = CommitType.NORMAL, tag: String? = null) {
        val commitId = id ?: "c${commitSeq}"
        val parents = mutableListOf<String>()
        val headCommit = branches[currentBranch]
        if (headCommit != null) parents.add(headCommit)

        val commit = GitCommit(
            id = commitId,
            message = message,
            tags = if (tag != null) mutableListOf(tag) else mutableListOf(),
            parents = parents,
            branch = currentBranch,
            type = type,
            seq = commitSeq++,
        )
        commits[commitId] = commit
        commitOrder.add(commitId)
        branches[currentBranch] = commitId
    }

    fun branch(name: String) {
        val headId = branches[currentBranch]
        branches[name] = headId
    }

    fun checkout(name: String) {
        if (branches.containsKey(name)) {
            currentBranch = name
        }
    }

    fun merge(branchName: String, id: String? = null, tag: String? = null) {
        val mergeCommitId = id ?: "merge_${commitSeq}"
        val parents = mutableListOf<String>()
        branches[currentBranch]?.let { parents.add(it) }
        branches[branchName]?.let { parents.add(it) }

        val commit = GitCommit(
            id = mergeCommitId,
            message = "Merge $branchName into $currentBranch",
            tags = if (tag != null) mutableListOf(tag) else mutableListOf(),
            parents = parents,
            branch = currentBranch,
            type = CommitType.MERGE,
            seq = commitSeq++,
        )
        commits[mergeCommitId] = commit
        commitOrder.add(mergeCommitId)
        branches[currentBranch] = mergeCommitId
    }

    fun cherryPick(commitId: String) {
        val srcCommit = commits[commitId] ?: return
        val newId = "cherry_${commitSeq}"
        val parents = mutableListOf<String>()
        branches[currentBranch]?.let { parents.add(it) }

        val commit = GitCommit(
            id = newId,
            message = "Cherry-pick ${srcCommit.id}",
            parents = parents,
            branch = currentBranch,
            type = CommitType.CHERRY_PICK,
            seq = commitSeq++,
        )
        commits[newId] = commit
        commitOrder.add(newId)
        branches[currentBranch] = newId
    }

    fun getCommits(): Map<String, GitCommit> = commits
    fun getCommitOrder(): List<String> = commitOrder
    fun getBranches(): Map<String, String?> = branches
    fun getCurrentBranch(): String = currentBranch

    override fun clear() {
        super.clear()
        commits.clear()
        branches.clear()
        commitOrder.clear()
        currentBranch = "main"
        commitSeq = 0
        branches["main"] = null
        orientation = "LR"
    }

}
