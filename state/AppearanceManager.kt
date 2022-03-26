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

package com.vaticle.typedb.studio.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppearanceManager {

    companion object {
        const val TEXT_EDITOR_SCALE_MIN = 0.5f
        const val TEXT_EDITOR_SCALE_MAX = 3f
    }
    var textEditorScale: Float by mutableStateOf(1f)

    fun increaseTextEditorScale() {
        if (textEditorScale >= TEXT_EDITOR_SCALE_MAX) return
        textEditorScale += if (textEditorScale < 1) .1f else .2f
    }

    fun decreaseTextEditorScale() {
        if (textEditorScale <= TEXT_EDITOR_SCALE_MIN) return
        textEditorScale -= if (textEditorScale <= 1) .1f else .2f
    }

    fun resetTextEditorScale() {
        textEditorScale = 1f
    }
}
