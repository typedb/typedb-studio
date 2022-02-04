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
import com.vaticle.typedb.studio.view.editor.InputTarget.Cursor
import com.vaticle.typedb.studio.view.editor.InputTarget.Selection
import java.util.regex.MatchResult
import java.util.regex.Pattern
import kotlin.streams.toList

internal class TextFinder(val file: File) {

    data class LineInfo(val start: Int, val length: Int)

    private var content: String by mutableStateOf("")
    private var lineInfo: List<LineInfo> by mutableStateOf(listOf())
    private var matches: List<Selection> by mutableStateOf(listOf())
    private var target: Int by mutableStateOf(0)
    internal val hasMatches: Boolean get() = matches.isNotEmpty()

    internal fun status(): String {
        val count = if (matches.isNotEmpty()) "${target + 1} / ${matches.size}" else matches.size.toString()
        return "$count found"
    }

    internal fun reset() {
        matches = listOf()
        target = 0
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

    private fun findPattern(string: String, isCaseSensitive: Boolean) {
        assert(string.isNotEmpty())
        val pattern = if (isCaseSensitive) Pattern.compile(string)
        else Pattern.compile(string, Pattern.CASE_INSENSITIVE)
        matches = pattern.matcher(content).results().map { selection(it) }.toList()
        target = 0
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

    internal fun findNext() {
        target = (target + 1) % matches.size
        println("findNext() -> ${matches[target].label()}") // TODO: remove
    }

    internal fun findPrevious() {
        target = (target - 1) % matches.size
        println("findPrevious() -> ${matches[target].label()}") // TODO: remove
    }

    internal fun replaceNext(text: String) {
        println("replaceNext() -> text: $text")
        // TODO
    }

    internal fun replaceAll(text: String) {
        println("replaceAll() -> text: $text")
        // TODO
    }
}