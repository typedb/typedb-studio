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

package com.vaticle.typedb.studio.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object Form {

    private const val LABEL_WEIGHT = 2f
    private const val INPUT_WEIGHT = 3f
    private val FIELD_SPACING = 12.dp
    private val FIELD_HEIGHT = 28.dp

    val ColumnScope.LABEL_MODIFIER: Modifier get() = Modifier.weight(LABEL_WEIGHT).height(FIELD_HEIGHT)
    val ColumnScope.INPUT_MODIFIER: Modifier get() = Modifier.weight(INPUT_WEIGHT).height(FIELD_HEIGHT)

    @Composable
    fun Field(content: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }

    @Composable
    fun FieldGroup(content: @Composable () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(FIELD_SPACING)) {
            content()
        }
    }
}