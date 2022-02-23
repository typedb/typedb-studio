package com.vaticle.typedb.studio.view.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposeWindow

object Context {

    val LocalWindow = staticCompositionLocalOf<ComposeWindow?> { null }
}