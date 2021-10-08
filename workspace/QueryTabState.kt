package com.vaticle.typedb.studio.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID.randomUUID

class QueryTabState(title: String, query: String) {
    var title: String by mutableStateOf(title)
    var query: String by mutableStateOf(query)
    val editorID: String = title
//    val codeEditor = CodeEditorState(randomUUID().toString(), query)
//
//    var query: String
//    get() = codeEditor.code
//    set(value) {
//        codeEditor.code = value
//    }
}
