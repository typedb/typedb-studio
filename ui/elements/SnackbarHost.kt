/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.studio.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme

@Composable
fun StudioSnackbarHost(snackbarHostState: SnackbarHostState){

    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = {
            // TODO: it's too big
            Snackbar (
                action = {
                    Text(
                        text = snackbarHostState.currentSnackbarData?.actionLabel ?: "",
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                snackbarHostState.currentSnackbarData?.dismiss()
                            },
                        style = StudioTheme.typography.body1.copy(color = StudioTheme.colors.onPrimary, fontWeight = FontWeight.SemiBold)
                    )
                },
            ) {
                Text(
                    text = snackbarHostState.currentSnackbarData?.message ?: "",
                    style = StudioTheme.typography.body1.copy(color = StudioTheme.colors.onPrimary))
            }
        },
    )
}
