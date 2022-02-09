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

package com.vaticle.typedb.studio.view.highlighter.language

import com.vaticle.typedb.common.yaml.YAML
import com.vaticle.typedb.studio.view.highlighter.common.Lexer
import com.vaticle.typedb.studio.view.highlighter.common.Lexer.Token
import com.vaticle.typedb.studio.view.highlighter.common.Scheme
import com.vaticle.typeql.grammar.TypeQLLexer.VOCABULARY
import com.vaticle.typeql.lang.TypeQL
import java.nio.file.Path
import org.antlr.v4.runtime.CommonTokenStream

// TODO: we should reimplement this class using a JFlex lexer,
//       instead of our native ANTLR lexer, to get more powerful
//       tokenisation rules using regular expressions
object TypeQLLexer : Lexer {

    private val TYPEQL_SCOPES_FILE = Path.of("view/highlighter/language/typeql_scopes.yml")
    private val tokenScope: Map<String, String> = loadTokenScopeDefinition()

    private fun loadTokenScopeDefinition(): Map<String, String> {
        val scopes = mutableMapOf<String, String>()
        val yaml = YAML.load(TYPEQL_SCOPES_FILE).asMap()
        yaml.forEach { antlrToken, scope -> scopes[antlrToken] = scope.asString().value() }
        return scopes
    }

    override fun tokenize(text: String, scheme: Scheme): List<Token> {
        val tokenStream = CommonTokenStream(TypeQL.lexer(text))
        tokenStream.fill()
        val antlrTokens = tokenStream.tokens.filter { it.type > 0 }
        return antlrTokens.map { antlrToken ->
            Token(antlrToken.text, antlrTokenName(antlrToken)?.let { tokenName ->
                tokenScope[tokenName]?.let { scopeName -> scheme.scopes[scopeName] }
            })
        }
    }

    private fun antlrTokenName(antlrToken: org.antlr.v4.runtime.Token): String? {
        return VOCABULARY.getSymbolicName(antlrToken.type)
    }
}