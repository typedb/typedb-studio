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

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertTrue


val SAVE_ICON_STRING = Icon.Code.FLOPPY_DISK.unicode
val PLUS_ICON_STRING = Icon.Code.PLUS.unicode
val PLAY_ICON_STRING = Icon.Code.PLAY.unicode
val CHECK_ICON_STRING = Icon.Code.CHECK.unicode
val ROLLBACK_ICON_STRING = Icon.Code.ROTATE_LEFT.unicode
val CHEVRON_UP_ICON_STRING = Icon.Code.CHEVRON_UP.unicode
val DOUBLE_CHEVRON_DOWN_ICON_STRING = Icon.Code.CHEVRONS_DOWN.unicode
val DOUBLE_CHEVRON_UP_ICON_STRING = Icon.Code.CHEVRONS_UP.unicode

val SAMPLE_DATA_PATH = File("test/data/sample_file_structure").absolutePath
val TQL_DATA_PATH = File("test/data").absolutePath

const val QUERY_FILE_NAME = "query_string.tql"
const val DATA_FILE_NAME = "data_string.tql"
const val SCHEMA_FILE_NAME = "schema_string.tql"

const val DB_ADDRESS = "localhost:1729"

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

suspend fun connectToTypeDB(composeRule: ComposeContentTestRule, address: String) {
    // This opens a dialog box (which we can't see through) so we assert that buttons with that text can be
    // clicked.
    composeRule.onAllNodesWithText(Label.CONNECT_TO_TYPEDB).assertAll(hasClickAction())

    StudioState.client.tryConnectToTypeDB(address) {}
    // We wait to connect to TypeDB. This can be slow by default on macOS, so we wait a while.
    delay(5_000)
    // Order is important here! We delay allowing the connection to take place, then give the program
    // time to recompose. If we waitForIdle then delay, it recomposes before connecting failing the
    // next assertExists.
    composeRule.waitForIdle()
    assertTrue(StudioState.client.isConnected)

    composeRule.onNodeWithText(address).assertExists()
}

suspend fun createDatabase(composeRule: ComposeContentTestRule, name: String) {
    // Same as connecting to typedb, but we can't see dropdowns either.
    composeRule.onAllNodesWithText(Label.SELECT_DATABASE).assertAll(hasClickAction())

    try {
        StudioState.client.tryDeleteDatabase(name)
    }
    catch (_: Exception) {}
    delay(1_000)

    StudioState.client.tryCreateDatabase(name) {}
    delay(1_000)

    StudioState.client.tryOpenSession(name)
    delay(1_000)
}
