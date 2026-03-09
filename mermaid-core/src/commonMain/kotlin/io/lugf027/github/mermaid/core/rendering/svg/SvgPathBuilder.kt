package io.lugf027.github.mermaid.core.rendering.svg

import kotlin.math.*

/**
 * SVG 路径数据构建器 - 精确对标 d3.line / d3.arc 的输出格式
 *
 * 生成 <path> 元素的 d 属性字符串。
 */
class SvgPathBuilder {
    private val commands = StringBuilder()

    /** 移动到指定点 M x,y */
    fun moveTo(x: Double, y: Double): SvgPathBuilder {
        commands.append("M${fmt(x)},${fmt(y)}")
        return this
    }

    /** 画直线到指定点 L x,y */
    fun lineTo(x: Double, y: Double): SvgPathBuilder {
        commands.append("L${fmt(x)},${fmt(y)}")
        return this
    }

    /** 水平线 H x */
    fun horizontalTo(x: Double): SvgPathBuilder {
        commands.append("H${fmt(x)}")
        return this
    }

    /** 垂直线 V y */
    fun verticalTo(y: Double): SvgPathBuilder {
        commands.append("V${fmt(y)}")
        return this
    }

    /** 三次贝塞尔曲线 C x1,y1 x2,y2 x,y */
    fun cubicTo(
        x1: Double, y1: Double,
        x2: Double, y2: Double,
        x: Double, y: Double
    ): SvgPathBuilder {
        commands.append("C${fmt(x1)},${fmt(y1)},${fmt(x2)},${fmt(y2)},${fmt(x)},${fmt(y)}")
        return this
    }

    /** 二次贝塞尔曲线 Q x1,y1 x,y */
    fun quadTo(
        x1: Double, y1: Double,
        x: Double, y: Double
    ): SvgPathBuilder {
        commands.append("Q${fmt(x1)},${fmt(y1)},${fmt(x)},${fmt(y)}")
        return this
    }

    /** 弧形 A rx,ry xRotation largeArc sweep x,y */
    fun arcTo(
        rx: Double, ry: Double,
        xRotation: Double = 0.0,
        largeArc: Boolean = false,
        sweep: Boolean = true,
        x: Double, y: Double
    ): SvgPathBuilder {
        val la = if (largeArc) 1 else 0
        val sw = if (sweep) 1 else 0
        commands.append("A${fmt(rx)},${fmt(ry)},${fmt(xRotation)},$la,$sw,${fmt(x)},${fmt(y)}")
        return this
    }

    /** 闭合路径 Z */
    fun closePath(): SvgPathBuilder {
        commands.append("Z")
        return this
    }

    /** 构建路径字符串 */
    fun build(): String = commands.toString()

    override fun toString(): String = build()

    companion object {
        /** 格式化数值 */
        private fun fmt(value: Double): String {
            return if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                // KMP 兼容：手动控制精度
                val rounded = kotlin.math.round(value * 10000) / 10000.0
                val str = rounded.toString()
                if (str.contains('.')) {
                    str.trimEnd('0').trimEnd('.')
                } else {
                    str
                }
            }
        }

        /**
         * 生成 d3.arc() 等效的弧形路径 - 用于饼图等
         *
         * @param innerRadius 内半径（0 = 实心饼）
         * @param outerRadius 外半径
         * @param startAngle 起始角度（弧度，12 点方向为 0）
         * @param endAngle 结束角度（弧度）
         * @param cornerRadius 圆角半径
         * @return SVG 路径字符串
         */
        fun arc(
            innerRadius: Double = 0.0,
            outerRadius: Double,
            startAngle: Double,
            endAngle: Double,
            cornerRadius: Double = 0.0
        ): String {
            val builder = SvgPathBuilder()
            val fullCircle = abs(endAngle - startAngle) >= 2 * PI - 1e-6

            // d3 约定：12 点方向为 0，顺时针
            // 转换为 SVG 坐标：3 点方向为 0
            val sa = startAngle - PI / 2
            val ea = endAngle - PI / 2

            val sx = outerRadius * cos(sa)
            val sy = outerRadius * sin(sa)
            val ex = outerRadius * cos(ea)
            val ey = outerRadius * sin(ea)

            val largeArc = abs(endAngle - startAngle) > PI

            if (fullCircle) {
                // 完整圆需要分成两个弧
                val mx = outerRadius * cos(sa + PI)
                val my = outerRadius * sin(sa + PI)
                builder.moveTo(sx, sy)
                builder.arcTo(outerRadius, outerRadius, 0.0, true, true, mx, my)
                builder.arcTo(outerRadius, outerRadius, 0.0, true, true, sx, sy)

                if (innerRadius > 0) {
                    val isx = innerRadius * cos(sa)
                    val isy = innerRadius * sin(sa)
                    val imx = innerRadius * cos(sa + PI)
                    val imy = innerRadius * sin(sa + PI)
                    builder.moveTo(isx, isy)
                    builder.arcTo(innerRadius, innerRadius, 0.0, true, false, imx, imy)
                    builder.arcTo(innerRadius, innerRadius, 0.0, true, false, isx, isy)
                }
            } else if (innerRadius > 0) {
                // 环形扇区
                val isx = innerRadius * cos(sa)
                val isy = innerRadius * sin(sa)
                val iex = innerRadius * cos(ea)
                val iey = innerRadius * sin(ea)

                builder.moveTo(sx, sy)
                builder.arcTo(outerRadius, outerRadius, 0.0, largeArc, true, ex, ey)
                builder.lineTo(iex, iey)
                builder.arcTo(innerRadius, innerRadius, 0.0, largeArc, false, isx, isy)
                builder.closePath()
            } else {
                // 实心扇区（饼图切片）
                builder.moveTo(sx, sy)
                builder.arcTo(outerRadius, outerRadius, 0.0, largeArc, true, ex, ey)
                builder.lineTo(0.0, 0.0)
                builder.closePath()
            }

            return builder.build()
        }

        /**
         * 生成圆角矩形路径
         */
        fun roundedRect(
            x: Double, y: Double, width: Double, height: Double, rx: Double, ry: Double = rx
        ): String {
            val builder = SvgPathBuilder()
            builder.moveTo(x + rx, y)
            builder.lineTo(x + width - rx, y)
            builder.arcTo(rx, ry, 0.0, false, true, x + width, y + ry)
            builder.lineTo(x + width, y + height - ry)
            builder.arcTo(rx, ry, 0.0, false, true, x + width - rx, y + height)
            builder.lineTo(x + rx, y + height)
            builder.arcTo(rx, ry, 0.0, false, true, x, y + height - ry)
            builder.lineTo(x, y + ry)
            builder.arcTo(rx, ry, 0.0, false, true, x + rx, y)
            builder.closePath()
            return builder.build()
        }
    }
}
