package io.github.lugf027.mermiad

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform