package com.vaticle.typedb.studio.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ExecutionTabState(title: String) {
    var title: String by mutableStateOf(title)
}
