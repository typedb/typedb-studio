/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.common

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
