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


import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.state.StudioState
import kotlinx.coroutines.delay
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test


class TypeBrowser {
    companion object {
        private const val DB_NAME = "typebrowser"
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun interactiveSchemaWritesAutomaticallyDisplayed() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            // We have to open a project to enable the '+' to create a new file.
            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            connectToTypeDB(composeRule, DB_ADDRESS)
            createDatabase(composeRule, DB_NAME)
            writeSchemaInteractively(composeRule, DB_NAME, SCHEMA_FILE_NAME)

            // We can assert that the schema has been written successfully here as the schema
            // is shown in the type browser.
            composeRule.onNodeWithText("attribute").assertExists()
            composeRule.onNodeWithText("commit-date").assertExists()
            composeRule.onNodeWithText("commit-hash").assertExists()
        }
    }

    @Test
    fun collapseTypes() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            connectToTypeDB(composeRule, DB_ADDRESS)
            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            createDatabase(composeRule, DB_NAME)
            writeSchemaInteractively(composeRule, DB_NAME, SCHEMA_FILE_NAME)

            composeRule.onAllNodesWithText("Project").get(0).performClick()
            composeRule.onAllNodesWithText("Project").get(1).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_UP_ICON_STRING).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText("commit-date").assertDoesNotExist()
        }
    }

    @Test
    fun expandTypes() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            connectToTypeDB(composeRule, DB_ADDRESS)
            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            createDatabase(composeRule, DB_NAME)
            writeSchemaInteractively(composeRule, DB_NAME, SCHEMA_FILE_NAME)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_UP_ICON_STRING).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText("commit-date").assertDoesNotExist()

            composeRule.onNodeWithText(DOUBLE_CHEVRON_DOWN_ICON_STRING).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText("commit-date").assertExists()
        }
    }

    // This test is ignored as the export schema button doesn't open a new file as expected.
    @Ignore
    @Test
    fun exportSchema() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            connectToTypeDB(composeRule, DB_ADDRESS)
            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            createDatabase(composeRule, DB_NAME)
            writeSchemaInteractively(composeRule, DB_NAME, SCHEMA_FILE_NAME)

            StudioState.schema.exportTypeSchema { schema ->
                StudioState.project.current!!.reloadEntries()
                StudioState.project.tryCreateUntitledFile()?.let { file ->
                    file.content(schema)
                    file.tryOpen()
                }
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithText("define").assertExists()
            composeRule.onNodeWithText("# This program is free software: you can redistribute it and/or modify").assertDoesNotExist()
        }
    }
}