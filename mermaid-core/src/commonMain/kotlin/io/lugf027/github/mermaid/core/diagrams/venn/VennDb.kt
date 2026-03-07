package io.lugf027.github.mermaid.core.diagrams.venn

import io.lugf027.github.mermaid.core.db.CommonDb

data class VennSet(val id: String, val label: String)

class VennDb : CommonDb() {
    private val sets = mutableListOf<VennSet>()
    fun addSet(set: VennSet) { sets.add(set) }
    fun getSets() = sets
    override fun clear() { super.clear(); sets.clear() }
}
