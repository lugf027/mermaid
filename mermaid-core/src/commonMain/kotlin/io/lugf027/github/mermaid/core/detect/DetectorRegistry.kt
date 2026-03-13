package io.lugf027.github.mermaid.core.detect

import io.lugf027.github.mermaid.core.config.ConfigManager

/**
 * 图表类型检测器注册表 - 对标 mermaid-js detectType.ts
 *
 * 存储所有图表的 (id, detector) 映射，按优先级排序。
 * 注册顺序即优先级（先注册的先匹配）。
 */
object DetectorRegistry {

    /** 检测器条目 */
    data class DetectorEntry(
        val id: String,
        val detector: (String) -> Boolean
    )

    private val detectors = mutableListOf<DetectorEntry>()

    /**
     * 注册检测器
     */
    fun register(id: String, detector: (String) -> Boolean) {
        detectors.add(DetectorEntry(id, detector))
    }

    /**
     * 清空所有检测器
     */
    fun clear() {
        detectors.clear()
    }

    /**
     * 获取所有已注册的检测器
     */
    fun getAll(): List<DetectorEntry> = detectors.toList()

    /**
     * 注册所有内置图表检测器 - 对标 diagram-orchestration.ts
     *
     * 注册顺序决定匹配优先级。条件检测器（依赖配置）使用简化逻辑。
     */
    fun registerBuiltinDetectors() {
        clear()

        val config = ConfigManager.getConfig()

        // flowchart-elk (最优先，检测 flowchart-elk 关键字或 elk renderer 配置)
        register("flowchart-elk") { text ->
            Regex("^\\s*flowchart-elk").containsMatchIn(text)
        }

        // flowchart-v2 (现代流程图，默认渲染器)
        register("flowchart-v2") { text ->
            val renderer = config.flowchart?.defaultRenderer ?: "dagre-wrapper"
            if (renderer == "dagre-d3") return@register false
            if (Regex("^\\s*graph").containsMatchIn(text) && renderer == "dagre-wrapper") return@register true
            Regex("^\\s*flowchart").containsMatchIn(text)
        }

        // flowchart (旧版，仅当 renderer 不是 dagre-wrapper/elk 时)
        register("flowchart") { text ->
            val renderer = config.flowchart?.defaultRenderer ?: "dagre-wrapper"
            if (renderer == "dagre-wrapper" || renderer == "elk") return@register false
            Regex("^\\s*graph").containsMatchIn(text)
        }

        // sequence
        register("sequence") { text -> Regex("^\\s*sequenceDiagram").containsMatchIn(text) }

        // classDiagram (v2 - 默认使用 dagre-wrapper)
        register("classDiagram") { text ->
            val renderer = config.`class`?.defaultRenderer ?: "dagre-wrapper"
            if (Regex("^\\s*classDiagram").containsMatchIn(text) && renderer == "dagre-wrapper") return@register true
            Regex("^\\s*classDiagram-v2").containsMatchIn(text)
        }

        // class (旧版)
        register("class") { text ->
            val renderer = config.`class`?.defaultRenderer ?: "dagre-wrapper"
            if (renderer == "dagre-wrapper") return@register false
            Regex("^\\s*classDiagram").containsMatchIn(text)
        }

        // stateDiagram (v2 - 默认使用 dagre-wrapper)
        register("stateDiagram") { text ->
            if (Regex("^\\s*stateDiagram-v2").containsMatchIn(text)) return@register true
            val renderer = config.state?.defaultRenderer ?: "dagre-wrapper"
            Regex("^\\s*stateDiagram").containsMatchIn(text) && renderer == "dagre-wrapper"
        }

        // state (旧版)
        register("state") { text ->
            val renderer = config.state?.defaultRenderer ?: "dagre-wrapper"
            if (renderer == "dagre-wrapper") return@register false
            Regex("^\\s*stateDiagram").containsMatchIn(text)
        }

        // 简单正则检测器
        register("er") { Regex("^\\s*erDiagram").containsMatchIn(it) }
        register("gantt") { Regex("^\\s*gantt").containsMatchIn(it) }
        register("pie") { Regex("^\\s*pie").containsMatchIn(it) }
        register("gitGraph") { Regex("^\\s*gitGraph").containsMatchIn(it) }
        register("info") { Regex("^\\s*info").containsMatchIn(it) }
        register("journey") { Regex("^\\s*journey").containsMatchIn(it) }

        register("c4") { text ->
            Regex("^\\s*(C4Context|C4Container|C4Component|C4Dynamic|C4Deployment)").containsMatchIn(text)
        }

        register("mindmap") { Regex("^\\s*mindmap").containsMatchIn(it) }
        register("timeline") { Regex("^\\s*timeline").containsMatchIn(it) }

        register("sankey") { Regex("^\\s*sankey(-beta)?").containsMatchIn(it) }
        register("quadrantChart") { Regex("^\\s*quadrantChart").containsMatchIn(it) }
        register("xychart") { Regex("^\\s*xychart(-beta)?").containsMatchIn(it) }
        register("requirement") { Regex("^\\s*requirement(Diagram)?").containsMatchIn(it) }
        register("block") { Regex("^\\s*block(-beta)?").containsMatchIn(it) }
        register("packet") { Regex("^\\s*packet(-beta)?").containsMatchIn(it) }
        register("kanban") { Regex("^\\s*kanban").containsMatchIn(it) }
        register("architecture") { Regex("^\\s*architecture").containsMatchIn(it) }
        register("radar") { Regex("^\\s*radar(-beta)?").containsMatchIn(it) }
        register("ishikawa") { Regex("^\\s*ishikawa(-beta)?\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) }
        register("venn") { Regex("^\\s*venn(-beta)?").containsMatchIn(it) }
        register("treemap") { Regex("^\\s*treemap").containsMatchIn(it) }
    }
}
