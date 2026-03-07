package io.lugf027.github.mermaid.core.diagrams.c4

import io.lugf027.github.mermaid.core.db.CommonDb

class C4Db : CommonDb() {
    private val shapes = mutableListOf<C4Shape>()
    private val boundaries = mutableListOf<C4Boundary>()
    private val rels = mutableListOf<C4Rel>()
    private var currentBoundary: String? = null
    var c4Type = "C4Context"
        private set

    fun setC4Type(t: String) { c4Type = t }

    fun addShape(shape: C4Shape) {
        shapes.add(shape.copy(parentBoundary = currentBoundary ?: shape.parentBoundary))
    }

    fun addBoundary(boundary: C4Boundary) {
        boundaries.add(boundary.copy(parentBoundary = currentBoundary))
    }

    fun pushBoundary(alias: String) { currentBoundary = alias }
    fun popBoundary() { currentBoundary = boundaries.lastOrNull { it.alias == currentBoundary }?.parentBoundary }

    fun addRel(rel: C4Rel) { rels.add(rel) }

    fun getShapes(): List<C4Shape> = shapes
    fun getBoundaries(): List<C4Boundary> = boundaries
    fun getRels(): List<C4Rel> = rels

    override fun clear() { super.clear(); shapes.clear(); boundaries.clear(); rels.clear(); currentBoundary = null }
}
