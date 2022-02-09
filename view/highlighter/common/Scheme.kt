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

import com.vaticle.typedb.common.yaml.YAML
import com.vaticle.typedb.studio.view.common.theme.Color.hexToColor
import java.nio.file.Path

class Scheme(val scopes: Map<String, Scope>) {

    companion object {

        private const val FOREGROUND = "foreground"
        private const val BACKGROUND = "background"
        private const val FONT_STYLE = "font_style"
        private const val RULES = "rules"

        val DRACULA: Scheme = createScheme("resources/schemes/dracula.yml")

        private fun createScheme(filename: String): Scheme {
            val scopes = Scope.instantiateNewScopes()
            val yaml = YAML.load(Path.of(filename)).asMap().content()
            yaml[Scope.GLOBAL_NAME]?.let { populateGlobal(scopes, it.asMap().content()) }
            yaml[RULES]?.let { populateRules(scopes, it.asMap().content(), null) }
            return Scheme(scopes)
        }

        private fun populateGlobal(scopes: Map<String, Scope>, global: MutableMap<String, YAML>) {
            scopes[Scope.GLOBAL_NAME]!!.foreground = global[FOREGROUND]?.let { hexToColor(it.asString().value()) }
        }

        private fun populateRules(scopes: Map<String, Scope>, rules: Map<String, YAML>, parentFullName: String?) {
            rules.forEach { (key, yaml) ->
                val fullName = parentFullName?.let { "$it.$key" } ?: key
                assert(scopes.contains(fullName))
                val scheme = yaml.asMap().content()
                scheme[FOREGROUND]?.let { scopes[fullName]!!.foreground = hexToColor(it.asString().value()) }
                scheme[BACKGROUND]?.let { scopes[fullName]!!.background = hexToColor(it.asString().value()) }
                scheme[FONT_STYLE]?.let { scopes[fullName]!!.fontStyle = listOfStyles(it.asList().content()) }
                scheme[RULES]?.let { populateRules(scopes, it.asMap().content(), fullName) }
            }
        }

        private fun listOfStyles(content: List<YAML>): List<Scope.FontStyle> {
            return content.map { Scope.FontStyle.of(it.asString().value())!! }
        }
    }
}