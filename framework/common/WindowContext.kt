/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.common

import androidx.compose.ui.awt.ComposeWindow

sealed interface WindowContext {
    val height: Int
    val width: Int
    val x: Int
    val y: Int

    class Test(
        override val height: Int,
        override val width: Int,
        override val x: Int,
        override val y: Int
    ) : WindowContext

    class Compose(val window: ComposeWindow) : WindowContext {
        override val height: Int get() = window.size.height
        override val width: Int get() = window.size.width
        override val x: Int get() = window.x
        override val y: Int get() = window.y
    }
}
