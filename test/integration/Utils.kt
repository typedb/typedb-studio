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

package com.vaticle.typedb.studio.test.integration

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.vaticle.typedb.studio.state.StudioState
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun runComposeRule(compose: ComposeContentTestRule, rule: suspend ComposeContentTestRule.() -> Unit) {
    runBlocking { compose.rule() }
}

fun fileNameToString(fileName: String): String {
    return Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8).filter { line -> !line.startsWith('#') }
        .joinToString("")
}

fun cloneAndOpenProject(composeRule: ComposeContentTestRule, path: String, name: String): Path {
    // This line looks needlessly verbose, but attempting to use File(name) as a unique identifier for a single location
    // isn't sufficient.
    val absolute = File(File(name).absolutePath)
    absolute.deleteRecursively()
    File(path).copyRecursively(overwrite = true, target = absolute)
    StudioState.project.tryOpenProject(absolute.toPath())
    StudioState.appData.project.path = absolute.toPath()
    composeRule.waitForIdle()
    return absolute.toPath()
}
