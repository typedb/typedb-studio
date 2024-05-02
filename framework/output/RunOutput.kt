/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.output

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.IconButton
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Separator

internal sealed class RunOutput {

    abstract val name: String
    abstract val icon: Icon
    abstract val buttons: List<Form.IconButtonArg>

    @Composable
    abstract fun content(modifier: Modifier)

    @Composable
    internal fun Layout() {
        Row {
            Toolbar(Modifier.fillMaxHeight().width(Theme.TOOLBAR_SIZE), buttons)
            Separator.Vertical()
            content(Modifier.fillMaxHeight().weight(1f).background(Theme.studio.backgroundDark))
        }
    }

    @Composable
    private fun Toolbar(modifier: Modifier, buttons: List<Form.IconButtonArg>) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            buttons.forEach {
                Spacer(Modifier.height(Theme.TOOLBAR_SPACING))
                IconButton(
                    icon = it.icon,
                    hoverIcon = it.hoverIcon,
                    modifier = Modifier.size(Theme.TOOLBAR_BUTTON_SIZE),
                    iconColor = it.color(),
                    iconHoverColor = it.hoverColor?.invoke(),
                    disabledColor = it.disabledColor?.invoke(),
                    enabled = it.enabled,
                    tooltip = it.tooltip,
                    onClick = it.onClick
                )
            }
        }
    }
}
