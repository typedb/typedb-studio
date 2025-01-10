/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.editor.highlighter.typeql

import com.typedb.studio.framework.editor.highlighter.common.Lexer
import com.typedb.studio.framework.editor.highlighter.common.Lexer.Token
import com.typedb.studio.framework.editor.highlighter.common.Scheme
import com.typedb.common.yaml.YAML
import com.typeql.grammar.TypeQLLexer.VOCABULARY
import com.typeql.lang.TypeQL
import org.antlr.v4.runtime.CommonTokenStream

// TODO: we should reimplement this using a JFlex lexer,
//       instead of our native ANTLR lexer, to get more powerful
//       tokenisation rules using regular expressions
object TypeQLLexer : Lexer {

    private val TYPEQL_SCOPES_FILE = "framework/editor/highlighter/typeql/typeql_scopes.yml"
    private val tokenScope: Map<String, String> = loadTokenScopeDefinition()

    private fun loadTokenScopeDefinition(): Map<String, String> {
        val scopes = mutableMapOf<String, String>()
        val fileStream = ClassLoader.getSystemClassLoader().getResourceAsStream(TYPEQL_SCOPES_FILE)!!
        val yaml = YAML.load(String(fileStream.readAllBytes())).asMap()
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
