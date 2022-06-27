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

package com.vaticle.typedb.studio.framework.editor.highlighter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.vaticle.typedb.studio.framework.editor.highlighter.common.Lexer
import com.vaticle.typedb.studio.framework.editor.highlighter.common.Lexer.Token
import com.vaticle.typedb.studio.framework.editor.highlighter.common.Scheme
import com.vaticle.typedb.studio.framework.editor.highlighter.common.Scope
import com.vaticle.typedb.studio.framework.editor.highlighter.typeql.TypeQLLexer
import com.vaticle.typedb.studio.state.common.util.Property

object SyntaxHighlighter {

    fun highlight(texts: List<String>, fileType: Property.FileType): List<AnnotatedString> {
        return texts.map { highlight(it, fileType) }
    }

    fun highlight(text: String, fileType: Property.FileType): AnnotatedString {
        return when (fileType) {
            Property.FileType.TYPEQL -> annotate(text, TypeQLLexer, Scheme.TYPEQL_DARK)
            else -> AnnotatedString(text)
        }
    }

    private fun annotate(text: String, lexer: Lexer, scheme: Scheme): AnnotatedString {
        if (text.isBlank()) return AnnotatedString(text)
        val builder = Builder()
        val tokens = lexer.tokenize(text, scheme)
        if (tokens.size > 64) return AnnotatedString(text) // TODO: figure out why lines with long tokens slow down rendering
        tokens.forEach { builder.appendToken(it, scheme.globalScope) }
        return builder.toAnnotatedString()
    }

    private fun Builder.appendToken(token: Token, globalScope: Scope) {
        val scope = token.scope
        this.appendText(token.text, if (scope?.hasScheme == true) scope else globalScope)
    }

    private fun Builder.appendText(text: String, scope: Scope): Builder {
        val style = SpanStyle(
            color = scope.foreground ?: Color.Unspecified,
            background = scope.background ?: Color.Unspecified,
            fontStyle = if (scope.isItalic) FontStyle.Italic else null,
            fontWeight = if (scope.isBold) FontWeight.Bold else null,
            textDecoration = if (scope.isUnderline) TextDecoration.Underline else null
        )
        this.pushStyle(style)
        this.append(text)
        this.pop()
        return this
    }
}
