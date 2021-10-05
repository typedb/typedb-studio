package com.vaticle.typedb.studio.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class QueryTabState(private val initialTitle: String, private val initialQuery: String) {
    var title: String by mutableStateOf(initialTitle)
    var query: String by mutableStateOf(initialQuery)
}
