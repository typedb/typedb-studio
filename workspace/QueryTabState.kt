package com.vaticle.typedb.studio.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.UUID.randomUUID

class QueryTabState(title: String, query: String, file: File? = null) {
    var title: String by mutableStateOf(title)
    var query: String by mutableStateOf(query)
    var file: File? by mutableStateOf(file)
    val editorID: String = randomUUID().toString()
}
