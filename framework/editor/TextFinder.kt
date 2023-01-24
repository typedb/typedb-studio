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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.framework.editor.InputTarget.Cursor
import com.vaticle.typedb.studio.framework.editor.InputTarget.Selection
import com.vaticle.typedb.studio.framework.editor.common.GlyphLine
import com.vaticle.typedb.studio.service.common.util.Label
import java.util.regex.MatchResult
import java.util.regex.Pattern
import kotlin.streams.toList

internal class TextFinder(private val content: SnapshotStateList<GlyphLine>) {

    data class LineInfo(val start: Int, val length: Int)

    private var contentAsString: String by mutableStateOf("")
    private var lineInfo: List<LineInfo> by mutableStateOf(listOf())
    private var matches: List<Selection> by mutableStateOf(listOf())
    private var matchesByLine: Map<Int, List<Selection>> by mutableStateOf(mapOf())
    private var pattern: Pattern? by mutableStateOf(null)
    internal var position: Int by mutableStateOf(0)
    internal val hasMatches: Boolean get() = matches.isNotEmpty()

    companion object {
        private val STARTS_WITH_WORD_CHAR_PATTERN = Pattern.compile("\\w.*")
        private val ENDS_WITH_WORD_CHAR_PATTERN = Pattern.compile(".*\\w")
        private const val NON_WORD_CHAR_REGEX = "\\W"
        private const val ANY_CHAR_REGEX = "(\\w|\\W)" // because "." does capture end of / new line
    }

    internal fun status(): String {
        val count = if (matches.isNotEmpty()) "${position + 1} / ${matches.size}" else matches.size.toString()
        return "$count ${Label.FOUND.lowercase()}"
    }

    internal fun reset() {
        pattern = null
        position = 0
        matches = listOf()
        matchesByLine = mapOf()
    }

    internal fun mayRecomputeAllMatches() {
        pattern?.let {
            updateContent()
            computeAllMatches()
        }
    }

    internal fun recomputeNextMatch(from: Cursor): Selection? {
        return pattern?.let {
            updateContent()
            computeNextMatch(index(from))
        }
    }

    internal fun updateContent() {
        val newLineInfo = mutableListOf<LineInfo>()
        content.forEachIndexed { i, line ->
            val length = if (i < content.size - 1) line.length + 1 else line.length
            if (i == 0) newLineInfo.add(LineInfo(0, length))
            else newLineInfo.add(LineInfo(newLineInfo[i - 1].start + newLineInfo[i - 1].length, length))
        }
        lineInfo = newLineInfo
        contentAsString = content.joinToString(separator = "\n") { it.annotatedString }
    }

    internal fun findText(text: String, isCaseSensitive: Boolean) {
        findPattern(text, isCaseSensitive, false)
    }

    internal fun findWord(word: String, isCaseSensitive: Boolean) {
        val pre = if (STARTS_WITH_WORD_CHAR_PATTERN.matcher(word).matches()) NON_WORD_CHAR_REGEX else ANY_CHAR_REGEX
        val post = if (ENDS_WITH_WORD_CHAR_PATTERN.matcher(word).matches()) NON_WORD_CHAR_REGEX else ANY_CHAR_REGEX
        findPattern("(?<=$pre)\\Q$word\\E(?=$post)", isCaseSensitive, true)
    }

    internal fun findRegex(regex: String, isCaseSensitive: Boolean) {
        findPattern(regex, isCaseSensitive, true)
    }

    private fun findPattern(patternStr: String, isCaseSensitive: Boolean, isRegex: Boolean) {
        assert(patternStr.isNotEmpty())
        val caseFlag = if (isCaseSensitive) 0 else Pattern.CASE_INSENSITIVE
        val literalFlag = if (isRegex) 0 else Pattern.LITERAL
        try {
            pattern = Pattern.compile(patternStr, caseFlag or literalFlag)
            computeAllMatches()
            trySetPosition(0)
        } catch (e: Exception) {
            reset()
        }
    }

    private fun computeAllMatches() {
        val byLine = mutableMapOf<Int, MutableList<Selection>>()
        matches = pattern!!.matcher(contentAsString).results().map { selection(it) }.toList()
        matches.forEach {
            (it.min.row..it.end.row).forEach { i -> byLine.computeIfAbsent(i) { mutableListOf() }.add(it) }
        }
        matchesByLine = byLine
    }

    private fun computeNextMatch(index: Int): Selection? {
        if (index < 0 || index > contentAsString.length) return null
        val matcher = pattern!!.matcher(contentAsString)
        return if (matcher.find(index)) selection(matcher.toMatchResult()) else null
    }

    private fun selection(match: MatchResult): Selection {
        val contentAsGlyphLine = GlyphLine(contentAsString)
        val matchStart = contentAsGlyphLine.charToGlyphOffset(match.start())
        val matchEnd = contentAsGlyphLine.charToGlyphOffset(match.end())
        return Selection(cursor(matchStart), cursor(matchEnd))
    }

    private fun cursor(index: Int): Cursor {
        var row = 0
        var col = index

        while (col >= lineInfo[row].length) {
            col -= lineInfo[row].length
            row++
        }
        return Cursor(row, col)
    }

    private fun index(cursor: Cursor): Int {
        var index = 0
        (0..cursor.row).forEach { i ->
            index += when {
                i < cursor.row -> lineInfo[i].length
                else -> cursor.col
            }
        }
        return index
    }

    internal fun trySetPosition(newPosition: Int): Int {
        position = newPosition.coerceIn(0, (matches.size - 1).coerceAtLeast(0))
        return position
    }

    internal fun findCurrent(): Selection? {
        return if (hasMatches) matches[position] else null
    }

    internal fun findNext(): Selection? {
        return if (hasMatches) matches[trySetPosition((position + 1) % matches.size)] else null
    }

    internal fun findPrevious(): Selection? {
        return if (hasMatches) {
            var newPos = position - 1
            if (newPos < 0) newPos += matches.size
            matches[trySetPosition(newPos)]
        } else null
    }

    internal fun matches(line: Int): List<Selection> {
        return matchesByLine[line] ?: listOf()
    }
}