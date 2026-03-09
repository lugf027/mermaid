package io.lugf027.github.mermaid.core.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 颜色工具 - 对标 mermaid-js 中 khroma 库的核心功能
 *
 * 支持 hex/rgb/hsl 颜色解析、明暗调节、混合等操作。
 */
object ColorUtils {

    /** RGB 颜色数据 */
    data class RGB(val r: Int, val g: Int, val b: Int, val a: Double = 1.0)

    /** HSL 颜色数据 */
    data class HSL(val h: Double, val s: Double, val l: Double, val a: Double = 1.0)

    /** 常用颜色名映射 */
    private val namedColors = mapOf(
        "black" to "#000000", "white" to "#ffffff",
        "red" to "#ff0000", "green" to "#008000", "blue" to "#0000ff",
        "yellow" to "#ffff00", "cyan" to "#00ffff", "magenta" to "#ff00ff",
        "gray" to "#808080", "grey" to "#808080",
        "lightgray" to "#d3d3d3", "lightgrey" to "#d3d3d3",
        "darkgray" to "#a9a9a9", "darkgrey" to "#a9a9a9",
        "orange" to "#ffa500", "pink" to "#ffc0cb", "purple" to "#800080",
        "brown" to "#a52a2a", "navy" to "#000080", "teal" to "#008080",
        "olive" to "#808000", "maroon" to "#800000",
        "honeydew" to "#f0fff0", "cornflowerblue" to "#6495ed",
        "aqua" to "#00ffff", "lime" to "#00ff00",
        "silver" to "#c0c0c0", "gold" to "#ffd700",
        "transparent" to "#00000000",
    )

    /**
     * 解析颜色字符串为 RGB
     */
    fun parseColor(color: String): RGB {
        val trimmed = color.trim().lowercase()

        // 检查命名颜色
        namedColors[trimmed]?.let { return parseHex(it) }

        return when {
            trimmed.startsWith("#") -> parseHex(trimmed)
            trimmed.startsWith("rgb") -> parseRgb(trimmed)
            trimmed.startsWith("hsl") -> {
                val hsl = parseHsl(trimmed)
                hslToRgb(hsl)
            }
            else -> RGB(0, 0, 0)
        }
    }

    /**
     * 解析 hex 颜色
     */
    fun parseHex(hex: String): RGB {
        val h = hex.removePrefix("#")
        return when (h.length) {
            3 -> RGB(
                "${h[0]}${h[0]}".toInt(16),
                "${h[1]}${h[1]}".toInt(16),
                "${h[2]}${h[2]}".toInt(16)
            )
            4 -> RGB(
                "${h[0]}${h[0]}".toInt(16),
                "${h[1]}${h[1]}".toInt(16),
                "${h[2]}${h[2]}".toInt(16),
                "${h[3]}${h[3]}".toInt(16) / 255.0
            )
            6 -> RGB(
                h.substring(0, 2).toInt(16),
                h.substring(2, 4).toInt(16),
                h.substring(4, 6).toInt(16)
            )
            8 -> RGB(
                h.substring(0, 2).toInt(16),
                h.substring(2, 4).toInt(16),
                h.substring(4, 6).toInt(16),
                h.substring(6, 8).toInt(16) / 255.0
            )
            else -> RGB(0, 0, 0)
        }
    }

    private fun parseRgb(rgb: String): RGB {
        val match = Regex("""rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+)\s*)?\)""").find(rgb)
        return if (match != null) {
            RGB(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
                match.groupValues[4].toDoubleOrNull() ?: 1.0
            )
        } else RGB(0, 0, 0)
    }

    private fun parseHsl(hsl: String): HSL {
        val match = Regex("""hsla?\(\s*([\d.]+)\s*,\s*([\d.]+)%?\s*,\s*([\d.]+)%?\s*(?:,\s*([\d.]+)\s*)?\)""").find(hsl)
        return if (match != null) {
            HSL(
                match.groupValues[1].toDouble(),
                match.groupValues[2].toDouble() / 100.0,
                match.groupValues[3].toDouble() / 100.0,
                match.groupValues[4].toDoubleOrNull() ?: 1.0
            )
        } else HSL(0.0, 0.0, 0.0)
    }

    /** RGB → hex */
    fun rgbToHex(rgb: RGB): String {
        fun toHex(v: Int) = v.coerceIn(0, 255).toString(16).padStart(2, '0')
        return "#${toHex(rgb.r)}${toHex(rgb.g)}${toHex(rgb.b)}"
    }

    /** RGB → HSL */
    fun rgbToHsl(rgb: RGB): HSL {
        val r = rgb.r / 255.0
        val g = rgb.g / 255.0
        val b = rgb.b / 255.0
        val cMax = max(r, max(g, b))
        val cMin = min(r, min(g, b))
        val delta = cMax - cMin
        val l = (cMax + cMin) / 2.0

        if (delta == 0.0) return HSL(0.0, 0.0, l, rgb.a)

        val s = if (l < 0.5) delta / (cMax + cMin) else delta / (2.0 - cMax - cMin)
        val h = when {
            cMax == r -> ((g - b) / delta + (if (g < b) 6 else 0)) * 60.0
            cMax == g -> ((b - r) / delta + 2) * 60.0
            else -> ((r - g) / delta + 4) * 60.0
        }
        return HSL(h, s, l, rgb.a)
    }

    /** HSL → RGB */
    fun hslToRgb(hsl: HSL): RGB {
        val h = hsl.h
        val s = hsl.s
        val l = hsl.l

        if (s == 0.0) {
            val v = (l * 255).roundToInt()
            return RGB(v, v, v, hsl.a)
        }

        val q = if (l < 0.5) l * (1 + s) else l + s - l * s
        val p = 2 * l - q

        fun hue2rgb(t: Double): Double {
            val tt = when {
                t < 0 -> t + 1
                t > 1 -> t - 1
                else -> t
            }
            return when {
                tt < 1.0 / 6 -> p + (q - p) * 6 * tt
                tt < 1.0 / 2 -> q
                tt < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - tt) * 6
                else -> p
            }
        }

        return RGB(
            (hue2rgb(h / 360.0 + 1.0 / 3) * 255).roundToInt(),
            (hue2rgb(h / 360.0) * 255).roundToInt(),
            (hue2rgb(h / 360.0 - 1.0 / 3) * 255).roundToInt(),
            hsl.a
        )
    }

    /** 使颜色变暗 */
    fun darken(color: String, amount: Double): String {
        val rgb = parseColor(color)
        val hsl = rgbToHsl(rgb)
        val darkened = hsl.copy(l = max(0.0, hsl.l - amount / 100.0))
        return rgbToHex(hslToRgb(darkened))
    }

    /** 使颜色变亮 */
    fun lighten(color: String, amount: Double): String {
        val rgb = parseColor(color)
        val hsl = rgbToHsl(rgb)
        val lightened = hsl.copy(l = min(1.0, hsl.l + amount / 100.0))
        return rgbToHex(hslToRgb(lightened))
    }

    /** 调整颜色 (正值变亮，负值变暗) */
    fun adjust(color: String, amount: Double): String {
        return if (amount >= 0) lighten(color, amount) else darken(color, -amount)
    }

    /** 反转颜色 */
    fun invert(color: String): String {
        val rgb = parseColor(color)
        return rgbToHex(RGB(255 - rgb.r, 255 - rgb.g, 255 - rgb.b, rgb.a))
    }

    /** 计算亮度 (0-1) */
    fun luminance(color: String): Double {
        val rgb = parseColor(color)
        val r = rgb.r / 255.0
        val g = rgb.g / 255.0
        val b = rgb.b / 255.0

        fun linearize(c: Double) = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

        return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)
    }

    /** 根据背景色选择对比文字颜色 */
    fun contrastColor(bgColor: String): String {
        return if (luminance(bgColor) > 0.179) "#000000" else "#ffffff"
    }
}
