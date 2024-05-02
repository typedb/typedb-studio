/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.material

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.common.theme.Theme.SCROLLBAR_END_PADDING
import com.vaticle.typedb.studio.framework.common.theme.Theme.SCROLLBAR_LONG_PADDING

object Scrollbar {

    @Composable
    fun Vertical(adapter: ScrollbarAdapter, modifier: Modifier, containerSize: Dp = 0.dp) {
        val density = LocalDensity.current.density
        val maxOffset = adapter.maxScrollOffset((containerSize.value * density).toInt())
        if (maxOffset > 0 && maxOffset < Int.MAX_VALUE) {
            VerticalScrollbar(adapter, modifier.fillMaxHeight().padding(SCROLLBAR_LONG_PADDING, SCROLLBAR_END_PADDING))
        }
    }

    @Composable
    fun Horizontal(adapter: ScrollbarAdapter, modifier: Modifier, containerSize: Dp = 0.dp) {
        val density = LocalDensity.current.density
        val maxOffset = adapter.maxScrollOffset((containerSize.value * density).toInt())
        if (maxOffset > 0 && maxOffset < Int.MAX_VALUE) {
            HorizontalScrollbar(adapter, modifier.fillMaxWidth().padding(SCROLLBAR_END_PADDING, SCROLLBAR_LONG_PADDING))
        }
    }
}
