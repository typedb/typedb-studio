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

// We need to access private function Studio.MainWindowColumn, this allows us to.
// Do not use this outside of tests anywhere. It is extremely dangerous to do so.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.vaticle.typedb.studio.test.integration

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.test.integration.runComposeRule
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.project.FileState
import com.vaticle.typedb.studio.state.project.PathState
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextEditor {
    companion object {
        private const val DB_ADDRESS = "localhost:1729"
        private const val DB_NAME = "github"

        private val SAMPLE_DATA_PATH = File("test/data/sample_file_structure").absolutePath
        private val TQL_DATA_PATH = File("test/data").absolutePath

        private val SAVE_ICON_STRING = Icon.Code.FLOPPY_DISK.unicode
        private val PLUS_ICON_STRING = Icon.Code.PLUS.unicode

        private val PLAY_ICON_STRING = Icon.Code.PLAY.unicode
        private val CHECK_ICON_STRING = Icon.Code.CHECK.unicode
        private val ROLLBACK_ICON_STRING = Icon.Code.ROTATE_LEFT.unicode

        private val CHEVRON_UP_ICON_STRING = Icon.Code.CHEVRON_UP.unicode
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `Make a New File and Save It`() {
        val funcName = object{}.javaClass.enclosingMethod.name
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            // We have to open a project to enable the '+' to create a new file.
            val path = cloneAndOpenProject(composeRule, SAMPLE_DATA_PATH, funcName)

            composeRule.onNodeWithText(PLUS_ICON_STRING).performClick()

            composeRule.waitForIdle()
            delay(500)

            composeRule.onNodeWithText("Untitled1.tql *").assertExists()

            // This sets saveFileDialog.file!! to the current file, so even though we can't see the window it is useful.
            composeRule.onNodeWithText(SAVE_ICON_STRING).performClick()
            val filePath = File("$path/Untitled1.tql").toPath()
            StudioState.project.saveFileDialog.file!!.trySave(filePath, true)
            StudioState.project.current!!.reloadEntries()

            composeRule.waitForIdle()
            delay(500)

            composeRule.onNodeWithText("Untitled1.tql").assertExists()
        }
    }

    @Test
    fun `Schema Write and Commit`() {
        val funcName = object{}.javaClass.enclosingMethod.name
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            // We have to open a project to enable the '+' to create a new file.
            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            connectToTypeDB(composeRule, DB_ADDRESS)
            createDatabase(composeRule, DB_NAME)

            StudioState.client.session.tryOpen(DB_NAME, TypeDBSession.Type.SCHEMA)
            composeRule.waitForIdle()
            delay(500)

            composeRule.onNodeWithText("schema").performClick()
            composeRule.onNodeWithText("write").performClick()

            StudioState.project.current!!.directory.entries.find { it.name == "schema_string.tql" }!!.asFile().tryOpen()

            composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()
            composeRule.onNodeWithText(CHECK_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()

            assertEquals(StudioState.notification.queue.last().code, "CNX10")

            composeRule.onNodeWithText(CHEVRON_UP_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()

            // Trying to click on Log requires an AWT backed API (event).
//            composeRule.onNodeWithText(Label.LOG).performClick()
//            delay(500)
//            composeRule.waitForIdle()

            // We can assert that the schema has been written successfully here as the schema
            // is shown in the type browser.
            composeRule.onNodeWithText("commit").assertExists()
        }
    }

    @Test
    fun `Schema Write and Rollback`() {
        val funcName = object{}.javaClass.enclosingMethod.name
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            // We have to open a project to enable the '+' to create a new file.
            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            connectToTypeDB(composeRule, DB_ADDRESS)
            createDatabase(composeRule, DB_NAME)

            StudioState.client.session.tryOpen(DB_NAME, TypeDBSession.Type.SCHEMA)
            composeRule.waitForIdle()
            delay(500)

            composeRule.onNodeWithText("schema").performClick()
            composeRule.onNodeWithText("write").performClick()

            StudioState.project.current!!.directory.entries.find { it.name == "schema_string.tql" }!!.asFile().tryOpen()

            composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()
            composeRule.onNodeWithText(ROLLBACK_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()

            assertEquals(StudioState.notification.queue.last().code, "CNX09")

            // Trying to click on Log requires an AWT backed API (event).
//            composeRule.onNodeWithText(Label.LOG).performClick()
//            delay(500)
//            composeRule.waitForIdle()

            // We can assert that the schema has been written successfully here as the schema
            // is shown in the type browser.
            composeRule.onNodeWithText("commit").assertDoesNotExist()
        }
    }

    @Test
    fun `Data Write and Commit`() {
        val funcName = object{}.javaClass.enclosingMethod.name
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            connectToTypeDB(composeRule, DB_ADDRESS)
            createDatabase(composeRule, DB_NAME)

            StudioState.client.session.tryOpen(DB_NAME, TypeDBSession.Type.SCHEMA)
            composeRule.waitForIdle()
            delay(500)

            composeRule.onNodeWithText("schema").performClick()
            composeRule.onNodeWithText("write").performClick()

            StudioState.project.current!!.directory.entries.find { it.name == "schema_string.tql" }!!.asFile().tryOpen()

            composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()
            composeRule.onNodeWithText(CHECK_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()

            assertEquals(StudioState.notification.queue.last().code, "CNX10")

            StudioState.client.session.tryOpen(DB_NAME, TypeDBSession.Type.DATA)

            composeRule.waitForIdle()
            delay(500)

            composeRule.onNodeWithText("data").performClick()
            composeRule.onNodeWithText("write").performClick()

            StudioState.project.current!!.directory.entries.find { it.name == "data_string.tql" }!!.asFile().tryOpen()

            composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()

            composeRule.onNodeWithText(CHECK_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()

            // I think we'll have to read using the client again to verify that the data was actually written.

            assertEquals(StudioState.notification.queue.last().code, "CNX10")
        }
    }

    @Ignore
    @Test
    fun `Data Read Query`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()
        }
    }

    @Ignore
    @Test
    fun `Data Read Requiring Infer Query`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()
        }
    }
}