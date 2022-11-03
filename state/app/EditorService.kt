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

package com.vaticle.typedb.studio.state.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class EditorService {

    companion object {
        const val TEXT_EDITOR_SCALE_MIN = 5 // over 10
        const val TEXT_EDITOR_SCALE_MAX = 30 // over 10
        const val TEXT_EDITOR_SCALE_DEFAULT = 10 // over 10
    }

    private var _scale: Int by mutableStateOf(TEXT_EDITOR_SCALE_DEFAULT)
    val scale: Float get() = _scale / 10f
    val isMaxScale get() = _scale == TEXT_EDITOR_SCALE_MAX
    val isMinScale get() = _scale == TEXT_EDITOR_SCALE_MIN
    val isDefaultScale get() = _scale == TEXT_EDITOR_SCALE_DEFAULT

    fun increaseScale() {
        if (_scale >= TEXT_EDITOR_SCALE_MAX) return
        _scale += if (_scale < TEXT_EDITOR_SCALE_DEFAULT) 1 else 2
    }

    fun decreaseScale() {
        if (_scale <= TEXT_EDITOR_SCALE_MIN) return
        _scale -= if (_scale <= TEXT_EDITOR_SCALE_DEFAULT) 1 else 2
    }

    fun resetScale() {
        _scale = TEXT_EDITOR_SCALE_DEFAULT
    }
}
