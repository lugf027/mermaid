package io.lugf027.github.mermaid.core.diagram.gitGraph

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * Git 图数据库 - 对标 mermaid-js gitGraphAst.ts
 *
 * 存储 commit/branch/merge/checkout/cherry-pick 等操作的结果。
 */
class GitGraphDb : DiagramDB {

    /** 提交类型 */
    object CommitType {
        const val NORMAL = 0
        const val REVERSE = 1
        const val HIGHLIGHT = 2
        const val MERGE = 3
        const val CHERRY_PICK = 4
    }

    /** 提交数据 */
    data class Commit(
        val id: String,
        val message: String = "",
        val seq: Int,
        val type: Int = CommitType.NORMAL,
        val tags: List<String> = emptyList(),
        val parents: List<String> = emptyList(),
        val branch: String,
        val customType: Int? = null,
        val customId: Boolean = false
    )

    /** 方向 */
    enum class Direction { LR, TB, BT }

    // --- 内部状态 ---
    private val commits = linkedMapOf<String, Commit>()
    private var head: Commit? = null
    private val branchConfig = linkedMapOf<String, Pair<String, Int?>>() // name -> (name, order?)
    private val branches = linkedMapOf<String, String?>() // branchName -> headCommitId
    private var currBranch = "main"
    private var direction = Direction.LR
    private var seq = 0

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    init {
        initMainBranch()
    }

    private fun initMainBranch() {
        branchConfig["main"] = Pair("main", 0)
        branches["main"] = null
        currBranch = "main"
    }

    override fun clear() {
        commits.clear()
        head = null
        branchConfig.clear()
        branches.clear()
        currBranch = "main"
        direction = Direction.LR
        seq = 0
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
        initMainBranch()
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription

    override fun setDirection(direction: String) {
        this.direction = when (direction.uppercase()) {
            "TB" -> Direction.TB
            "BT" -> Direction.BT
            else -> Direction.LR
        }
    }

    override fun getDirection(): String = direction.name

    // --- Git 操作 ---

    fun setDirectionEnum(dir: Direction) { direction = dir }
    fun getDirectionEnum(): Direction = direction

    /**
     * 创建提交
     */
    fun commit(
        id: String = "",
        message: String = "",
        type: Int = CommitType.NORMAL,
        tags: List<String> = emptyList()
    ) {
        val commitId = if (id.isNotEmpty()) id else generateId()
        val customId = id.isNotEmpty()
        val parents = if (head != null) listOf(head!!.id) else emptyList()

        val commit = Commit(
            id = commitId,
            message = message,
            seq = seq++,
            type = type,
            tags = tags,
            parents = parents,
            branch = currBranch,
            customId = customId
        )

        commits[commitId] = commit
        head = commit
        branches[currBranch] = commitId
    }

    /**
     * 创建分支
     */
    fun branch(name: String, order: Int? = null) {
        if (branches.containsKey(name)) {
            throw IllegalStateException("Trying to create an existing branch: $name")
        }
        branchConfig[name] = Pair(name, order)
        branches[name] = head?.id
        checkout(name)
    }

    /**
     * 合并分支
     */
    fun merge(
        branch: String,
        id: String = "",
        type: Int? = null,
        tags: List<String> = emptyList()
    ) {
        val otherHeadId = branches[branch]
            ?: throw IllegalStateException("Branch $branch has no commits or does not exist")

        if (branch == currBranch) {
            throw IllegalStateException("Cannot merge a branch into itself: $branch")
        }

        val headId = head?.id
            ?: throw IllegalStateException("Current branch $currBranch has no commits")

        if (headId == otherHeadId) {
            throw IllegalStateException("Branches are already at the same commit, cannot merge")
        }

        val commitId = if (id.isNotEmpty()) id else generateId()
        val commit = Commit(
            id = commitId,
            message = "merged branch $branch into $currBranch",
            seq = seq++,
            type = CommitType.MERGE,
            tags = tags,
            parents = listOf(headId, otherHeadId),
            branch = currBranch,
            customType = type,
            customId = id.isNotEmpty()
        )

        commits[commitId] = commit
        head = commit
        branches[currBranch] = commitId
    }

    /**
     * 樱桃拣选
     */
    fun cherryPick(
        sourceId: String,
        parentId: String = "",
        tags: List<String>? = null
    ) {
        val sourceCommit = commits[sourceId]
            ?: throw IllegalStateException("Cannot cherry-pick: commit $sourceId does not exist")

        val parents = if (head != null) listOf(head!!.id, sourceId) else listOf(sourceId)

        val defaultTag = if (sourceCommit.type == CommitType.MERGE && parentId.isNotEmpty()) {
            "cherry-pick:${sourceId}|parent:${parentId}"
        } else {
            "cherry-pick:${sourceId}"
        }

        val finalTags = tags ?: listOf(defaultTag)
        val commitId = generateId()

        val commit = Commit(
            id = commitId,
            message = "cherry-picked from $sourceId",
            seq = seq++,
            type = CommitType.CHERRY_PICK,
            tags = finalTags,
            parents = parents,
            branch = currBranch
        )

        commits[commitId] = commit
        head = commit
        branches[currBranch] = commitId
    }

    /**
     * 切换分支
     */
    fun checkout(branchName: String) {
        if (!branches.containsKey(branchName)) {
            throw IllegalStateException("Branch $branchName does not exist")
        }
        currBranch = branchName
        val headId = branches[branchName]
        head = if (headId != null) commits[headId] else null
    }

    // --- 查询 ---

    fun getCommits(): Map<String, Commit> = commits

    fun getCommitsArray(): List<Commit> = commits.values.sortedBy { it.seq }

    fun getBranches(): Map<String, String?> = branches

    /**
     * 获取排序后的分支数组
     */
    fun getBranchesAsObjArray(): List<String> {
        return branchConfig.entries
            .sortedWith(compareBy { it.value.second ?: 0 })
            .map { it.key }
    }

    fun getCurrentBranch(): String = currBranch

    fun getHead(): Commit? = head

    fun getMainBranchName(): String = branchConfig.keys.firstOrNull() ?: "main"

    // --- 内部工具 ---

    private var idCounter = 0

    private fun generateId(): String {
        val seqStr = seq.toString()
        val random = buildString {
            repeat(7) {
                append("0123456789abcdef"[(idCounter++ * 31 + seq * 17) % 16])
            }
        }
        return "$seqStr-$random"
    }
}
