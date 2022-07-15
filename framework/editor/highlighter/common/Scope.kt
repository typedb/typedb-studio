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

package com.vaticle.typedb.studio.framework.editor.highlighter.common

import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.common.yaml.YAML
import com.vaticle.typedb.studio.framework.common.theme.Typography
import com.vaticle.typedb.studio.framework.common.theme.Typography.Style.BOLD
import com.vaticle.typedb.studio.framework.common.theme.Typography.Style.ITALIC
import com.vaticle.typedb.studio.framework.common.theme.Typography.Style.UNDERLINE

class Scope private constructor(val name: String, var parent: Scope?) {

    init {
        parent?.children?.add(this)
    }

    var foreground: Color? = null
        get() = if (parent == null || field != null) field else parent!!.foreground
    var background: Color? = null
        get() = if (parent == null || field != null) field else parent!!.background
    var style: List<Typography.Style> = listOf()
        get() = if (parent == null || field.isNotEmpty()) field else parent!!.style
    val isItalic: Boolean get() = style.contains(ITALIC)
    val isBold: Boolean get() = style.contains(BOLD)
    val isUnderline: Boolean get() = style.contains(UNDERLINE)
    val hasScheme: Boolean get() = foreground != null || background != null || style.isNotEmpty()
    val children: MutableList<Scope> = mutableListOf()
    val fullName: String
        get() {
            return (parent?.let { if (it.name != GLOBAL_NAME) it.fullName + "." else "" } ?: "") + name
        }

    companion object {

        internal const val GLOBAL_NAME = "global"
        private const val SCOPE_DEFINITION_FILE = "framework/editor/highlighter/common/scope_definitions.yml"

        fun instantiateNewScopes(): Map<String, Scope> {
            val globalScope = Scope(name = GLOBAL_NAME, parent = null)
            val scopes = mutableMapOf(GLOBAL_NAME to globalScope)
            val fileStream = ClassLoader.getSystemClassLoader().getResourceAsStream(SCOPE_DEFINITION_FILE)!!
            YAML.load(String(fileStream.readAllBytes())).asMap().content().map { entry ->
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