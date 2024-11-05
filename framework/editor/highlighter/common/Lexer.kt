/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.editor.highlighter.common

interface Lexer {

    data class Token(val text: String, val scope: Scope?)

    fun tokenize(text: String, scheme: Scheme): List<Token>
}
