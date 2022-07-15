package com.vaticle.typedb.studio.framework.common

import androidx.compose.ui.awt.ComposeWindow

class WindowContext(val height: Int, val width: Int, val x: Int, val y: Int) {
    constructor(window: ComposeWindow): this(window.height, window.width, window.x, window.y)
}