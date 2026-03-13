package io.lugf027.github.mermaid.core.diagram.journey

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Journey 图表渲染器 - 对标 mermaid-js journeyRenderer.ts + svgDraw.js
 *
 * 自定义渲染模式：
 * - 左侧显示 actor 图例（彩色圆圈 + 名称）
 * - 中央按 section 分组显示任务条
 * - 每个任务下方用虚线连接到表情脸（分值映射）
 * - 底部水平时间轴线
 */
class JourneyRenderer : DiagramRenderer {

    companion object {
        // mermaid-js 默认 actor 颜色
        private val ACTOR_COLOURS = listOf(
            "#8FBC8F", "#7CFC00", "#00FFFF", "#20B2AA", "#B0E0E6", "#FFFFE0"
        )

        // mermaid-js 默认 section 填充色
        private val SECTION_FILLS = listOf(
            "#191970", "#8B008B", "#4B0082", "#2F4F4F", "#800000", "#8B4513", "#00008B"
        )

        // section 文字颜色
        private val SECTION_COLOURS = listOf("#fff")

        // 表情脸颜色
        private const val FACE_COLOR = "#FFF8DC"
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val journeyDb = db as? JourneyDb ?: throw IllegalArgumentException("Expected JourneyDb")

        val conf = config.journey ?: io.lugf027.github.mermaid.core.config.JourneyDiagramConfig()
        val tasks = journeyDb.getTasks()
        val title = journeyDb.getDiagramTitle()
        val actors = journeyDb.getActors()
        val sections = journeyDb.getSections()

        // 配置常量
        val confWidth = conf.width ?: 150
        val confHeight = conf.height ?: 50
        val diagramMarginX = conf.diagramMarginX ?: 50
        val diagramMarginY = conf.diagramMarginY ?: 10
        val taskMargin = conf.taskMargin ?: 50
        val taskFontSize = conf.taskFontSize ?: 14
        val leftMarginConf = conf.leftMargin ?: 150

        // 为每个 actor 分配颜色
        val actorColorMap = mutableMapOf<String, String>()
        actors.forEachIndexed { index, actor ->
            actorColorMap[actor] = ACTOR_COLOURS[index % ACTOR_COLOURS.size]
        }

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("width", "100%")
            attr("style", "max-width: 800px;")

            // 1. Arrowhead marker 定义
            defs {
                marker {
                    attr("id", "arrowhead")
                    attr("refX", "5")
                    attr("refY", "2")
                    attr("markerWidth", "6")
                    attr("markerHeight", "4")
                    attr("orient", "auto")
                    path("M 0,0 V 4 L6,2 Z")
                }
            }

            // 2. Actor Legend (左侧)
            var actorYPos = 60.0
            var maxActorWidth = 0.0

            for ((index, actor) in actors.withIndex()) {
                // 彩色圆圈
                circle(20.0, actorYPos, 7.0) {
                    addClass("actor-$index")
                    attr("fill", actorColorMap[actor] ?: "#8FBC8F")
                    attr("stroke", "#000")
                }

                // actor 名称文字
                val actorWidth = TextUtils.estimateTextWidth(actor, taskFontSize.toDouble())
                text(actor, 40.0, actorYPos + 5.0) {
                    addClass("legend")
                    attr("fill", "#666")
                    attr("font-size", taskFontSize.toString())
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                }
                if (actorWidth > maxActorWidth) maxActorWidth = actorWidth
                actorYPos += max(20.0, 20.0)
            }

            // 计算左边距
            val leftMargin = leftMarginConf + maxActorWidth

            // 3. Section 背景和任务
            var sectionNumber = 0
            var currentSectionName = ""
            var taskIndex = 0
            val sectionVHeight = confHeight * 2 + diagramMarginY
            val taskPos = sectionVHeight.toDouble()

            // 按 section 分组绘制任务
            for (task in tasks) {
                // 新 section 开始时绘制 section 背景
                if (task.section != currentSectionName) {
                    currentSectionName = task.section

                    // 计算这个 section 有多少个任务
                    val tasksInSection = tasks.count { it.section == currentSectionName }

                    // section 背景矩形
                    val sectionX = taskIndex * taskMargin + taskIndex * confWidth + leftMargin
                    val sectionWidth = confWidth * tasksInSection + diagramMarginX * (tasksInSection - 1)
                    val sectionFill = SECTION_FILLS[sectionNumber % SECTION_FILLS.size]
                    val sectionColor = SECTION_COLOURS[sectionNumber % SECTION_COLOURS.size]

                    group {
                        // section 背景
                        rect(sectionX.toDouble(), 50.0, sectionWidth.toDouble(), confHeight.toDouble()) {
                            addClass("journey-section section-type-$sectionNumber")
                            attr("rx", "3")
                            attr("ry", "3")
                            attr("fill", sectionFill)
                        }
                        // section 文字
                        text(currentSectionName, sectionX + sectionWidth / 2.0, 50.0 + confHeight / 2.0 + 5.0) {
                            attr("text-anchor", "middle")
                            attr("font-size", taskFontSize.toString())
                            attr("fill", sectionColor)
                            attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                        }
                    }
                    sectionNumber++
                }

                // 绘制 task
                val taskX = taskIndex * taskMargin + taskIndex * confWidth + leftMargin
                val taskY = taskPos
                val center = taskX + confWidth / 2.0

                group {
                    // 垂直虚线
                    line(center, taskY, center, 450.0) {
                        addClass("task-line")
                        attr("stroke-dasharray", "4 2")
                        attr("stroke", "#666")
                    }

                    // 表情脸 - score 映射 y 坐标
                    val faceY = 300.0 + (5 - task.score) * 30.0
                    drawFace(this@group, center, faceY, task.score)

                    // 任务矩形
                    rect(taskX.toDouble(), taskY, confWidth.toDouble(), confHeight.toDouble()) {
                        addClass("task task-type-${(sectionNumber - 1) % SECTION_FILLS.size}")
                        attr("rx", "3")
                        attr("ry", "3")
                    }

                    // 参与者圆圈（在任务矩形顶部）
                    var xPos = taskX + 14.0
                    for (person in task.people) {
                        val actorIndex = actors.indexOf(person)
                        if (actorIndex >= 0) {
                            circle(xPos, taskY, 7.0) {
                                addClass("actor-$actorIndex")
                                attr("fill", actorColorMap[person] ?: "#8FBC8F")
                                attr("stroke", "#000")
                                title(person)
                            }
                            xPos += 10.0
                        }
                    }

                    // 任务文字
                    text(task.task, taskX + confWidth / 2.0, taskY + confHeight / 2.0 + 5.0) {
                        addClass("task")
                        attr("text-anchor", "middle")
                        attr("font-size", taskFontSize.toString())
                        attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                    }
                }

                taskIndex++
            }

            // 4. 标题（如果有）
            val extraVertForTitle: Double
            if (title.isNotEmpty()) {
                text(title, leftMargin.toDouble(), 25.0) {
                    addClass("titleText")
                    attr("font-size", "4ex")
                    attr("font-weight", "bold")
                    attr("fill", themeVariables.titleColor)
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                }
                extraVertForTitle = 70.0
            } else {
                extraVertForTitle = 0.0
            }

            // 5. 水平活动线（箭头）
            val totalWidth = leftMargin + taskIndex * (taskMargin + confWidth) + diagramMarginX * 2
            line(leftMargin.toDouble(), confHeight * 4.0, totalWidth - leftMargin - 4.0, confHeight * 4.0) {
                attr("stroke-width", "4")
                attr("stroke", "black")
                attr("marker-end", "url(#arrowhead)")
            }

            // 6. 设置 viewBox
            val height = 450.0 + 2.0 * diagramMarginY
            viewBox(0.0, -25.0, totalWidth.toDouble(), height + extraVertForTitle)
            attr("preserveAspectRatio", "xMinYMin meet")
            attr("height", SvgElement.formatNumber(height + extraVertForTitle + 25.0))
        }
    }

    /**
     * 绘制表情脸
     */
    private fun drawFace(parent: SvgElement, cx: Double, cy: Double, score: Int) {
        // 脸圈
        parent.circle(cx, cy, 15.0) {
            addClass("face")
            attr("stroke-width", "2")
            attr("overflow", "visible")
        }

        // 眼睛
        parent.group {
            // 左眼
            circle(cx - 5.0, cy - 5.0, 1.5) {
                attr("fill", "#666")
                attr("stroke", "#666")
            }
            // 右眼
            circle(cx + 5.0, cy - 5.0, 1.5) {
                attr("fill", "#666")
                attr("stroke", "#666")
            }

            // 嘴巴
            if (score > 3) {
                // 微笑 - 弧线
                val mouthPath = buildSmileArc(cx, cy + 2.0)
                path(mouthPath) {
                    addClass("mouth")
                    attr("fill", "none")
                    attr("stroke", "#666")
                    attr("stroke-width", "1")
                }
            } else if (score < 3) {
                // 悲伤 - 反弧线
                val mouthPath = buildSadArc(cx, cy + 7.0)
                path(mouthPath) {
                    addClass("mouth")
                    attr("fill", "none")
                    attr("stroke", "#666")
                    attr("stroke-width", "1")
                }
            } else {
                // 中性 - 水平线
                line(cx - 5.0, cy + 7.0, cx + 5.0, cy + 7.0) {
                    addClass("mouth")
                    attr("stroke", "#666")
                    attr("stroke-width", "1")
                }
            }
        }
    }

    /**
     * 构建微笑弧线路径
     */
    private fun buildSmileArc(cx: Double, cy: Double): String {
        val r = 7.5
        // 半圆弧（下半部分） - 近似 d3.arc 的 startAngle=PI/2, endAngle=3*PI/2
        val startX = cx + r * cos(PI / 2)
        val startY = cy + r * sin(PI / 2)
        val endX = cx + r * cos(3 * PI / 2)
        val endY = cy + r * sin(3 * PI / 2)
        return "M ${SvgElement.formatNumber(startX)} ${SvgElement.formatNumber(startY)} " +
               "A $r $r 0 1 1 ${SvgElement.formatNumber(endX)} ${SvgElement.formatNumber(endY)}"
    }

    /**
     * 构建悲伤弧线路径
     */
    private fun buildSadArc(cx: Double, cy: Double): String {
        val r = 7.5
        // 半圆弧（上半部分）
        val startX = cx + r * cos(3 * PI / 2)
        val startY = cy + r * sin(3 * PI / 2)
        val endX = cx + r * cos(5 * PI / 2)
        val endY = cy + r * sin(5 * PI / 2)
        return "M ${SvgElement.formatNumber(startX)} ${SvgElement.formatNumber(startY)} " +
               "A $r $r 0 1 1 ${SvgElement.formatNumber(endX)} ${SvgElement.formatNumber(endY)}"
    }
}
