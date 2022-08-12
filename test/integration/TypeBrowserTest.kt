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

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.studio.state.StudioState
import org.junit.Ignore
import org.junit.Test

class TypeBrowserTest: IntegrationTest() {

    @Test
    fun interactiveSchemaWritesAutomaticallyDisplayed() {
        studioTest(composeRule) {
            connectToTypeDB(composeRule, DB_ADDRESS)
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

            // We can assert that the schema has been written successfully here as the schema
            // is shown in the type browser.
            composeRule.onNodeWithText("attribute").assertExists()
            composeRule.onNodeWithText("commit-date").assertExists()
            composeRule.onNodeWithText("commit-hash").assertExists()
        }
    }

    @Test
    fun collapseTypes() {
        println("Started collapseTypes")
        studioTest(composeRule) {
            connectToTypeDB(composeRule, DB_ADDRESS)
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

            StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)

            composeRule.onAllNodesWithText("Project").get(0).performClick()
            composeRule.onAllNodesWithText("Project").get(1).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_UP_ICON_STRING).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText("commit-date").assertDoesNotExist()

            StudioState.client.session.close()
        }
        println("Ended collapseTypes")
    }

    @Test
    fun collapseThenExpandTypes() {
        println("Started expandTypes")

        studioTest(composeRule) {
            connectToTypeDB(composeRule, DB_ADDRESS)
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

            StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_UP_ICON_STRING).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText("commit-date").assertDoesNotExist()

            composeRule.onNodeWithText(DOUBLE_CHEVRON_DOWN_ICON_STRING).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText("commit-date").assertExists()

            StudioState.client.session.close()
        }
        println("Ended expandTypes")
    }

    // This test is ignored as the export schema button doesn't open a new file during testing.
    @Ignore
    @Test
    fun exportSchema() {
        studioTest(composeRule) {
            connectToTypeDB(composeRule, DB_ADDRESS)
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

            StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)

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

            StudioState.client.session.close()
        }
    }
}