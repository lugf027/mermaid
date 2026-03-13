package io.lugf027.github.mermaid.core.diagram.sequence

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.config.SequenceDiagramConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramRenderer
import io.lugf027.github.mermaid.core.rendering.svg.*
import io.lugf027.github.mermaid.core.themes.ThemeVariables
import io.lugf027.github.mermaid.core.util.Logger
import io.lugf027.github.mermaid.core.util.TextUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 时序图渲染器 - 对标 mermaid-js sequenceRenderer.ts + svgDraw.js
 *
 * 渲染管线:
 * 1. 计算参与者尺寸和间距
 * 2. 绘制顶部参与者
 * 3. 遍历消息，按类型绘制（信号线/笔记/循环框等）
 * 4. 绘制底部镜像参与者
 * 5. 调整生命线高度
 * 6. 设置 viewBox
 */
class SequenceRenderer : DiagramRenderer {

    private val log = Logger("SequenceRenderer")

    // ── 默认配置值（对标 mermaid-js config.schema.yaml） ─
    private companion object {
        const val DEFAULT_ACTOR_MARGIN = 50.0
        const val DEFAULT_ACTOR_WIDTH = 150.0
        const val DEFAULT_ACTOR_HEIGHT = 65.0
        const val DEFAULT_DIAGRAM_MARGIN_X = 50.0
        const val DEFAULT_DIAGRAM_MARGIN_Y = 10.0
        const val DEFAULT_MESSAGE_MARGIN = 35.0
        const val DEFAULT_BOX_MARGIN = 10.0
        const val DEFAULT_BOX_TEXT_MARGIN = 5.0
        const val DEFAULT_NOTE_MARGIN = 10.0
        const val DEFAULT_ACTIVATION_WIDTH = 10.0
        const val DEFAULT_LABEL_BOX_WIDTH = 50.0
        const val DEFAULT_LABEL_BOX_HEIGHT = 20.0
        const val DEFAULT_WRAP_PADDING = 10.0
        const val DEFAULT_BOTTOM_MARGIN_ADJ = 1.0
        const val DEFAULT_ACTOR_FONT_SIZE = 14.0
        const val DEFAULT_NOTE_FONT_SIZE = 14.0
        const val DEFAULT_MESSAGE_FONT_SIZE = 16.0
    }

    override fun draw(
        db: DiagramDB,
        config: MermaidConfig,
        themeVariables: ThemeVariables,
        diagramId: String
    ): SvgRoot {
        val seqDb = db as? SequenceDb ?: throw IllegalArgumentException("Expected SequenceDb")
        val seqConfig = config.sequence ?: SequenceDiagramConfig()

        // 配置参数提取
        val actorMargin = seqConfig.actorMargin?.toDouble() ?: DEFAULT_ACTOR_MARGIN
        val actorWidth = seqConfig.width?.toDouble() ?: DEFAULT_ACTOR_WIDTH
        val actorHeight = seqConfig.height?.toDouble() ?: DEFAULT_ACTOR_HEIGHT
        val diagramMarginX = DEFAULT_DIAGRAM_MARGIN_X
        val diagramMarginY = DEFAULT_DIAGRAM_MARGIN_Y
        val messageMargin = seqConfig.messageMargin?.toDouble() ?: DEFAULT_MESSAGE_MARGIN
        val noteMargin = DEFAULT_NOTE_MARGIN
        val activationWidth = DEFAULT_ACTIVATION_WIDTH
        val labelBoxWidth = seqConfig.labelBoxWidth?.toDouble() ?: DEFAULT_LABEL_BOX_WIDTH
        val labelBoxHeight = seqConfig.labelBoxHeight?.toDouble() ?: DEFAULT_LABEL_BOX_HEIGHT
        val mirrorActors = seqConfig.mirrorActors ?: true
        val showSequenceNumbers = seqConfig.showSequenceNumbers ?: false
        val messageFontSize = seqConfig.messageFontSize?.toDouble() ?: DEFAULT_MESSAGE_FONT_SIZE
        val actorFontSize = seqConfig.actorFontSize?.toDouble() ?: DEFAULT_ACTOR_FONT_SIZE
        val noteFontSize = seqConfig.noteFontSize?.toDouble() ?: DEFAULT_NOTE_FONT_SIZE

        // 获取数据
        val actorKeys = seqDb.getActorKeys()
        val actors = seqDb.getActors()
        val messages = seqDb.getMessages()

        // ── 阶段 1: 计算参与者尺寸和位置 ─────────────
        // 对标 mermaid-js: actor 从 x=0 开始，viewBox 使用 -diagramMarginX 偏移
        val actorWidths = mutableMapOf<String, Double>()
        val actorPositions = mutableMapOf<String, Double>()  // 存储 actor 中心 x 坐标
        var currentX = 0.0

        // 计算每个参与者文本的宽度（JS 使用 calculateTextDimensions 返回 getBBox）
        for (key in actorKeys) {
            val actor = actors[key]!!
            val textWidth = TextUtils.estimateTextWidth(actor.description, actorFontSize)
            val width = max(actorWidth, textWidth + 20)
            actorWidths[key] = width
        }

        // 计算消息跨度所需的额外间距
        val actorSpacings = calculateActorSpacings(
            actorKeys, actors, messages, actorWidths, actorMargin, messageFontSize
        )

        // 定位参与者：第一个 actor 的中心在 width/2，后续加上间距
        for ((idx, key) in actorKeys.withIndex()) {
            val width = actorWidths[key] ?: actorWidth
            if (idx == 0) {
                // 第一个 actor：rect 从 x=0 开始，中心在 width/2
                actorPositions[key] = width / 2
                currentX = width
            } else {
                val spacing = actorSpacings[key] ?: actorMargin
                currentX += spacing
                actorPositions[key] = currentX + width / 2
                currentX += width
            }
        }

        val contentWidth = currentX  // 不含边距的内容宽度
        val totalWidth = contentWidth + diagramMarginX * 2  // 含边距的总宽度
        var verticalPos = actorHeight  // 消息起始 y，在顶部 actor 下方
        val actorsTopY = 0.0  // 顶部 actor 的 y 坐标

        // ── 阶段 2: 构建 SVG ────────────────────────
        return buildSvg {
            attr("id", diagramId)
            addClass("sequenceDiagram")
            attr("xmlns", "http://www.w3.org/2000/svg")
            attr("xmlns:xlink", "http://www.w3.org/1999/xlink")

            // 无障碍信息
            val titleText = seqDb.getDiagramTitle()
            if (titleText.isNotEmpty()) {
                title(titleText)
            }
            val accTitleText = seqDb.getAccTitle()
            if (accTitleText.isNotEmpty()) {
                attr("aria-roledescription", "sequence")
                title(accTitleText)
            }

            // ── defs: 样式 + 箭头标记 ──
            defs {
                style(generateSequenceStyles(themeVariables))

                // 箭头标记
                marker {
                    attr("id", "arrowhead")
                    attr("refX", "7.9")
                    attr("refY", "5")
                    attr("markerWidth", "12")
                    attr("markerHeight", "12")
                    attr("orient", "auto")
                    path("M 0 0 L 10 5 L 0 10 z")
                }

                marker {
                    attr("id", "crosshead")
                    attr("refX", "15.5")
                    attr("refY", "8")
                    attr("markerWidth", "20")
                    attr("markerHeight", "20")
                    attr("orient", "auto")
                    path("M 1,2 L 6,7 M 6,2 L 1,7") {
                        attr("style", "stroke-width: 1; stroke-dasharray: 0, 0;")
                    }
                    path("M 9,2 L 15,8 L 9,14 z") {
                        attr("style", "stroke-width: 1;")
                    }
                }

                marker {
                    attr("id", "filled-head")
                    attr("refX", "15.5")
                    attr("refY", "7")
                    attr("markerWidth", "20")
                    attr("markerHeight", "28")
                    attr("orient", "auto")
                    path("M 18,7 L9,13 L14,7 L9,1 Z")
                }

                if (showSequenceNumbers || seqDb.isSequenceNumbersEnabled()) {
                    marker {
                        attr("id", "sequencenumber")
                        attr("refX", "15")
                        attr("refY", "15")
                        attr("markerWidth", "60")
                        attr("markerHeight", "40")
                        attr("orient", "auto")
                        circle(15.0, 15.0, 6.0) {
                            addClass("actor")
                        }
                    }
                }
            }

            // ── 绘制顶部参与者 ──
            val actorsGroup = group {
                addClass("actors")
            }

            for (key in actorKeys) {
                val actor = actors[key]!!
                val cx = actorPositions[key] ?: 0.0
                val width = actorWidths[key] ?: actorWidth

                drawActor(actorsGroup, actor, key, cx, actorsTopY, width, actorHeight,
                    actorFontSize, themeVariables, isFooter = false)
            }

            // ── 绘制消息和循环框 ──
            var seqNum = if (seqDb.isSequenceNumbersEnabled()) seqDb.getAutoNumberStart() else 1
            val seqStep = seqDb.getAutoNumberStep()
            val loopStack = mutableListOf<LoopModel>()
            val activationStack = mutableListOf<ActivationInfo>()

            val messagesGroup = group {
                addClass("messages")
            }

            for (msg in messages) {
                when (msg.type) {
                    LineType.NOTE -> {
                        // 绘制笔记
                        verticalPos += messageMargin / 2
                        val noteActor = msg.from ?: continue
                        val noteActor2 = msg.to?.takeIf { it != noteActor }
                        val placement = msg.placement ?: Placement.RIGHTOF

                        val noteWidth = max(100.0, TextUtils.estimateTextWidth(msg.message, noteFontSize) + 20)
                        val noteHeight = max(30.0, 20.0 + 16.0) // simplified

                        val noteX = when (placement) {
                            Placement.LEFTOF -> (actorPositions[noteActor] ?: 0.0) - noteWidth - noteMargin
                            Placement.OVER -> {
                                if (noteActor2 != null) {
                                    val x1 = actorPositions[noteActor] ?: 0.0
                                    val x2 = actorPositions[noteActor2] ?: 0.0
                                    (min(x1, x2) + max(x1, x2)) / 2 - noteWidth / 2
                                } else {
                                    (actorPositions[noteActor] ?: 0.0) - noteWidth / 2
                                }
                            }
                            else -> (actorPositions[noteActor] ?: 0.0) + noteMargin
                        }

                        drawNote(messagesGroup, noteX, verticalPos, noteWidth, noteHeight,
                            msg.message, noteFontSize, themeVariables)

                        verticalPos += noteHeight + messageMargin / 2
                    }

                    LineType.ACTIVE_START -> {
                        val actorId = msg.message
                        val cx = actorPositions[actorId] ?: 0.0
                        activationStack.add(ActivationInfo(
                            actorId = actorId,
                            startY = verticalPos,
                            x = cx - activationWidth / 2,
                            stackDepth = activationStack.count { it.actorId == actorId }
                        ))
                    }

                    LineType.ACTIVE_END -> {
                        val actorId = msg.message
                        val activation = activationStack.lastOrNull { it.actorId == actorId }
                        if (activation != null) {
                            drawActivation(messagesGroup, activation, verticalPos,
                                activationWidth, themeVariables)
                            activationStack.remove(activation)
                        }
                    }

                    LineType.LOOP_START, LineType.ALT_START, LineType.OPT_START,
                    LineType.PAR_START, LineType.CRITICAL_START, LineType.BREAK_START,
                    LineType.RECT_START, LineType.PAR_OVER_START -> {
                        val label = when (msg.type) {
                            LineType.LOOP_START -> "loop"
                            LineType.ALT_START -> "alt"
                            LineType.OPT_START -> "opt"
                            LineType.PAR_START -> "par"
                            LineType.CRITICAL_START -> "critical"
                            LineType.BREAK_START -> "break"
                            LineType.RECT_START -> "rect"
                            else -> "loop"
                        }
                        loopStack.add(LoopModel(
                            label = label,
                            title = msg.message,
                            startY = verticalPos,
                            sections = mutableListOf()
                        ))
                        verticalPos += labelBoxHeight + DEFAULT_BOX_TEXT_MARGIN
                    }

                    LineType.ALT_ELSE, LineType.PAR_AND, LineType.CRITICAL_OPTION -> {
                        val loop = loopStack.lastOrNull()
                        loop?.sections?.add(LoopSection(
                            message = msg.message,
                            y = verticalPos
                        ))
                        verticalPos += labelBoxHeight / 2
                    }

                    LineType.LOOP_END, LineType.ALT_END, LineType.OPT_END,
                    LineType.PAR_END, LineType.CRITICAL_END, LineType.BREAK_END,
                    LineType.RECT_END -> {
                        val loop = loopStack.removeLastOrNull()
                        if (loop != null) {
                            drawLoop(messagesGroup, loop, 0.0,
                                contentWidth, verticalPos,
                                labelBoxWidth, labelBoxHeight, themeVariables)
                        }
                    }

                    LineType.AUTONUMBER -> {
                        // handled by flag
                    }

                    else -> {
                        // 普通消息
                        if (!LineType.isMessage(msg.type)) continue

                        // 对标 JS: 消息间距 ≈ messageMargin + messageFontSize / 2
                        verticalPos += messageMargin + messageFontSize / 2

                        val from = msg.from ?: continue
                        val to = msg.to ?: continue
                        val fromX = actorPositions[from] ?: continue
                        val toX = actorPositions[to] ?: continue

                        // JS 中消息线端点有微偏移（考虑线宽和 actor 框边界）
                        val lineOffset = 1.0
                        val adjustedFromX = if (fromX < toX) fromX + lineOffset else fromX - lineOffset
                        val adjustedToX = if (toX < fromX) toX + lineOffset else toX - lineOffset

                        val isSelfMessage = from == to

                        if (isSelfMessage) {
                            // 自引用消息：绘制 U 型路径
                            val selfWidth = 40.0
                            val selfHeight = 20.0
                            drawSelfMessage(messagesGroup, fromX, verticalPos, selfWidth, selfHeight,
                                msg, messageFontSize, themeVariables,
                                if (seqDb.isSequenceNumbersEnabled()) seqNum else null)
                            verticalPos += selfHeight + messageMargin / 2
                        } else {
                            // 普通消息：绘制直线
                            drawMessage(messagesGroup, adjustedFromX, adjustedToX, verticalPos,
                                msg, messageFontSize, themeVariables,
                                if (seqDb.isSequenceNumbersEnabled()) seqNum else null)
                        }

                        if (seqDb.isSequenceNumbersEnabled()) {
                            seqNum += seqStep
                        }

                        // 激活处理
                        if (msg.activate) {
                            val cx = actorPositions[to] ?: 0.0
                            activationStack.add(ActivationInfo(
                                actorId = to,
                                startY = verticalPos,
                                x = cx - activationWidth / 2,
                                stackDepth = activationStack.count { it.actorId == to }
                            ))
                        }
                    }
                }
            }

            // 结束未闭合的激活
            for (act in activationStack) {
                drawActivation(messagesGroup, act, verticalPos, activationWidth, themeVariables)
            }

            verticalPos += messageMargin

            // ── 绘制底部镜像参与者 ──
            if (mirrorActors) {
                val footerGroup = group {
                    addClass("actors")
                }
                for (key in actorKeys) {
                    val actor = actors[key]!!
                    val cx = actorPositions[key] ?: 0.0
                    val width = actorWidths[key] ?: actorWidth

                    drawActor(footerGroup, actor, key, cx, verticalPos, width, actorHeight,
                        actorFontSize, themeVariables, isFooter = true)
                }
                verticalPos += actorHeight
            }

            // viewBox 的高度包含所有内容

            // ── 设置 viewBox ──
            // 对标 mermaid-js: viewBox 使用负偏移提供边距
            val finalWidth = contentWidth + diagramMarginX * 2
            val finalHeight = verticalPos + diagramMarginY
            viewBox(-diagramMarginX, -diagramMarginY, finalWidth, finalHeight)
            attr("width", "100%")
            attr("style", "max-width: ${finalWidth.toInt()}px;")
            attr("height", "${finalHeight}")
        }
    }

    // ════════════════════════════════════════════════════
    //  绘制方法
    // ════════════════════════════════════════════════════

    private fun drawActor(
        parent: SvgElement,
        actor: Actor,
        actorId: String,
        centerX: Double,
        y: Double,
        width: Double,
        height: Double,
        fontSize: Double,
        tv: ThemeVariables,
        isFooter: Boolean
    ) {
        val positionClass = if (isFooter) "actor-bottom" else "actor-top"

        parent.group {
            // 对标 JS: 生命线在 actor group 外部或先绘制
            if (!isFooter) {
                // 生命线 - 从 actor 底部延伸到底部 actor 顶部
                line(centerX, y + height, centerX, 2000.0) {
                    addClass("actor-line")
                    attr("stroke-width", "0.5px")
                    attr("stroke", tv.actorLineColor)
                    attr("id", "actor$actorId")
                    attr("name", actorId)
                }
            }

            group {
                when (actor.type) {
                    ParticipantType.ACTOR -> {
                        // 火柴人
                        val headR = 14.0
                        val headY = y + height / 2 - 18
                        circle(centerX, headY, headR) {
                            addClass("actor-man")
                            attr("fill", tv.actorBkg)
                            attr("stroke", tv.actorBorder)
                        }
                        // 身体
                        line(centerX, headY + headR, centerX, headY + headR + 20) {
                            addClass("actor-man")
                            attr("stroke", tv.actorBorder)
                        }
                        // 手臂
                        line(centerX - 15, headY + headR + 8, centerX + 15, headY + headR + 8) {
                            addClass("actor-man")
                            attr("stroke", tv.actorBorder)
                        }
                        // 腿
                        line(centerX, headY + headR + 20, centerX - 12, y + height - 4) {
                            addClass("actor-man")
                            attr("stroke", tv.actorBorder)
                        }
                        line(centerX, headY + headR + 20, centerX + 12, y + height - 4) {
                            addClass("actor-man")
                            attr("stroke", tv.actorBorder)
                        }
                    }

                    ParticipantType.DATABASE -> {
                        // 圆柱体
                        val dbW = width * 0.7
                        val dbH = height - 10
                        val ellipseRY = 8.0
                        rect(centerX - dbW / 2, y + 5, dbW, dbH) {
                            attr("fill", tv.actorBkg)
                            attr("stroke", tv.actorBorder)
                            attr("rx", "0")
                        }
                        ellipse(centerX, y + 5 + ellipseRY, dbW / 2, ellipseRY) {
                            attr("fill", tv.actorBkg)
                            attr("stroke", tv.actorBorder)
                        }
                    }

                    else -> {
                        // 默认: 矩形框 - 对标 JS: rect 使用 absolute 坐标
                        rect(centerX - width / 2, y, width, height) {
                            addClass("actor $positionClass")
                            attr("fill", tv.actorBkg)
                            attr("stroke", tv.actorBorder)
                            attr("rx", "3")
                            attr("ry", "3")
                            attr("name", actorId)
                        }
                    }
                }

                // 名称文本 - 对标 JS: y = actorY + actorHeight/2, dominant-baseline=central
                text(actor.description, centerX, y + height / 2) {
                    addClass("actor")
                    attr("text-anchor", "middle")
                    attr("dominant-baseline", "central")
                    attr("alignment-baseline", "central")
                    attr("font-size", "${fontSize}px")
                    attr("font-family", "'Open Sans', sans-serif")
                    attr("fill", tv.actorTextColor)
                }
            }
        }
    }

    private fun drawMessage(
        parent: SvgElement,
        fromX: Double,
        toX: Double,
        y: Double,
        msg: Message,
        fontSize: Double,
        tv: ThemeVariables,
        seqNum: Int?
    ) {
        parent.group {
            addClass("messageLine")

            val isDotted = LineType.isDotted(msg.type)
            val lineClass = if (isDotted) "messageLine1" else "messageLine0"

            // 消息线
            line(fromX, y, toX, y) {
                addClass(lineClass)
                attr("stroke", tv.signalColor)
                attr("stroke-width", "1.5")
                if (isDotted) {
                    attr("stroke-dasharray", "3, 3")
                }

                // 箭头标记
                when (msg.type) {
                    LineType.SOLID, LineType.DOTTED ->
                        attr("marker-end", "url(#arrowhead)")
                    LineType.SOLID_CROSS, LineType.DOTTED_CROSS ->
                        attr("marker-end", "url(#crosshead)")
                    LineType.SOLID_POINT, LineType.DOTTED_POINT ->
                        attr("marker-end", "url(#filled-head)")
                    LineType.BIDIRECTIONAL_SOLID, LineType.BIDIRECTIONAL_DOTTED -> {
                        attr("marker-end", "url(#arrowhead)")
                        attr("marker-start", "url(#arrowhead)")
                    }
                }
            }

            // 消息文本 - 对标 JS: text y = lineY - 29, dy="1em"
            if (msg.message.isNotEmpty()) {
                val textX = (fromX + toX) / 2
                val textY = y - fontSize - 13  // JS: y - 29 relative to line
                text(msg.message, textX, textY) {
                    addClass("messageText")
                    attr("text-anchor", "middle")
                    attr("dominant-baseline", "middle")
                    attr("alignment-baseline", "middle")
                    attr("dy", "1em")
                    attr("font-size", "${fontSize}px")
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                    attr("fill", tv.signalTextColor)
                }
            }

            // 序列号
            if (seqNum != null) {
                val numX = if (fromX < toX) fromX else toX
                circle(numX, y, 8.0) {
                    addClass("sequenceNumber")
                    attr("fill", tv.sequenceNumberColor)
                }
                text("$seqNum", numX, y + 3) {
                    addClass("sequenceNumber")
                    attr("text-anchor", "middle")
                    attr("font-size", "11px")
                    attr("fill", "white")
                }
            }
        }
    }

    private fun drawSelfMessage(
        parent: SvgElement,
        x: Double,
        y: Double,
        selfWidth: Double,
        selfHeight: Double,
        msg: Message,
        fontSize: Double,
        tv: ThemeVariables,
        seqNum: Int?
    ) {
        parent.group {
            addClass("messageLine")

            val isDotted = LineType.isDotted(msg.type)
            val lineClass = if (isDotted) "messageLine1" else "messageLine0"

            // U 型自引用路径
            val pathD = "M ${"%.2f".format(x)},${"%.2f".format(y)} " +
                "L ${"%.2f".format(x + selfWidth)},${"%.2f".format(y)} " +
                "L ${"%.2f".format(x + selfWidth)},${"%.2f".format(y + selfHeight)} " +
                "L ${"%.2f".format(x)},${"%.2f".format(y + selfHeight)}"

            path(pathD) {
                addClass(lineClass)
                attr("stroke", tv.signalColor)
                attr("stroke-width", "1.5")
                attr("fill", "none")
                if (isDotted) {
                    attr("stroke-dasharray", "3, 3")
                }
                attr("marker-end", "url(#arrowhead)")
            }

            // 消息文本
            if (msg.message.isNotEmpty()) {
                text(msg.message, x + selfWidth + 5, y + selfHeight / 2) {
                    addClass("messageText")
                    attr("font-size", "${fontSize}px")
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                    attr("fill", tv.signalTextColor)
                }
            }
        }
    }

    private fun drawNote(
        parent: SvgElement,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        message: String,
        fontSize: Double,
        tv: ThemeVariables
    ) {
        parent.group {
            addClass("note")

            // 笔记矩形
            rect(x, y, width, height) {
                addClass("note")
                attr("fill", tv.noteBkgColor)
                attr("stroke", tv.noteBorderColor)
                attr("rx", "0")
                attr("ry", "0")
            }

            // 折角效果
            val foldSize = 7.0
            path("M ${x + width - foldSize},$y L ${x + width},${"%.2f".format(y + foldSize)}") {
                attr("stroke", tv.noteBorderColor)
                attr("fill", "none")
            }

            // 笔记文本
            text(message, x + width / 2, y + height / 2 + fontSize / 3) {
                addClass("noteText")
                attr("text-anchor", "middle")
                attr("font-size", "${fontSize}px")
                attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                attr("fill", tv.noteTextColor)
            }
        }
    }

    private fun drawActivation(
        parent: SvgElement,
        activation: ActivationInfo,
        endY: Double,
        activationWidth: Double,
        tv: ThemeVariables
    ) {
        val classIdx = activation.stackDepth % 3
        parent.rect(activation.x, activation.startY, activationWidth, endY - activation.startY) {
            addClass("activation$classIdx")
            attr("fill", tv.activationBkgColor)
            attr("stroke", tv.activationBorderColor)
        }
    }

    private fun drawLoop(
        parent: SvgElement,
        loop: LoopModel,
        startX: Double,
        stopX: Double,
        endY: Double,
        labelBoxWidth: Double,
        labelBoxHeight: Double,
        tv: ThemeVariables
    ) {
        parent.group {
            addClass("loop")

            // 外框
            rect(startX, loop.startY, stopX - startX, endY - loop.startY) {
                addClass("loopLine")
                attr("fill", "none")
                attr("stroke", tv.loopTextColor)
                attr("stroke-dasharray", "2, 2")
            }

            // 标签背景（带切角）
            val labelY = loop.startY
            val cutSize = 5.0
            val labelPath = buildString {
                append("M ${startX},${labelY}")
                append(" L ${startX + labelBoxWidth},$labelY")
                append(" L ${startX + labelBoxWidth},${labelY + labelBoxHeight - cutSize}")
                append(" L ${startX + labelBoxWidth - cutSize},${labelY + labelBoxHeight}")
                append(" L $startX,${labelY + labelBoxHeight}")
                append(" Z")
            }
            path(labelPath) {
                addClass("labelBox")
                attr("fill", tv.labelBoxBkgColor)
                attr("stroke", tv.loopTextColor)
            }

            // 标签文本
            text(loop.label, startX + labelBoxWidth / 2, labelY + labelBoxHeight / 2 + 4) {
                addClass("labelText")
                attr("text-anchor", "middle")
                attr("font-size", "13px")
                attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                attr("fill", tv.loopTextColor)
            }

            // 标题文本
            if (loop.title.isNotEmpty()) {
                text(loop.title, startX + labelBoxWidth + 10, labelY + labelBoxHeight / 2 + 4) {
                    addClass("loopText")
                    attr("font-size", "13px")
                    attr("font-family", "'trebuchet ms', verdana, arial, sans-serif")
                    attr("fill", tv.loopTextColor)
                }
            }

            // section 分隔虚线
            for (section in loop.sections) {
                line(startX, section.y, stopX, section.y) {
                    addClass("loopLine")
                    attr("stroke", tv.loopTextColor)
                    attr("stroke-dasharray", "2, 2")
                }
                if (section.message.isNotEmpty()) {
                    text(section.message, startX + 10, section.y + 15) {
                        addClass("loopText")
                        attr("font-size", "13px")
                        attr("fill", tv.loopTextColor)
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  布局计算
    // ════════════════════════════════════════════════════

    private fun calculateActorSpacings(
        actorKeys: List<String>,
        actors: Map<String, Actor>,
        messages: List<Message>,
        actorWidths: Map<String, Double>,
        defaultMargin: Double,
        messageFontSize: Double
    ): Map<String, Double> {
        val spacings = mutableMapOf<String, Double>()

        // 基础间距
        for (key in actorKeys) {
            spacings[key] = defaultMargin
        }

        // 根据消息宽度调整间距
        for (msg in messages) {
            if (!LineType.isMessage(msg.type)) continue
            val from = msg.from ?: continue
            val to = msg.to ?: continue
            if (from == to) continue  // 自引用不影响间距

            val fromIdx = actorKeys.indexOf(from)
            val toIdx = actorKeys.indexOf(to)
            if (fromIdx < 0 || toIdx < 0) continue

            val msgWidth = TextUtils.estimateTextWidth(msg.message, messageFontSize) + 20
            val actorsBetween = abs(toIdx - fromIdx)
            val widthPerGap = msgWidth / actorsBetween

            // 确保每个间隔足够宽
            val minIdx = min(fromIdx, toIdx)
            val maxIdx = max(fromIdx, toIdx)
            for (idx in (minIdx + 1)..maxIdx) {
                val key = actorKeys[idx]
                val currentWidth = (actorWidths[actorKeys[idx - 1]] ?: 0.0) / 2 +
                    (actorWidths[key] ?: 0.0) / 2
                val neededSpacing = max(defaultMargin, widthPerGap - currentWidth)
                spacings[key] = max(spacings[key] ?: defaultMargin, neededSpacing)
            }
        }

        return spacings
    }

    // ════════════════════════════════════════════════════
    //  样式生成
    // ════════════════════════════════════════════════════

    private fun generateSequenceStyles(tv: ThemeVariables): String = """
.actor {
  stroke: ${tv.actorBorder};
  fill: ${tv.actorBkg};
}
text.actor > tspan {
  fill: ${tv.actorTextColor};
  stroke: none;
}
.actor-line {
  stroke: ${tv.actorLineColor};
}
.messageLine0 {
  stroke-width: 1.5;
  stroke-dasharray: none;
  stroke: ${tv.signalColor};
}
.messageLine1 {
  stroke-width: 1.5;
  stroke-dasharray: 2, 2;
  stroke: ${tv.signalColor};
}
#arrowhead path {
  fill: ${tv.signalColor};
  stroke: ${tv.signalColor};
}
.sequenceNumber {
  fill: ${tv.sequenceNumberColor};
}
#crosshead path {
  fill: ${tv.signalColor};
  stroke: ${tv.signalColor};
}
.messageText {
  fill: ${tv.signalTextColor};
  stroke: none;
}
.labelBox {
  stroke: ${tv.labelBoxBorderColor};
  fill: ${tv.labelBoxBkgColor};
}
.labelText, .labelText > tspan {
  fill: ${tv.loopTextColor};
  stroke: none;
}
.loopText, .loopText > tspan {
  fill: ${tv.loopTextColor};
  stroke: none;
}
.loopLine {
  stroke-width: 2px;
  stroke-dasharray: 2, 2;
  stroke: ${tv.labelBoxBorderColor};
  fill: ${tv.labelBoxBkgColor};
}
.note {
  stroke: ${tv.noteBorderColor};
  fill: ${tv.noteBkgColor};
}
.noteText, .noteText > tspan {
  fill: ${tv.noteTextColor};
  stroke: none;
}
.activation0 {
  fill: ${tv.activationBkgColor};
  stroke: ${tv.activationBorderColor};
}
.activation1 {
  fill: ${tv.activationBkgColor};
  stroke: ${tv.activationBorderColor};
}
.activation2 {
  fill: ${tv.activationBkgColor};
  stroke: ${tv.activationBorderColor};
}
.actor-man line, .actor-man circle {
  stroke: ${tv.actorBorder};
  fill: ${tv.actorBkg};
  stroke-width: 2px;
}
""".trimIndent()
}

// ════════════════════════════════════════════════════════
//  内部数据模型
// ════════════════════════════════════════════════════════

private data class LoopModel(
    val label: String,
    val title: String,
    val startY: Double,
    val sections: MutableList<LoopSection> = mutableListOf()
)

private data class LoopSection(
    val message: String,
    val y: Double
)

private data class ActivationInfo(
    val actorId: String,
    val startY: Double,
    val x: Double,
    val stackDepth: Int = 0
)
