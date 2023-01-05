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