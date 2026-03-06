package io.lugf027.github.mermaid.mermaid

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform