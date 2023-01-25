/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.framework.editor.common

import androidx.compose.ui.text.AnnotatedString
import kotlin.streams.toList

class GlyphLine constructor(val annotatedString: AnnotatedString) {

    constructor(text: String) : this(AnnotatedString(text))

    private val codepoints = annotatedString.codePoints().toList()

    val length = codepoints.size

    fun isEmpty(): Boolean = length == 0

    private fun subSequence(start: Int, end: Int): GlyphLine {
        val codepointSubsequence = codepoints.subList(start, end)
        val text = codepointSubsequence.joinToString("") { Character.toString(it) }
        return GlyphLine(text)
    }

    operator fun plus(other: GlyphLine): GlyphLine {
        return GlyphLine(this.annotatedString.text + other.annotatedString.text)
    }

    fun indexOf(element: String): Int {
        return charToGlyphOffset(annotatedString.indexOf(element))
    }

    fun subSequenceSafely(start: Int, end: Int): GlyphLine {
        if (isEmpty()) return GlyphLine("")
        val coercedStart = start.coerceIn(0, length)
        val coercedEnd = end.coerceIn(coercedStart, length)
        return this.subSequence(coercedStart, coercedEnd)
    }

    fun glyphToCharOffset(_glyphOffset: Int): Int {
        var glyphOffset = _glyphOffset
        if (_glyphOffset <= 0) return 0
        if (_glyphOffset > length) glyphOffset = length
        return codepoints.subList(0, glyphOffset).sumOf { Character.charCount(it) }
    }

    fun charToGlyphOffset(_charOffset: Int): Int {
        var charOffset = _charOffset
        if (_charOffset <= 0) return 0

        var glyphOffset = 0
        for (code in codepoints) {
            charOffset -= Character.charCount(code)
            glyphOffset += 1
            if (charOffset == 0) {
                return glyphOffset
            }
        }
        return length
    }

    companion object {
        fun String.toGlyphLines(): List<GlyphLine> {
            return if (this.isEmpty()) listOf() else this.split("\n").map { GlyphLine(it) }
        }
    }
}
