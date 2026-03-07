package io.lugf027.github.mermaid.core.diagrams.git

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.renderer.compose.toComposeColor
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.themes.getGitColors
import io.lugf027.github.mermaid.core.types.DiagramDB
import io.lugf027.github.mermaid.core.types.DiagramRenderer

/**
 * Git 图渲染器 — 自定义布局。
 */
class GitRenderer : DiagramRenderer {

    companion object {
        private const val COMMIT_RADIUS = 10f
        private const val LANE_HEIGHT = 40f
        private const val COMMIT_SPACING = 60f
        private const val MARGIN = 40f
        private const val LABEL_OFFSET = 16f
    }

    override fun draw(
        drawScope: DrawScope,
        db: DiagramDB,
        config: MermaidConfig,
        theme: ThemeVariables,
        textMeasurer: TextMeasurer,
        size: Size,
    ) {
        with(drawScope) {
            val gitDb = db as? GitDb ?: return
            val commits = gitDb.getCommits()
            val commitOrder = gitDb.getCommitOrder()
            val branches = gitDb.getBranches()

            if (commitOrder.isEmpty()) return

            val textColor = theme.primaryTextColor.toComposeColor()
            val commitStyle = TextStyle(fontSize = 11.sp, color = textColor)
            val tagStyle = TextStyle(fontSize = 10.sp, color = Color.White)

            // 分支颜色
            val gitColors = theme.getGitColors().map { it.toComposeColor() }
            val branchNames = branches.keys.toList()
            val branchLane = mutableMapOf<String, Int>()
            branchNames.forEachIndexed { idx, name -> branchLane[name] = idx }

            val isLR = gitDb.orientation == "LR"

            // 计算每个 commit 的位置
            data class CommitPos(val x: Float, val y: Float, val branch: String)
            val positions = mutableMapOf<String, CommitPos>()

            for ((idx, commitId) in commitOrder.withIndex()) {
                val commit = commits[commitId] ?: continue
                val lane = branchLane[commit.branch] ?: 0

                val x: Float
                val y: Float
                if (isLR) {
                    x = MARGIN + idx * COMMIT_SPACING
                    y = MARGIN + lane * LANE_HEIGHT
                } else {
                    x = MARGIN + lane * COMMIT_SPACING
                    y = MARGIN + idx * LANE_HEIGHT
                }
                positions[commitId] = CommitPos(x, y, commit.branch)
            }

            // 绘制连线（parent → child）
            for (commitId in commitOrder) {
                val commit = commits[commitId] ?: continue
                val pos = positions[commitId] ?: continue
                for (parentId in commit.parents) {
                    val parentPos = positions[parentId] ?: continue
                    val color = gitColors[(branchLane[commit.branch] ?: 0) % gitColors.size]
                    drawLine(
                        color = color,
                        start = Offset(parentPos.x, parentPos.y),
                        end = Offset(pos.x, pos.y),
                        strokeWidth = 2f,
                    )
                }
            }

            // 绘制 commit 圆点
            for (commitId in commitOrder) {
                val commit = commits[commitId] ?: continue
                val pos = positions[commitId] ?: continue
                val laneIdx = branchLane[commit.branch] ?: 0
                val color = gitColors[laneIdx % gitColors.size]

                when (commit.type) {
                    CommitType.MERGE -> {
                        drawCircle(color, COMMIT_RADIUS, Offset(pos.x, pos.y))
                        drawCircle(Color.White, COMMIT_RADIUS - 3f, Offset(pos.x, pos.y))
                    }
                    CommitType.REVERSE -> {
                        drawCircle(color, COMMIT_RADIUS, Offset(pos.x, pos.y))
                        // X mark
                        drawLine(Color.White, Offset(pos.x - 4f, pos.y - 4f), Offset(pos.x + 4f, pos.y + 4f), 2f)
                        drawLine(Color.White, Offset(pos.x + 4f, pos.y - 4f), Offset(pos.x - 4f, pos.y + 4f), 2f)
                    }
                    CommitType.HIGHLIGHT -> {
                        drawCircle(color, COMMIT_RADIUS + 2f, Offset(pos.x, pos.y))
                        drawCircle(Color.White, COMMIT_RADIUS, Offset(pos.x, pos.y), style = Stroke(2f))
                        drawCircle(color, COMMIT_RADIUS - 3f, Offset(pos.x, pos.y))
                    }
                    else -> {
                        drawCircle(color, COMMIT_RADIUS, Offset(pos.x, pos.y))
                    }
                }

                // Commit ID 标签
                val label = commit.id.take(7)
                val result = textMeasurer.measure(label, commitStyle)
                drawText(result, topLeft = Offset(pos.x - result.size.width / 2, pos.y + LABEL_OFFSET))

                // Tags
                for (tag in commit.tags) {
                    val tagResult = textMeasurer.measure(tag, tagStyle)
                    val tx = pos.x - tagResult.size.width / 2
                    val ty = pos.y - LABEL_OFFSET - tagResult.size.height
                    drawRoundRect(color, Offset(tx - 4f, ty - 2f), Size(tagResult.size.width + 8f, tagResult.size.height + 4f))
                    drawText(tagResult, topLeft = Offset(tx, ty))
                }
            }

            // 分支标签
            for ((branch, lane) in branchLane) {
                val branchResult = textMeasurer.measure(branch, commitStyle)
                if (isLR) {
                    drawText(branchResult, topLeft = Offset(4f, MARGIN + lane * LANE_HEIGHT - branchResult.size.height / 2))
                } else {
                    drawText(branchResult, topLeft = Offset(MARGIN + lane * COMMIT_SPACING - branchResult.size.width / 2, 4f))
                }
            }
        }
    }
}
