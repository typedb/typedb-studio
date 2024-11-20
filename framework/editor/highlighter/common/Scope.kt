/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.editor.highlighter.common

import androidx.compose.ui.graphics.Color
import com.typedb.studio.framework.common.theme.Typography
import com.typedb.studio.framework.common.theme.Typography.Style.BOLD
import com.typedb.studio.framework.common.theme.Typography.Style.ITALIC
import com.typedb.studio.framework.common.theme.Typography.Style.UNDERLINE
import com.typedb.common.yaml.YAML

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
