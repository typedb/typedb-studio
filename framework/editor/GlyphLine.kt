package com.vaticle.typedb.studio.framework.editor

import androidx.compose.ui.text.AnnotatedString
import com.vaticle.typedb.studio.framework.common.Util.subSequenceSafely
import javax.swing.text.GlyphView.GlyphPainter
import kotlin.streams.toList

internal class GlyphLine constructor(val annotatedString: AnnotatedString) {

    val text = annotatedString.text
    val length = annotatedString.codePoints().toList().size
    fun isEmpty(): Boolean = length == 0

    fun subSequence(start: Int, end: Int): GlyphLine {
        val codepoints = annotatedString.codePoints()
        val codepointSubsequence = codepoints.toList().subList(start, end)
        val text = codepointSubsequence.joinToString("") { Character.toString(it) }
        return GlyphLine(AnnotatedString(text))
    }

    fun subSequenceSafely(start: Int, end: Int): GlyphLine {
        val coercedStart = start.coerceIn(0, length)
        return this.subSequence(coercedStart, end.coerceIn(coercedStart, length))
    }
}