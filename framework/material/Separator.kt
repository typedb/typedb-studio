/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.common.theme.Theme

object Separator {

    val WEIGHT = 1.dp

    @Composable
    fun Horizontal(height: Dp = WEIGHT, color: Color = Theme.studio.border, modifier: Modifier = Modifier) {
        Spacer(modifier = modifier.fillMaxWidth().height(height = height).background(color = color))
    }

    @Composable
    fun Vertical(width: Dp = WEIGHT, color: Color = Theme.studio.border, modifier: Modifier = Modifier) {
        Spacer(modifier = modifier.fillMaxHeight().width(width = width).background(color = color))
    }
}
