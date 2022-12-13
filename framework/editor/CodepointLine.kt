package com.vaticle.typedb.studio.framework.editor

import androidx.compose.ui.text.AnnotatedString
import kotlin.streams.toList

internal class CodepointLine constructor(val annotatedString: AnnotatedString) {

    fun subsequence(start: Int, end: Int): CodepointLine {
        val codepoints = annotatedString.codePoints()
        val subsequence = codepoints.toList().subList(start, end)
        return CodepointLine(AnnotatedString(subsequence.toString()))
    }

    fun length(): Int {
        return annotatedString.codePoints().toList().size
    }
}