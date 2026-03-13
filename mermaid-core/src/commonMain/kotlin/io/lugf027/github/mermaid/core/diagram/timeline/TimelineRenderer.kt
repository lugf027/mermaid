package io.lugf027.github.mermaid.core.diagram.timeline

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.max

/**
 * Timeline 渲染器 - 对标 mermaid-js timelineRenderer.ts + svgDraw.js
 *
 * 自定义渲染模式：
 * - 水平排列时间段(periods)
 * - 每个时间段下方用虚线连接到事件(events)
 * - 可选 section 分区在时间段上方
 * - 水平时间轴线（带箭头）
 */
class TimelineRenderer : DiagramRenderer {

    companion object {
        private const val NODE_WIDTH = 150
        private const val NODE_PADDING = 20
        private const val TASK_HORIZONTAL_SPACING = 200
        private const val EVENT_VERTICAL_OFFSET = 100
        private const val EVENT_SPACING = 10
        private const val LEFT_MARGIN_DEFAULT = 50
        private const val FONT_SIZE = 14.0
        private const val RD = 5.0  // 圆角半径
        private const val MAX_SECTIONS = 12
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val timelineDb = db as? TimelineDb ?: throw IllegalArgumentException("Expected TimelineDb")

        val conf = config.timeline ?: io.lugf027.github.mermaid.core.config.TimelineDiagramConfig()
        val tasks = timelineDb.getTasks()
        val title = timelineDb.getDiagramTitle()
        val sections = timelineDb.getSections()

        val leftMargin = (conf.leftMargin ?: LEFT_MARGIN_DEFAULT)
        val disableMulticolor = conf.disableMulticolor ?: false

        // 计算节点高度
        val nodeHeight = estimateNodeHeight(FONT_SIZE, NODE_PADDING)
        val maxTaskHeight = nodeHeight + 20.0
        val maxSectionHeight = nodeHeight + 20.0

        return buildSvg {
            attr("id", diagramId)
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("width", "100%")

            // 1. Arrowhead marker
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

            // 初始坐标
            var masterX = 50.0 + leftMargin
            val sectionBeginY = 50.0
            var sectionColor = 0

            if (sections.isNotEmpty()) {
                // 有 sections: 按 section 分组
                for (section in sections) {
                    val sectionTasks = timelineDb.getTasksForSection(section)
                    val taskCount = max(sectionTasks.size, 1)

                    // 绘制 section 背景
                    val sectionWidth = TASK_HORIZONTAL_SPACING * taskCount - 50.0
                    drawTimelineNode(
                        parent = this,
                        text = section,
                        x = masterX,
                        y = sectionBeginY,
                        width = sectionWidth,
                        height = maxSectionHeight,
                        sectionNum = sectionColor,
                        cssClass = "timeline-node section-${sectionColor % (MAX_SECTIONS - 1)}"
                    )

                    // 绘制 tasks
                    val masterY = sectionBeginY + maxSectionHeight + 50.0
                    drawTimelineTasks(
                        parent = this,
                        tasks = sectionTasks,
                        masterX = masterX,
                        masterY = masterY,
                        maxTaskHeight = maxTaskHeight,
                        sectionColor = sectionColor,
                        isWithoutSections = false,
                        disableMulticolor = disableMulticolor
                    )

                    masterX += TASK_HORIZONTAL_SPACING * taskCount
                    sectionColor++
                }
            } else {
                // 无 sections: 直接绘制所有 tasks
                val masterY = 50.0
                drawTimelineTasks(
                    parent = this,
                    tasks = tasks,
                    masterX = masterX,
                    masterY = masterY,
                    maxTaskHeight = maxTaskHeight,
                    sectionColor = 0,
                    isWithoutSections = true,
                    disableMulticolor = disableMulticolor
                )
                masterX += TASK_HORIZONTAL_SPACING * tasks.size
            }

            // 标题
            val extraVertForTitle: Double
            if (title.isNotEmpty()) {
                val titleX = masterX / 2
                text(title, titleX, 20.0) {
                    attr("font-size", "4ex")
                    attr("font-weight", "bold")
                    attr("fill", themeVariables.titleColor)
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                    attr("text-anchor", "middle")
                }
                extraVertForTitle = 70.0
            } else {
                extraVertForTitle = 0.0
            }

            // 水平时间轴线
            val depthY = if (sections.isNotEmpty()) {
                maxSectionHeight + maxTaskHeight + 150.0
            } else {
                maxTaskHeight + 100.0
            }

            group {
                addClass("lineWrapper")
                line(leftMargin.toDouble(), depthY, masterX - leftMargin.toDouble(), depthY) {
                    attr("stroke-width", "4")
                    attr("stroke", "black")
                    attr("marker-end", "url(#arrowhead)")
                }
            }

            // 计算总尺寸
            val totalHeight = depthY + 100.0 + extraVertForTitle
            val totalWidth = masterX + 2 * leftMargin

            viewBox(0.0, 0.0, totalWidth.toDouble(), totalHeight)
            attr("style", "max-width: ${SvgElement.formatNumber(totalWidth)}px;")
        }
    }

    /**
     * 绘制时间线任务列表
     */
    private fun drawTimelineTasks(
        parent: SvgElement,
        tasks: List<TimelineTask>,
        masterX: Double,
        masterY: Double,
        maxTaskHeight: Double,
        sectionColor: Int,
        isWithoutSections: Boolean,
        disableMulticolor: Boolean
    ) {
        var currentX = masterX
        var currentSectionColor = sectionColor

        for (task in tasks) {
            val nodeWidth = NODE_WIDTH + 2.0 * NODE_PADDING

            // Task 节点
            parent.group {
                addClass("taskWrapper")
                translate(currentX, masterY)
                drawTimelineNodeInner(
                    parent = this,
                    text = task.task,
                    width = nodeWidth,
                    height = maxTaskHeight,
                    sectionNum = currentSectionColor,
                    cssClass = "timeline-node section-${currentSectionColor % (MAX_SECTIONS - 1)}"
                )
            }

            // Events
            if (task.events.isNotEmpty()) {
                // 虚线连接线
                parent.group {
                    addClass("lineWrapper")
                    line(
                        currentX + nodeWidth / 2, masterY + maxTaskHeight,
                        currentX + nodeWidth / 2, masterY + maxTaskHeight + EVENT_VERTICAL_OFFSET
                    ) {
                        attr("stroke-width", "2")
                        attr("stroke", "black")
                        attr("marker-end", "url(#arrowhead)")
                        attr("stroke-dasharray", "5,5")
                    }
                }

                // Event 节点
                var eventY = masterY + maxTaskHeight + EVENT_VERTICAL_OFFSET
                for (event in task.events) {
                    val eventHeight = estimateNodeHeight(FONT_SIZE, NODE_PADDING)

                    parent.group {
                        addClass("eventWrapper")
                        translate(currentX, eventY)
                        drawTimelineNodeInner(
                            parent = this,
                            text = event,
                            width = nodeWidth,
                            height = eventHeight,
                            sectionNum = currentSectionColor,
                            cssClass = "timeline-node section-${currentSectionColor % (MAX_SECTIONS - 1)}"
                        )
                    }
                    eventY += eventHeight + EVENT_SPACING
                }
            }

            currentX += TASK_HORIZONTAL_SPACING
            if (isWithoutSections && !disableMulticolor) {
                currentSectionColor++
            }
        }
    }

    /**
     * 绘制一个 timeline 节点（在给定位置）
     */
    private fun drawTimelineNode(
        parent: SvgElement,
        text: String,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        sectionNum: Int,
        cssClass: String
    ) {
        parent.group {
            translate(x, y)
            drawTimelineNodeInner(this, text, width, height, sectionNum, cssClass)
        }
    }

    /**
     * 绘制 timeline 节点内部（背景 + 文字）
     * 在已 translate 的坐标系中绘制
     */
    private fun drawTimelineNodeInner(
        parent: SvgElement,
        text: String,
        width: Double,
        height: Double,
        sectionNum: Int,
        cssClass: String
    ) {
        val sectionIdx = sectionNum % (MAX_SECTIONS - 1)
        parent.group {
            addClass(cssClass)

            // 背景
            group {
                // 圆角矩形路径
                val d = "M0 ${SvgElement.formatNumber(height - RD)} " +
                        "v${SvgElement.formatNumber(-height + 2 * RD)} " +
                        "q0,-${SvgElement.formatNumber(RD)} ${SvgElement.formatNumber(RD)},-${SvgElement.formatNumber(RD)} " +
                        "h${SvgElement.formatNumber(width - 2 * RD)} " +
                        "q${SvgElement.formatNumber(RD)},0 ${SvgElement.formatNumber(RD)},${SvgElement.formatNumber(RD)} " +
                        "v${SvgElement.formatNumber(height - RD)} H0 Z"
                path(d) {
                    addClass("node-bkg node-section-$sectionIdx")
                }
                line(0.0, height, width, height) {
                    addClass("node-line-$sectionIdx")
                }
            }

            // 文字
            group {
                translate(width / 2, NODE_PADDING / 2.0)
                text(text, 0.0, 0.0) {
                    attr("dy", "1em")
                    attr("alignment-baseline", "middle")
                    attr("dominant-baseline", "middle")
                    attr("text-anchor", "middle")
                    attr("font-size", FONT_SIZE.toString())
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                }
            }
        }
    }

    /**
     * 估算节点高度
     */
    private fun estimateNodeHeight(fontSize: Double, padding: Int): Double {
        return fontSize * 1.1 * 0.5 + padding * 2.0 + fontSize
    }
}
