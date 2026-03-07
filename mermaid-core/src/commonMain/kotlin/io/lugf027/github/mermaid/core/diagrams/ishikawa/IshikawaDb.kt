package io.lugf027.github.mermaid.core.diagrams.ishikawa

import io.lugf027.github.mermaid.core.db.CommonDb

data class IshikawaNode(val text: String, val children: MutableList<IshikawaNode> = mutableListOf())

class IshikawaDb : CommonDb() {
    var root: IshikawaNode? = null

    override fun clear() { super.clear(); root = null }
}
