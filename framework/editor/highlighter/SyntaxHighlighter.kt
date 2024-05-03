/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.editor.highlighter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.vaticle.typedb.studio.framework.editor.common.GlyphLine
import com.vaticle.typedb.studio.framework.editor.highlighter.common.Lexer
import com.vaticle.typedb.studio.framework.editor.highlighter.common.Lexer.Token
import com.vaticle.typedb.studio.framework.editor.highlighter.common.Scheme
import com.vaticle.typedb.studio.framework.editor.highlighter.common.Scope
import com.vaticle.typedb.studio.framework.editor.highlighter.typeql.TypeQLLexer
import com.vaticle.typedb.studio.service.common.util.Property

object SyntaxHighlighter {

    fun highlight(texts: List<String>, fileType: Property.FileType): List<GlyphLine> {
        return texts.map { highlight(it, fileType) }
    }

    fun highlight(text: String, fileType: Property.FileType): GlyphLine {
        return when (fileType) {
            Property.FileType.TYPEQL -> annotate(text, TypeQLLexer, Scheme.TYPEQL_DARK)
            else -> GlyphLine(text)
        }
    }

    private fun annotate(text: String, lexer: Lexer, scheme: Scheme): GlyphLine {
        if (text.isBlank()) return GlyphLine(text)
        val builder = Builder()
        val tokens = lexer.tokenize(text, scheme)
        if (tokens.size > 64) return GlyphLine(text) // TODO: figure out why lines with long tokens slow down rendering
        tokens.forEach { builder.appendToken(it, scheme.globalScope) }
        val annotatedString = builder.toAnnotatedString()
        return GlyphLine(annotatedString)
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
