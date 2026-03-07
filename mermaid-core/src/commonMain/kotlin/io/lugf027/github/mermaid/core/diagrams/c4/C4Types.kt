package io.lugf027.github.mermaid.core.diagrams.c4

enum class C4ShapeType { PERSON, SYSTEM, CONTAINER, COMPONENT, SYSTEM_DB, CONTAINER_DB, SYSTEM_QUEUE, CONTAINER_QUEUE, SYSTEM_EXT, PERSON_EXT }

data class C4Shape(
    val alias: String,
    val label: String,
    val description: String = "",
    val technology: String = "",
    val type: C4ShapeType = C4ShapeType.SYSTEM,
    val parentBoundary: String? = null,
)

data class C4Boundary(
    val alias: String,
    val label: String,
    val type: String = "",
    val parentBoundary: String? = null,
)

data class C4Rel(
    val from: String,
    val to: String,
    val label: String = "",
    val technology: String = "",
    val description: String = "",
)
