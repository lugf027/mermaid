package io.github.lugf027.mermiad

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kmp_mermaid",
    ) {
        App()
    }
}