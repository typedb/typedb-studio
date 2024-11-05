/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp

object Context {
    val LocalWindow = staticCompositionLocalOf<ComposeWindow?> { null }
    val LocalWindowContext = staticCompositionLocalOf<WindowContext?> { null }
    val LocalTitleBarHeight = staticCompositionLocalOf { 0.dp }
}
