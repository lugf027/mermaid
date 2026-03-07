package io.lugf027.github.mermaid.core.themes

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 颜色工具函数。
 * 提供 HSL 颜色运算（adjust/invert/lighten/darken），
 * 替代 mermaid-js 使用的 khroma 库。
 */
object ColorUtils {

    /**
     * 将 CSS 颜色字符串解析为 Compose Color。
     * 支持 #RGB、#RRGGBB、#RRGGBBAA、rgb()、rgba()、命名颜色等格式。
     */
    fun parseColor(colorStr: String): Color {
        val s = colorStr.trim().lowercase()
        return when {
            s.startsWith("#") -> parseHexColor(s)
            s.startsWith("rgb") -> parseRgbColor(s)
            s.startsWith("hsl") -> parseHslColor(s)
            else -> getNamedColor(s) ?: Color.Black
        }
    }

    /**
     * 将 Color 转为 #RRGGBB 字符串。
     */
    fun toHexString(color: Color): String {
        val r = (color.red * 255).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
        return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
    }

    /**
     * 加亮颜色。
     */
    fun lighten(color: Color, amount: Float): Color {
        val hsl = rgbToHsl(color.red, color.green, color.blue)
        val newL = (hsl[2] + amount).coerceIn(0f, 1f)
        return hslToColor(hsl[0], hsl[1], newL, color.alpha)
    }

    /**
     * 加深颜色。
     */
    fun darken(color: Color, amount: Float): Color {
        val hsl = rgbToHsl(color.red, color.green, color.blue)
        val newL = (hsl[2] - amount).coerceIn(0f, 1f)
        return hslToColor(hsl[0], hsl[1], newL, color.alpha)
    }

    /**
     * 调整颜色（色调/饱和度/亮度偏移）。
     */
    fun adjust(color: Color, hueShift: Float = 0f, satShift: Float = 0f, lightShift: Float = 0f): Color {
        val hsl = rgbToHsl(color.red, color.green, color.blue)
        val newH = ((hsl[0] + hueShift) % 360f + 360f) % 360f
        val newS = (hsl[1] + satShift).coerceIn(0f, 1f)
        val newL = (hsl[2] + lightShift).coerceIn(0f, 1f)
        return hslToColor(newH, newS, newL, color.alpha)
    }

    /**
     * 反转颜色。
     */
    fun invert(color: Color): Color {
        return Color(1f - color.red, 1f - color.green, 1f - color.blue, color.alpha)
    }

    /**
     * 混合两种颜色。
     */
    fun mix(color1: Color, color2: Color, weight: Float = 0.5f): Color {
        val w = weight.coerceIn(0f, 1f)
        return Color(
            red = color1.red * (1f - w) + color2.red * w,
            green = color1.green * (1f - w) + color2.green * w,
            blue = color1.blue * (1f - w) + color2.blue * w,
            alpha = color1.alpha * (1f - w) + color2.alpha * w
        )
    }

    /**
     * 判断颜色是否为深色。
     */
    fun isDark(color: Color): Boolean {
        val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        return luminance < 0.5f
    }

    // ─── 内部工具 ──────────────────────────────────────────────

    private fun parseHexColor(hex: String): Color {
        val clean = hex.removePrefix("#")
        return when (clean.length) {
            3 -> {
                val r = clean[0].toString().repeat(2).toInt(16)
                val g = clean[1].toString().repeat(2).toInt(16)
                val b = clean[2].toString().repeat(2).toInt(16)
                Color(r, g, b)
            }
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                val a = clean.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> Color.Black
        }
    }

    private fun parseRgbColor(rgb: String): Color {
        val nums = Regex("""\d+\.?\d*""").findAll(rgb).map { it.value.toFloat() }.toList()
        return when {
            nums.size >= 4 -> Color(nums[0] / 255f, nums[1] / 255f, nums[2] / 255f, nums[3])
            nums.size >= 3 -> Color(nums[0] / 255f, nums[1] / 255f, nums[2] / 255f)
            else -> Color.Black
        }
    }

    private fun parseHslColor(hsl: String): Color {
        val nums = Regex("""\d+\.?\d*""").findAll(hsl).map { it.value.toFloat() }.toList()
        if (nums.size < 3) return Color.Black
        val alpha = if (nums.size >= 4) nums[3] else 1f
        return hslToColor(nums[0], nums[1] / 100f, nums[2] / 100f, alpha)
    }

    internal fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val l = (max + min) / 2f

        if (max == min) return floatArrayOf(0f, 0f, l)

        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f * 360f
            g -> ((b - r) / d + 2f) / 6f * 360f
            else -> ((r - g) / d + 4f) / 6f * 360f
        }
        return floatArrayOf(h, s, l)
    }

    internal fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
        if (s == 0f) return Color(l, l, l, alpha)

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        val hNorm = h / 360f
        val r = hueToRgb(p, q, hNorm + 1f / 3f)
        val g = hueToRgb(p, q, hNorm)
        val b = hueToRgb(p, q, hNorm - 1f / 3f)

        return Color(r, g, b, alpha)
    }

    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }

    private fun getNamedColor(name: String): Color? {
        return namedColors[name]
    }

    private val namedColors = mapOf(
        "black" to Color.Black,
        "white" to Color.White,
        "red" to Color.Red,
        "green" to Color.Green,
        "blue" to Color.Blue,
        "yellow" to Color.Yellow,
        "cyan" to Color.Cyan,
        "magenta" to Color.Magenta,
        "gray" to Color.Gray,
        "grey" to Color.Gray,
        "darkgray" to Color.DarkGray,
        "darkgrey" to Color.DarkGray,
        "lightgray" to Color.LightGray,
        "lightgrey" to Color.LightGray,
        "transparent" to Color.Transparent,
        "honeydew" to Color(0xFFF0FFF0),
        "lightblue" to Color(0xFFADD8E6),
        "lightyellow" to Color(0xFFFFFFE0),
        "lightgreen" to Color(0xFF90EE90),
        "pink" to Color(0xFFFFC0CB),
        "orange" to Color(0xFFFFA500),
        "purple" to Color(0xFF800080),
        "brown" to Color(0xFFA52A2A),
    )
}
