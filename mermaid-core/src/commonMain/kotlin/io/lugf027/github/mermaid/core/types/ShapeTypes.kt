package io.lugf027.github.mermaid.core.types

/**
 * 节点形状标识枚举。
 * 包含 mermaid-js 支持的所有 71+ 种形状。
 * 对应 mermaid-js rendering-util/rendering-elements/shapes/ 目录。
 */
enum class ShapeId(val displayName: String) {
    // ─── 基础形状 ─────────────────────────────────────────────────
    RECT("rectangle"),
    ROUNDED_RECT("rounded-rectangle"),
    SQUARE("square"),
    CIRCLE("circle"),
    ELLIPSE("ellipse"),
    DIAMOND("diamond"),
    HEXAGON("hexagon"),
    PARALLELOGRAM("parallelogram"),
    PARALLELOGRAM_ALT("parallelogram-alt"),
    TRAPEZOID("trapezoid"),
    TRAPEZOID_ALT("trapezoid-alt"),
    STADIUM("stadium"),
    SUBROUTINE("subroutine"),
    CYLINDER("cylinder"),
    DOUBLE_CIRCLE("double-circle"),

    // ─── 扩展形状 ─────────────────────────────────────────────────
    CLOUD("cloud"),
    BANG("bang"),
    BOLT("bolt"),
    BRACE_LEFT("brace-left"),
    BRACE_RIGHT("brace-right"),
    BRACES("braces"),
    LEAN_LEFT("lean-left"),
    LEAN_RIGHT("lean-right"),
    TRIANGLE("triangle"),
    CROSS("cross"),
    HOURGLASS("hourglass"),
    STAR("star"),
    FLAG("flag"),
    TAG_RECT("tag-rect"),
    TAG_ROUND("tag-round"),

    // ─── 标注形状 ─────────────────────────────────────────────────
    NOTE("note"),
    LABEL_RECT("label-rect"),
    COMMENT("comment"),
    TEXT_BLOCK("text-block"),
    ICON("icon"),
    ICON_CIRCLE("icon-circle"),
    ICON_SQUARE("icon-square"),
    ICON_ROUNDED("icon-rounded"),
    IMAGE("image"),

    // ─── 流程图特殊形状 ───────────────────────────────────────────
    /** 方形矩形（流程图默认形状） */
    SQUARE_RECT("square-rect"),
    /** 倒梯形 */
    INV_TRAPEZOID("inv-trapezoid"),
    /** 旗帜形/非对称矩形（>text]） */
    ODD("odd"),

    // ─── 流程图形状 ─────────────────────────────────────────────
    CARD("card"),
    LINED_CYLINDER("lined-cylinder"),
    FLIP_TRIANGLE("flip-triangle"),
    SLASH_RECT("slash-rect"),
    CURLY_BRACE_LEFT("curly-brace-left"),
    CURLY_BRACE_RIGHT("curly-brace-right"),
    LINED_WAVEFORM("lined-waveform"),
    HALF_ROUNDED_RECT("half-rounded-rect"),
    CURVED_TRAPEZOID("curved-trapezoid"),
    WINDOW_PANE("window-pane"),
    BOW_RECT("bow-rect"),
    DIVIDED_RECT("divided-rect"),
    DOCUMENT("document"),
    MULTI_RECT("multi-rect"),
    MULTI_WAVE_RECT("multi-wave-rect"),
    FILLER("filler"),

    // ─── 状态图形状 ─────────────────────────────────────────────
    STATE_START("state-start"),
    STATE_END("state-end"),
    FORK_JOIN("fork-join"),
    CHOICE("choice"),

    // ─── 类图形状 ──────────────────────────────────────────────
    CLASS_BOX("class-box"),

    // ─── ER 图形状 ──────────────────────────────────────────────
    ER_ENTITY("er-entity"),

    // ─── C4 形状 ────────────────────────────────────────────────
    C4_PERSON("c4-person"),
    C4_CONTAINER("c4-container"),
    C4_DATABASE("c4-database"),

    // ─── 架构图形状 ─────────────────────────────────────────────
    ARCH_CLOUD("arch-cloud"),
    ARCH_DATABASE("arch-database"),
    ARCH_SERVER("arch-server"),
    ARCH_DISK("arch-disk"),
    ARCH_NETWORK("arch-network"),
    ARCH_SERVICE("arch-service"),

    // ─── 其他 ──────────────────────────────────────────────────
    ANCHOR("anchor"),
    INVISIBLE("invisible");

    companion object {
        /** 从字符串查找对应的 ShapeId，找不到则返回 RECT */
        fun fromString(name: String): ShapeId {
            return entries.find {
                it.displayName.equals(name, ignoreCase = true) ||
                    it.name.equals(name, ignoreCase = true)
            } ?: RECT
        }
    }
}
