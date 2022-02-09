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

package com.vaticle.typedb.studio.view.highlighter.common

import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.common.yaml.YAML
import java.nio.file.Path

class Scope private constructor(val name: String, var parent: Scope?) {

    enum class Style { ITALIC, UNDERLINE, BOLD }

    init {
        parent?.children?.add(this)
    }

    var foreground: Color? = null
    var background: Color? = null
    var style: List<Style> = listOf()
    val children: MutableList<Scope> = mutableListOf()
    val hasScheme: Boolean get() = foreground != null || background != null || style.isNotEmpty()
    val fullName: String
        get() {
            return (parent?.let { if (it.name != GLOBAL_NAME) it.fullName + "." else "" } ?: "") + name
        }

    companion object {

        private const val GLOBAL_NAME = "global"
        private val SCOPE_DEFINITION_FILE = Path.of("view/highlighter/common/scope_definitions.yml")

        fun instantiateNewScopes(): Map<String, Scope> {
            val globalScope = Scope(name = GLOBAL_NAME, parent = null)
            val scopes = mutableMapOf(GLOBAL_NAME to globalScope)
            YAML.load(SCOPE_DEFINITION_FILE).asMap().content().map { entry ->
                createScope(entry.key, globalScope, entry.value, scopes)
            }.forEach { scopes[it.fullName] = it }
            return scopes
        }

        private fun createScope(
            name: String, globalScope: Scope, children: YAML?, scopes: MutableMap<String, Scope>
        ): Scope {
            val scope = Scope(name, globalScope)
            children?.let {
                when (it) {
                    is YAML.List -> it.content().map { child -> Scope(child.asString().value(), scope) }
                    is YAML.Map -> it.content().map { e -> createScope(e.key, scope, e.value, scopes) }
                    else -> throw IllegalArgumentException("Invalid Scope Definition File")
                }
            }?.forEach { scopes[it.fullName] = it }
            return scope
        }
    }

    override fun toString(): String {
        return fullName
    }
}