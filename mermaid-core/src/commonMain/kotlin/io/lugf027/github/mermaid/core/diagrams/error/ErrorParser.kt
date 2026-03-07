package io.lugf027.github.mermaid.core.diagrams.error

import io.lugf027.github.mermaid.core.types.ParserDefinition

class ErrorParser(private val db: ErrorDb) : ParserDefinition {
    override fun parse(input: String) { db.errorMessage = input }
}
