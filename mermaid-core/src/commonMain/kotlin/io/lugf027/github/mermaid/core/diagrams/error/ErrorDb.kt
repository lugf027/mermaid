package io.lugf027.github.mermaid.core.diagrams.error

import io.lugf027.github.mermaid.core.db.CommonDb

class ErrorDb : CommonDb() {
    var errorMessage = "Syntax error in diagram"
    override fun clear() { super.clear(); errorMessage = "Syntax error in diagram" }
}
