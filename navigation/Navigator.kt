package com.vaticle.typedb.studio.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class Navigator(initialState: ScreenState) {

    var activeScreenState: ScreenState by mutableStateOf(initialState)
        private set

    fun pushState(newScreenState: ScreenState) {
        activeScreenState = newScreenState
    }
}
