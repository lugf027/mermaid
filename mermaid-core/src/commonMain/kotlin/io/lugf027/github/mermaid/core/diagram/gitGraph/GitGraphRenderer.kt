package io.lugf027.github.mermaid.core.diagram.gitGraph

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.rendering.svg.buildSvg
import io.lugf027.github.mermaid.core.rendering.svg.SvgElement.Companion.formatNumber
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Git 图渲染器 - 对标 mermaid-js gitGraphRenderer.ts
 *
 * 自定义渲染模式：commit 圆点 + branch 线 + 箭头连线 + 标签/标记
 */
class GitGraphRenderer : DiagramRenderer {

    companion object {
        const val LAYOUT_OFFSET = 10
        const val COMMIT_STEP = 40
        const val PX = 4
        const val PY = 2
        const val THEME_COLOR_LIMIT = 8
        const val DEFAULT_POS = 30
        const val ARROW_RADIUS = 20
    }

    // 预定义的 git 颜色（对标 theme-default.js git0-git7）
    private val defaultGitColors = listOf(
        "#979797", "#F5F5F5", "#E8E8E8", "#BDB76B",
        "#7B68EE", "#6B8E23", "#FF69B4", "#BA55D3"
    )

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val gitDb = db as GitGraphDb
        val dir = gitDb.getDirectionEnum()
        val commitsArray = gitDb.getCommitsArray()
        val allCommits = gitDb.getCommits()
        val sortedBranches = gitDb.getBranchesAsObjArray()
        val showBranches = config.gitGraph?.showBranches ?: true
        val showCommitLabel = config.gitGraph?.showCommitLabel ?: true
        val rotateCommitLabel = config.gitGraph?.rotateCommitLabel ?: true
        val title = gitDb.getDiagramTitle()

        // 获取颜色
        val gitColors = getGitColors(themeVariables)

        // 1. 计算分支位置
        val branchPos = linkedMapOf<String, BranchPosition>()
        var pos = 0.0
        for ((index, branchName) in sortedBranches.withIndex()) {
            val labelWidth = if (showBranches) {
                TextUtils.estimateTextWidth(branchName, 14.0) + 20
            } else 0.0
            branchPos[branchName] = BranchPosition(pos, index)
            pos += 50.0 + (if (rotateCommitLabel) 40.0 else 0.0) +
                    (if (dir == GitGraphDb.Direction.TB || dir == GitGraphDb.Direction.BT) labelWidth / 2 else 0.0)
        }

        // 2. 计算提交位置（第一遍，不绘制）
        val commitPos = linkedMapOf<String, CommitPosition>()
        var currentPos = 0.0
        var maxPos = 0.0
        for (commit in commitsArray) {
            val bp = branchPos[commit.branch] ?: continue
            val posWithOffset = currentPos + LAYOUT_OFFSET

            val cp = when (dir) {
                GitGraphDb.Direction.LR -> CommitPosition(posWithOffset, bp.pos)
                GitGraphDb.Direction.TB -> CommitPosition(bp.pos, posWithOffset)
                GitGraphDb.Direction.BT -> CommitPosition(bp.pos, posWithOffset)
            }
            commitPos[commit.id] = cp
            currentPos += COMMIT_STEP + LAYOUT_OFFSET
            maxPos = currentPos
        }

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            // 3. 绘制分支线
            if (showBranches) {
                group {
                    for ((branchName, bp) in branchPos) {
                        val colorIdx = bp.index % THEME_COLOR_LIMIT
                        val color = gitColors.getOrElse(colorIdx) { "#979797" }

                        when (dir) {
                            GitGraphDb.Direction.LR -> {
                                line(0.0, bp.pos, maxPos, bp.pos) {
                                    addClass("branch branch$colorIdx")
                                    attr("stroke", color)
                                    attr("stroke-width", "1")
                                    attr("stroke-dasharray", "2")
                                }
                            }
                            GitGraphDb.Direction.TB -> {
                                line(bp.pos, DEFAULT_POS.toDouble(), bp.pos, maxPos) {
                                    addClass("branch branch$colorIdx")
                                    attr("stroke", color)
                                    attr("stroke-width", "1")
                                    attr("stroke-dasharray", "2")
                                }
                            }
                            GitGraphDb.Direction.BT -> {
                                line(bp.pos, maxPos, bp.pos, DEFAULT_POS.toDouble()) {
                                    addClass("branch branch$colorIdx")
                                    attr("stroke", color)
                                    attr("stroke-width", "1")
                                    attr("stroke-dasharray", "2")
                                }
                            }
                        }

                        // 分支标签
                        val labelWidth = TextUtils.estimateTextWidth(branchName, 14.0)
                        when (dir) {
                            GitGraphDb.Direction.LR -> {
                                val labelX = -(labelWidth + 14 + (if (rotateCommitLabel) 30 else 0))
                                val labelY = bp.pos - 8.0
                                rect(labelX, labelY, labelWidth + 14, 20.0) {
                                    addClass("branchLabelBkg label$colorIdx")
                                    attr("rx", "4")
                                    attr("ry", "4")
                                    attr("fill", color)
                                }
                                group {
                                    addClass("branchLabel")
                                    group {
                                        addClass("label branch-label$colorIdx")
                                        text(branchName, labelX + 7, bp.pos + 4.0) {
                                            attr("fill", "lightgrey")
                                            attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                                        }
                                    }
                                }
                            }
                            GitGraphDb.Direction.TB, GitGraphDb.Direction.BT -> {
                                val lblX = bp.pos - labelWidth / 2 - 10
                                val lblY = if (dir == GitGraphDb.Direction.BT) maxPos else 0.0
                                rect(lblX, lblY, labelWidth + 20, 20.0) {
                                    addClass("branchLabelBkg label$colorIdx")
                                    attr("rx", "4")
                                    attr("ry", "4")
                                    attr("fill", color)
                                }
                                group {
                                    addClass("branchLabel")
                                    group {
                                        addClass("label branch-label$colorIdx")
                                        text(branchName, bp.pos - labelWidth / 2 - 5, lblY + 14.0) {
                                            attr("fill", "lightgrey")
                                            attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. 绘制箭头/连接线
            group {
                addClass("commit-arrows")
                for (commit in commitsArray) {
                    val childPos = commitPos[commit.id] ?: continue
                    for ((pIdx, parentId) in commit.parents.withIndex()) {
                        val parentPos = commitPos[parentId] ?: continue
                        val parentCommit = allCommits[parentId] ?: continue
                        val bp = branchPos[commit.branch] ?: continue

                        // 颜色: merge 的第二个父使用父提交分支颜色
                        val colorIdx = if (commit.type == GitGraphDb.CommitType.MERGE && pIdx > 0) {
                            (branchPos[parentCommit.branch]?.index ?: bp.index) % THEME_COLOR_LIMIT
                        } else {
                            bp.index % THEME_COLOR_LIMIT
                        }
                        val color = gitColors.getOrElse(colorIdx) { "#979797" }

                        val pathD = buildArrowPath(parentPos, childPos, dir, commit.type == GitGraphDb.CommitType.MERGE && pIdx > 0)
                        path(pathD) {
                            addClass("arrow arrow$colorIdx")
                            attr("stroke", color)
                            attr("stroke-width", "8")
                            attr("stroke-linecap", "round")
                            attr("fill", "none")
                        }
                    }
                }
            }

            // 5. 绘制提交圆点
            group {
                addClass("commit-bullets")
                for (commit in commitsArray) {
                    val cp = commitPos[commit.id] ?: continue
                    val bp = branchPos[commit.branch] ?: continue
                    val colorIdx = bp.index % THEME_COLOR_LIMIT
                    val color = gitColors.getOrElse(colorIdx) { "#979797" }

                    drawCommitBullet(commit, cp, colorIdx, color, themeVariables)
                }
            }

            // 6. 绘制提交标签和标记
            group {
                addClass("commit-labels")
                for (commit in commitsArray) {
                    val cp = commitPos[commit.id] ?: continue
                    val bp = branchPos[commit.branch] ?: continue
                    val colorIdx = bp.index % THEME_COLOR_LIMIT

                    // 提交标签 (commit ID)
                    if (showCommitLabel && shouldShowLabel(commit)) {
                        drawCommitLabel(commit, cp, colorIdx, dir, rotateCommitLabel, themeVariables)
                    }

                    // Tag 标签
                    if (commit.tags.isNotEmpty()) {
                        drawCommitTags(commit, cp, colorIdx, dir, themeVariables)
                    }
                }
            }

            // 7. 标题
            if (title.isNotEmpty()) {
                text(title, maxPos / 2, -20.0) {
                    addClass("gitTitleText")
                    attr("text-anchor", "middle")
                    attr("font-size", "18")
                    attr("fill", themeVariables.textColor)
                }
            }

            // 8. 计算 viewBox
            val padding = config.gitGraph?.diagramPadding ?: 8
            val vbMinX = if (showBranches) {
                val maxLabelW = sortedBranches.maxOfOrNull { TextUtils.estimateTextWidth(it, 14.0) } ?: 0.0
                -(maxLabelW + 40)
            } else -padding.toDouble()
            val vbMinY = -(padding.toDouble() + 40 + (if (title.isNotEmpty()) 30 else 0))
            val vbMaxX = maxPos + padding * 2
            val vbMaxY = pos + padding * 2 + 50

            viewBox(vbMinX, vbMinY, max(vbMaxX - vbMinX, 100.0), max(vbMaxY - vbMinY, 100.0))
            attr("width", "100%")
            attr("style", "max-width: ${max(vbMaxX - vbMinX, 100.0).toInt()}px;")
        }
    }

    private fun shouldShowLabel(commit: GitGraphDb.Commit): Boolean {
        if (commit.type == GitGraphDb.CommitType.CHERRY_PICK) return false
        if (commit.type == GitGraphDb.CommitType.MERGE && !commit.customId) return false
        return true
    }

    private fun buildArrowPath(
        p1: CommitPosition, p2: CommitPosition,
        dir: GitGraphDb.Direction, isMergeSecondParent: Boolean
    ): String {
        val r = ARROW_RADIUS.toDouble()
        return when (dir) {
            GitGraphDb.Direction.LR -> buildArrowPathLR(p1, p2, r, isMergeSecondParent)
            GitGraphDb.Direction.TB -> buildArrowPathTB(p1, p2, r, isMergeSecondParent)
            GitGraphDb.Direction.BT -> buildArrowPathBT(p1, p2, r, isMergeSecondParent)
        }
    }

    private fun buildArrowPathLR(p1: CommitPosition, p2: CommitPosition, r: Double, isMerge2nd: Boolean): String {
        if (abs(p1.y - p2.y) < 0.01) {
            return "M ${p1.x} ${p1.y} L ${p2.x} ${p2.y}"
        }
        return if (p1.y < p2.y) {
            if (isMerge2nd) {
                "M ${p1.x} ${p1.y} L ${p2.x - r} ${p1.y} A $r $r 0 0 1 ${p2.x} ${p1.y + r} L ${p2.x} ${p2.y}"
            } else {
                "M ${p1.x} ${p1.y} L ${p1.x} ${p2.y - r} A $r $r 0 0 0 ${p1.x + r} ${p2.y} L ${p2.x} ${p2.y}"
            }
        } else {
            if (isMerge2nd) {
                "M ${p1.x} ${p1.y} L ${p2.x - r} ${p1.y} A $r $r 0 0 0 ${p2.x} ${p1.y - r} L ${p2.x} ${p2.y}"
            } else {
                "M ${p1.x} ${p1.y} L ${p1.x} ${p2.y + r} A $r $r 0 0 1 ${p1.x + r} ${p2.y} L ${p2.x} ${p2.y}"
            }
        }
    }

    private fun buildArrowPathTB(p1: CommitPosition, p2: CommitPosition, r: Double, isMerge2nd: Boolean): String {
        if (abs(p1.x - p2.x) < 0.01) {
            return "M ${p1.x} ${p1.y} L ${p2.x} ${p2.y}"
        }
        return if (p1.x < p2.x) {
            if (isMerge2nd) {
                "M ${p1.x} ${p1.y} L ${p1.x} ${p2.y - r} A $r $r 0 0 0 ${p1.x + r} ${p2.y} L ${p2.x} ${p2.y}"
            } else {
                "M ${p1.x} ${p1.y} L ${p1.x} ${p1.y} L ${p2.x - r} ${p1.y} A $r $r 0 0 1 ${p2.x} ${p1.y + r} L ${p2.x} ${p2.y}"
            }
        } else {
            if (isMerge2nd) {
                "M ${p1.x} ${p1.y} L ${p1.x} ${p2.y - r} A $r $r 0 0 1 ${p1.x - r} ${p2.y} L ${p2.x} ${p2.y}"
            } else {
                "M ${p1.x} ${p1.y} L ${p2.x + r} ${p1.y} A $r $r 0 0 0 ${p2.x} ${p1.y + r} L ${p2.x} ${p2.y}"
            }
        }
    }

    private fun buildArrowPathBT(p1: CommitPosition, p2: CommitPosition, r: Double, isMerge2nd: Boolean): String {
        if (abs(p1.x - p2.x) < 0.01) {
            return "M ${p1.x} ${p1.y} L ${p2.x} ${p2.y}"
        }
        return if (p1.x < p2.x) {
            if (isMerge2nd) {
                "M ${p1.x} ${p1.y} L ${p1.x} ${p2.y + r} A $r $r 0 0 1 ${p1.x + r} ${p2.y} L ${p2.x} ${p2.y}"
            } else {
                "M ${p1.x} ${p1.y} L ${p2.x - r} ${p1.y} A $r $r 0 0 0 ${p2.x} ${p1.y - r} L ${p2.x} ${p2.y}"
            }
        } else {
            if (isMerge2nd) {
                "M ${p1.x} ${p1.y} L ${p1.x} ${p2.y + r} A $r $r 0 0 0 ${p1.x - r} ${p2.y} L ${p2.x} ${p2.y}"
            } else {
                "M ${p1.x} ${p1.y} L ${p2.x + r} ${p1.y} A $r $r 0 0 1 ${p2.x} ${p1.y - r} L ${p2.x} ${p2.y}"
            }
        }
    }

    /**
     * 绘制 commit 圆点 - 对标 drawCommitBullet
     */
    private fun SvgElement.drawCommitBullet(
        commit: GitGraphDb.Commit,
        cp: CommitPosition,
        colorIdx: Int,
        color: String,
        tv: ThemeVariables
    ) {
        val typeClass = getCommitClassType(commit.type)
        when (commit.type) {
            GitGraphDb.CommitType.HIGHLIGHT -> {
                // 外层 rect 20x20
                rect(cp.x - 10, cp.y - 10, 20.0, 20.0) {
                    addClass("commit ${commit.id} commit-highlight$colorIdx $typeClass-outer")
                }
                // 内层 rect 12x12
                rect(cp.x - 6, cp.y - 6, 12.0, 12.0) {
                    addClass("commit ${commit.id} commit$colorIdx $typeClass-inner")
                }
            }
            GitGraphDb.CommitType.CHERRY_PICK -> {
                circle(cp.x, cp.y, 10.0) {
                    addClass("commit ${commit.id} $typeClass")
                    attr("fill", color)
                    attr("stroke", color)
                }
                // 樱桃形状装饰
                circle(cp.x - 3, cp.y + 2, 2.75) {
                    attr("fill", "#fff")
                }
                circle(cp.x + 3, cp.y + 2, 2.75) {
                    attr("fill", "#fff")
                }
                line(cp.x - 3, cp.y + 1, cp.x, cp.y - 5) {
                    attr("stroke", "#fff")
                    attr("stroke-width", "1")
                }
                line(cp.x + 3, cp.y + 1, cp.x, cp.y - 5) {
                    attr("stroke", "#fff")
                    attr("stroke-width", "1")
                }
            }
            GitGraphDb.CommitType.MERGE -> {
                circle(cp.x, cp.y, 9.0) {
                    addClass("commit ${commit.id} commit$colorIdx")
                    attr("fill", color)
                    attr("stroke", color)
                }
                circle(cp.x, cp.y, 6.0) {
                    addClass("commit commit-merge ${commit.id} commit$colorIdx")
                    attr("fill", tv.primaryColor)
                    attr("stroke", tv.primaryColor)
                }
            }
            GitGraphDb.CommitType.REVERSE -> {
                circle(cp.x, cp.y, 10.0) {
                    addClass("commit ${commit.id} commit$colorIdx")
                    attr("fill", color)
                    attr("stroke", color)
                }
                // 叉号 X
                path("M ${cp.x - 5} ${cp.y - 5} L ${cp.x + 5} ${cp.y + 5} M ${cp.x - 5} ${cp.y + 5} L ${cp.x + 5} ${cp.y - 5}") {
                    addClass("commit $typeClass ${commit.id} commit$colorIdx")
                    attr("stroke", tv.primaryColor)
                    attr("stroke-width", "3")
                }
            }
            else -> {
                // NORMAL
                circle(cp.x, cp.y, 10.0) {
                    addClass("commit ${commit.id} commit$colorIdx")
                    attr("fill", color)
                    attr("stroke", color)
                }
            }
        }
    }

    /**
     * 绘制 commit 标签 - 对标 drawCommitLabel
     */
    private fun SvgElement.drawCommitLabel(
        commit: GitGraphDb.Commit,
        cp: CommitPosition,
        colorIdx: Int,
        dir: GitGraphDb.Direction,
        rotateLabel: Boolean,
        tv: ThemeVariables
    ) {
        val labelText = commit.id
        val labelWidth = TextUtils.estimateTextWidth(labelText, 10.0) + 2 * PY
        val labelHeight = 14.0

        group {
            when (dir) {
                GitGraphDb.Direction.LR -> {
                    val textY = cp.y + 25
                    rect(cp.x - labelWidth / 2 - PY, cp.y + 13.5, labelWidth + 2 * PY, labelHeight + 2 * PY) {
                        addClass("commit-label-bkg")
                        attr("fill", tv.secondaryColor)
                        attr("opacity", "0.5")
                    }
                    text(labelText, cp.x - labelWidth / 2, textY) {
                        addClass("commit-label")
                        attr("fill", tv.secondaryTextColor)
                        attr("font-size", "10")
                    }
                }
                GitGraphDb.Direction.TB, GitGraphDb.Direction.BT -> {
                    rect(cp.x - labelWidth - 4 * PX - 5, cp.y - 12, labelWidth + 4 * PX + 5, labelHeight + 2 * PY) {
                        addClass("commit-label-bkg")
                        attr("fill", tv.secondaryColor)
                        attr("opacity", "0.5")
                    }
                    text(labelText, cp.x - labelWidth - 4 * PX, cp.y + 2.0) {
                        addClass("commit-label")
                        attr("fill", tv.secondaryTextColor)
                        attr("font-size", "10")
                    }
                }
            }
        }
    }

    /**
     * 绘制 commit 标记 (tags) - 对标 drawCommitTags
     */
    private fun SvgElement.drawCommitTags(
        commit: GitGraphDb.Commit,
        cp: CommitPosition,
        colorIdx: Int,
        dir: GitGraphDb.Direction,
        tv: ThemeVariables
    ) {
        var yOffset = 0.0
        for (tag in commit.tags.reversed()) {
            val tagWidth = TextUtils.estimateTextWidth(tag, 10.0)
            val tagHeight = 12.0
            val h2 = tagHeight / 2

            when (dir) {
                GitGraphDb.Direction.LR -> {
                    val ly = cp.y - 19.2 - yOffset
                    // Tag 背景多边形（箭头形）
                    val pts = listOf(
                        Pair(cp.x - tagWidth / 2 - PX, ly + h2),
                        Pair(cp.x - tagWidth / 2 - PX, ly - h2),
                        Pair(cp.x + tagWidth / 2 + PX, ly - h2),
                        Pair(cp.x + tagWidth / 2 + PX + 10, ly),
                        Pair(cp.x + tagWidth / 2 + PX, ly + h2)
                    )
                    polygon(pts) {
                        addClass("tag-label-bkg")
                        attr("fill", tv.primaryColor)
                        attr("stroke", tv.primaryBorderColor)
                    }
                    // Tag 小孔
                    circle(cp.x - tagWidth / 2 + 2, ly, 1.5) {
                        addClass("tag-hole")
                        attr("fill", tv.textColor)
                    }
                    // Tag 文字
                    text(tag, cp.x - tagWidth / 2 + PX + 5, ly + 4.0) {
                        addClass("tag-label")
                        attr("fill", tv.primaryTextColor)
                        attr("font-size", "10")
                    }
                    yOffset += 20
                }
                GitGraphDb.Direction.TB, GitGraphDb.Direction.BT -> {
                    val tx = cp.x + 15 + yOffset
                    val ty = cp.y - 8
                    rect(tx, ty, tagWidth + 2 * PX + 10, tagHeight + 2 * PY) {
                        addClass("tag-label-bkg")
                        attr("fill", tv.primaryColor)
                        attr("stroke", tv.primaryBorderColor)
                        attr("rx", "3")
                    }
                    circle(tx + 3, ty + h2 + PY, 1.5) {
                        addClass("tag-hole")
                        attr("fill", tv.textColor)
                    }
                    text(tag, tx + PX + 6, ty + tagHeight) {
                        addClass("tag-label")
                        attr("fill", tv.primaryTextColor)
                        attr("font-size", "10")
                    }
                    yOffset += tagWidth + 25
                }
            }
        }
    }

    private fun getCommitClassType(type: Int): String = when (type) {
        GitGraphDb.CommitType.NORMAL -> "commit-normal"
        GitGraphDb.CommitType.REVERSE -> "commit-reverse"
        GitGraphDb.CommitType.HIGHLIGHT -> "commit-highlight"
        GitGraphDb.CommitType.MERGE -> "commit-merge"
        GitGraphDb.CommitType.CHERRY_PICK -> "commit-cherry-pick"
        else -> "commit-normal"
    }

    private fun getGitColors(tv: ThemeVariables): List<String> {
        return listOf(
            tv.git0, tv.git1, tv.git2, tv.git3,
            tv.git4, tv.git5, tv.git6, tv.git7
        )
    }

    private data class BranchPosition(val pos: Double, val index: Int)
    private data class CommitPosition(val x: Double, val y: Double)
}
