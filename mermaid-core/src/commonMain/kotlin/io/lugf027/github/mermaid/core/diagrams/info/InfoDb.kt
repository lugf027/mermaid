package io.lugf027.github.mermaid.core.diagrams.info

import io.lugf027.github.mermaid.core.db.CommonDb

class InfoDb : CommonDb() {
    var version = "mermaid-KMP v1.0.0"
    override fun clear() { super.clear(); version = "mermaid-KMP v1.0.0" }
}
