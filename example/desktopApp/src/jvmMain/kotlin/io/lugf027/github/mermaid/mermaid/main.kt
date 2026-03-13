package io.lugf027.github.mermaid.mermaid

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "mermaid",
    ) {
        App()
    }
}
