package com.vaticle.typedb.studio.state.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

abstract class DialogManager {

    var isOpen by mutableStateOf(false)

    fun toggle() {
        isOpen = !isOpen
    }

    open fun close() {
        isOpen = false
    }

    class Base : DialogManager() {

        fun open() {
            isOpen = true
        }
    }
}