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
        private const val DB_NAME = "texteditor"
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `Make a new file and save it`() {
        val funcName = object{}.javaClass.enclosingMethod.name
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            val x = composeRule.onRoot().printToString()
            println(x)

            // We have to open a project to enable the '+' to create a new file.
            val path = cloneAndOpenProject(composeRule, SAMPLE_DATA_PATH, funcName)

            val y = composeRule.onRoot().printToString()
            println(y)
            composeRule.onNodeWithText(PLUS_ICON_STRING).performClick()
            composeRule.waitForIdle()
            delay(500)

            // This sets saveFileDialog.file!! to the current file, so even though we can't see the window it is useful.
            composeRule.onNodeWithText(SAVE_ICON_STRING).performClick()
            val filePath = File("$path/Untitled1.tql").toPath()
            StudioState.project.saveFileDialog.file!!.trySave(filePath, true)
            StudioState.project.current!!.reloadEntries()

            composeRule.waitForIdle()
            delay(500)

            assertTrue(File("$path/Untitled1.tql").exists())
        }
    }

    @Test
    fun `Schema write and commit`() {
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
            writeSchemaInteractively(composeRule, DB_NAME, SCHEMA_FILE_NAME)

            composeRule.onNodeWithText(CHEVRON_UP_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()

            // We can assert that the schema has been written successfully here as the schema
            // is shown in the type browser.
            composeRule.onNodeWithText("commit-date").assertExists()
        }
    }

    @Test
    fun `Data write and commit`() {
        val funcName = object{}.javaClass.enclosingMethod.name
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            connectToTypeDB(composeRule, DB_ADDRESS)
            createDatabase(composeRule, DB_NAME)
//            StudioState.client.tryOpenSession(DB_NAME)
            writeSchemaInteractively(composeRule, DB_NAME, SCHEMA_FILE_NAME)
            writeDataInteractively(composeRule, DB_NAME, DATA_FILE_NAME)

            // We'll have to read using the client again to verify that the data was actually written.
        }
    }

    @Test
    fun `Schema write and rollback`() {
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

            delay(500)
            composeRule.waitForIdle()

            composeRule.onNodeWithText("schema").performClick()
            composeRule.onNodeWithText("write").performClick()

            StudioState.project.current!!.directory.entries.find { it.name == SCHEMA_FILE_NAME }!!.asFile().tryOpen()

            composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()
            composeRule.onNodeWithText(ROLLBACK_ICON_STRING).performClick()
            delay(500)
            composeRule.waitForIdle()

            composeRule.onNodeWithText("repository-id").assertDoesNotExist()

            StudioState.client.session.close()
        }
    }

}