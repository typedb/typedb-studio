/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.studio.view.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.editor.InputTarget.Cursor
import com.vaticle.typedb.studio.view.editor.InputTarget.Selection
import java.util.regex.MatchResult
import java.util.regex.Pattern
import kotlin.streams.toList

internal class TextFinder(
    private val file: File,
    private val target: InputTarget,
    private val processor: TextProcessor
) {

    data class LineInfo(val start: Int, val length: Int)
    data class FindArgs(val pattern: String, val isCaseSensitive: Boolean)

    private var content: String by mutableStateOf("")
    private var lineInfo: List<LineInfo> by mutableStateOf(listOf())
    private var matches: MutableList<Selection> by mutableStateOf(mutableListOf())
    private var findArgs: FindArgs? by mutableStateOf(null)
    private var position: Int by mutableStateOf(0)
    internal val hasMatches: Boolean get() = matches.isNotEmpty()

    internal fun status(): String {
        val count = if (matches.isNotEmpty()) "${position + 1} / ${matches.size}" else matches.size.toString()
        return "$count ${Label.FOUND.lowercase()}"
    }

    internal fun reset() {
        findArgs = null
        matches = mutableListOf()
        position = 0
    }

    internal fun updateContent() {
        val newLineInfo = mutableListOf<LineInfo>()
        file.content.forEachIndexed { i, line ->
            val length = if (i < file.content.size - 1) line.length + 1 else line.length
            if (i == 0) newLineInfo.add(LineInfo(0, length))
            else newLineInfo.add(LineInfo(newLineInfo[i - 1].start + newLineInfo[i - 1].length, length))
        }
        lineInfo = newLineInfo
        content = file.content.joinToString(separator = "\n")
    }

    internal fun findText(text: String, isCaseSensitive: Boolean) {
        findPattern(text, isCaseSensitive)
    }

    internal fun findWord(word: String, isCaseSensitive: Boolean) {
        findPattern("\b$word\b", isCaseSensitive)
    }

    internal fun findRegex(regex: String, isCaseSensitive: Boolean) {
        findPattern(regex, isCaseSensitive)
    }

    private fun findPattern(patternStr: String, isCaseSensitive: Boolean, lastPosition: Int = 0) {
        assert(patternStr.isNotEmpty())
        findArgs = FindArgs(patternStr, isCaseSensitive)
        matches = when {
            isCaseSensitive -> Pattern.compile(patternStr)
            else -> Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
        }.matcher(content).results().map { selection(it) }.toList().toMutableList()
        updatePosition(lastPosition)
    }

    private fun selection(match: MatchResult): Selection {
        return Selection(cursor(match.start()), cursor(match.end()))
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

    private fun updatePosition(newPosition: Int) {
        position = newPosition.coerceIn(0, (matches.size - 1).coerceAtLeast(0))
        if (hasMatches) target.updateSelection(matches[position])
        else target.clearSelection()
    }

    internal fun findNext() {
        if (!hasMatches) return
        updatePosition((position + 1) % matches.size)
    }

    internal fun findPrevious() {
        if (!hasMatches) return
        updatePosition((position - 1) % matches.size)
    }

    internal fun replaceCurrent(replaceText: String) {
        if (!hasMatches) return
        val findArgs = this.findArgs!!
        processor.insertText(replaceText)
        updateContent()
        findPattern(findArgs.pattern, findArgs.isCaseSensitive, position)
    }

    internal fun replaceAll(text: String) {
        while (hasMatches) replaceCurrent(text)
    }
}