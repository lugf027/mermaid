package io.lugf027.github.mermaid.core.diagram.gitGraph

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * Git 图解析器 - 对标 mermaid-js gitGraphParser.ts
 *
 * 支持语法：gitGraph [LR|TB|BT]:
 *   commit [id:"id"] [msg:"msg"] [tag:"tag"] [type: NORMAL|REVERSE|HIGHLIGHT]
 *   branch name [order: N]
 *   checkout|switch name
 *   merge name [id:"id"] [tag:"tag"] [type: NORMAL|REVERSE|HIGHLIGHT]
 *   cherry-pick id:"id" [tag:"tag"] [parent:"id"]
 */
class GitGraphParser : DiagramParser {

    // 关键字正则
    private val RE_GITGRAPH = Regex("^\\s*gitGraph\\s*(LR|TB|BT)?\\s*:?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_COMMIT = Regex("^\\s*commit\\b(.*)$", RegexOption.IGNORE_CASE)
    private val RE_BRANCH = Regex("^\\s*branch\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_CHECKOUT = Regex("^\\s*(?:checkout|switch)\\s+(\\S+)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_MERGE = Regex("^\\s*merge\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_CHERRY_PICK = Regex("^\\s*cherry-pick\\b(.*)$", RegexOption.IGNORE_CASE)

    // commit/merge 参数解析
    private val RE_ID = Regex("""id:\s*"([^"]+)"""")
    private val RE_MSG = Regex("""msg:\s*"([^"]+)"""")
    private val RE_TAG = Regex("""tag:\s*"([^"]+)"""")
    private val RE_TYPE = Regex("""type:\s*(NORMAL|REVERSE|HIGHLIGHT)""", RegexOption.IGNORE_CASE)
    private val RE_PARENT = Regex("""parent:\s*"([^"]+)"""")
    private val RE_ORDER = Regex("""order:\s*(\d+)""")

    // 标题和无障碍
    private val RE_TITLE = Regex("^\\s*title\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR_MULTI_START = Regex("^\\s*accDescr\\s*\\{\\s*$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val gitDb = db as GitGraphDb
        gitDb.clear()

        val lines = text.lines()
        var started = false
        var inAccDescr = false
        val accDescrBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            // 跳过空行和注释
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            // 多行 accDescr 处理
            if (inAccDescr) {
                if (trimmed == "}") {
                    inAccDescr = false
                    gitDb.setAccDescription(accDescrBuilder.toString().trim())
                } else {
                    accDescrBuilder.appendLine(trimmed)
                }
                continue
            }

            // 匹配图表声明
            if (!started) {
                val gitMatch = RE_GITGRAPH.find(trimmed)
                if (gitMatch != null) {
                    val dir = gitMatch.groupValues[1]
                    if (dir.isNotEmpty()) {
                        gitDb.setDirection(dir.uppercase())
                    }
                    started = true
                }
                continue
            }

            // 标题/无障碍
            RE_TITLE.find(trimmed)?.let {
                gitDb.setDiagramTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_TITLE.find(trimmed)?.let {
                gitDb.setAccTitle(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            RE_ACC_DESCR.find(trimmed)?.let {
                gitDb.setAccDescription(it.groupValues[1].trim())
                return@let
            }?.also { continue }

            if (RE_ACC_DESCR_MULTI_START.matches(trimmed)) {
                inAccDescr = true
                accDescrBuilder.clear()
                continue
            }

            // commit
            RE_COMMIT.find(trimmed)?.let { m ->
                val args = m.groupValues[1]
                val id = RE_ID.find(args)?.groupValues?.get(1) ?: ""
                val msg = RE_MSG.find(args)?.groupValues?.get(1) ?: ""
                val tags = RE_TAG.findAll(args).map { it.groupValues[1] }.toList()
                val type = parseCommitType(RE_TYPE.find(args)?.groupValues?.get(1))

                gitDb.commit(id = id, message = msg, type = type, tags = tags)
                return@let
            }?.also { continue }

            // branch
            RE_BRANCH.find(trimmed)?.let { m ->
                val args = m.groupValues[1].trim()
                val order = RE_ORDER.find(args)?.groupValues?.get(1)?.toIntOrNull()
                // 分支名是第一个 token（去掉 order: 部分）
                val name = args.replace(RE_ORDER, "").trim()
                    .removeSurrounding("\"")
                    .trim()
                if (name.isNotEmpty()) {
                    gitDb.branch(name, order)
                }
                return@let
            }?.also { continue }

            // checkout / switch
            RE_CHECKOUT.find(trimmed)?.let { m ->
                val branchName = m.groupValues[1].removeSurrounding("\"")
                gitDb.checkout(branchName)
                return@let
            }?.also { continue }

            // merge
            RE_MERGE.find(trimmed)?.let { m ->
                val args = m.groupValues[1].trim()
                val id = RE_ID.find(args)?.groupValues?.get(1) ?: ""
                val tags = RE_TAG.findAll(args).map { it.groupValues[1] }.toList()
                val type = parseCommitType(RE_TYPE.find(args)?.groupValues?.get(1))

                // 分支名是第一个 token
                val branchName = args.split("\\s+".toRegex())[0].removeSurrounding("\"")
                gitDb.merge(branchName, id, type, tags)
                return@let
            }?.also { continue }

            // cherry-pick
            RE_CHERRY_PICK.find(trimmed)?.let { m ->
                val args = m.groupValues[1]
                val sourceId = RE_ID.find(args)?.groupValues?.get(1) ?: ""
                val parentId = RE_PARENT.find(args)?.groupValues?.get(1) ?: ""
                val tags = RE_TAG.findAll(args).map { it.groupValues[1] }.toList()

                if (sourceId.isNotEmpty()) {
                    gitDb.cherryPick(sourceId, parentId, tags.ifEmpty { null })
                }
                return@let
            }
        }
    }

    private fun parseCommitType(typeStr: String?): Int {
        if (typeStr == null) return GitGraphDb.CommitType.NORMAL
        return when (typeStr.uppercase()) {
            "REVERSE" -> GitGraphDb.CommitType.REVERSE
            "HIGHLIGHT" -> GitGraphDb.CommitType.HIGHLIGHT
            else -> GitGraphDb.CommitType.NORMAL
        }
    }
}
