package com.vaticle.typedb.studio.state.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Settings {

    companion object {
        private const val AUTO_SAVE = true
    }

    var autosave: Boolean by mutableStateOf(AUTO_SAVE)
}