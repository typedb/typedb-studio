package com.vaticle

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.graph.renderer.GraphVisualiser

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose for Desktop",
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {
        MaterialTheme {
            GraphVisualiser()
        }
    }
}
