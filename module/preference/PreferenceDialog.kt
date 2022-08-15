/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.module.preference

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label

object PreferenceDialog {

    private val WIDTH = 600.dp
    private val HEIGHT = 600.dp
    @Composable
    fun MayShowDialogs() {
        if (StudioState.preference.openPreferenceDialog.isOpen) Preferences()
    }

    @Composable
    private fun Preferences() {
        val dialogState = StudioState.preference.openPreferenceDialog
        Dialog.Layout(dialogState, Label.MANAGE_PREFERENCES, WIDTH, HEIGHT) {
            Row(modifier = Modifier.fillMaxSize()) {
                Form.Text(value = "test", softWrap = true)
                Form.TextButton("Accept") {}
                Form.TextButton("Discard Changes") {}
            }
        }
    }
}