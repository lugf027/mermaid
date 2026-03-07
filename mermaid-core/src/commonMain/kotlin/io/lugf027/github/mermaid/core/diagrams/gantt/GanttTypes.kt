package io.lugf027.github.mermaid.core.diagrams.gantt

/**
 * 甘特图类型定义。
 */

/** 甘特图任务 */
data class GanttTask(
    val id: String,
    val section: String = "",
    val task: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val duration: String = "",
    val isActive: Boolean = false,
    val isDone: Boolean = false,
    val isCrit: Boolean = false,
    val isMilestone: Boolean = false,
    val afterId: String? = null,
    val order: Int = 0,
)

/** 甘特图分段 */
data class GanttSection(
    val name: String,
    val tasks: MutableList<GanttTask> = mutableListOf(),
)
