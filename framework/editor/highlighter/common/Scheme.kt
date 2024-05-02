/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.editor.highlighter.common

import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.common.yaml.YAML
import com.vaticle.typedb.studio.framework.common.theme.Color.hexToColor
import com.vaticle.typedb.studio.framework.common.theme.Typography

class Scheme(val scopes: Map<String, Scope>) {

    val globalScope get() = scopes[Scope.GLOBAL_NAME]!!

    companion object {

        private const val COLORS = "colors"
        private const val RULES = "rules"
        private const val FOREGROUND = "foreground"
        private const val BACKGROUND = "background"
        private const val STYLES = "styles"

        val TYPEQL_DARK: Scheme = createScheme("resources/schemes/typeql_dark.yml")

        private fun createScheme(filename: String): Scheme {
            val colors = mutableMapOf<String, Color>()
            val scopes = Scope.instantiateNewScopes()
            val fileStream = ClassLoader.getSystemClassLoader().getResourceAsStream(filename)!!
            val yaml = YAML.load(String(fileStream.readAllBytes())).asMap().content()
            yaml[COLORS]?.let { populateColors(colors, it.asMap().content()) }
            yaml[Scope.GLOBAL_NAME]?.let { populateGlobal(scopes, colors, it.asMap().content()) }
            yaml[RULES]?.let { populateRules(scopes, colors, it.asMap().content(), null) }
            return Scheme(scopes)
        }

        private fun populateColors(colors: MutableMap<String, Color>, colorsYAML: Map<String, YAML>) {
            colorsYAML.forEach { (name, hexYAML) -> colors[name] = hexToColor(hexYAML.asString().value()) }
        }

        private fun populateGlobal(
            scopes: Map<String, Scope>, colors: Map<String, Color>, globalYAML: MutableMap<String, YAML>
        ) {
            scopes[Scope.GLOBAL_NAME]!!.foreground = globalYAML[FOREGROUND]?.let {
                color(colors, it.asString().value())
            }
        }

        private fun populateRules(
            scopes: Map<String, Scope>, colors: Map<String, Color>,
            rulesYAML: Map<String, YAML>, parentFullName: String?
        ) {
            rulesYAML.forEach { (key, yaml) ->
                val fullName = parentFullName?.let { "$it.$key" } ?: key
                assert(scopes.contains(fullName))
                val scheme = yaml.asMap().content()
                scheme[FOREGROUND]?.let { scopes[fullName]!!.foreground = color(colors, it.asString().value()) }
                scheme[BACKGROUND]?.let { scopes[fullName]!!.background = color(colors, it.asString().value()) }
                scheme[STYLES]?.let { scopes[fullName]!!.style = listOfStyles(it.asList().content()) }
                scheme[RULES]?.let { populateRules(scopes, colors, it.asMap().content(), fullName) }
            }
        }

        private fun color(colors: Map<String, Color>, value: String): Color {
            return if (value.startsWith("#")) hexToColor(value) else colors[value]!!
        }

        private fun listOfStyles(content: List<YAML>): List<Typography.Style> {
            return content.map { Typography.Style.of(it.asString().value())!! }
        }
    }
}
