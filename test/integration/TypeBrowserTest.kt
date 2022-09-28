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
import com.vaticle.typedb.studio.test.integration.Utils.studioTestWithRunner
import com.vaticle.typedb.studio.test.integration.Utils.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.Utils.createDatabase
import com.vaticle.typedb.studio.test.integration.Utils.cloneAndOpenProject
import com.vaticle.typedb.studio.test.integration.Utils.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.Utils.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.Utils.SCHEMA_FILE_NAME
import com.vaticle.typedb.studio.test.integration.Utils.TQL_DATA_PATH
import com.vaticle.typedb.studio.test.integration.Utils.EXPAND_ICON_STRING
import com.vaticle.typedb.studio.test.integration.Utils.COLLAPSE_ICON_STRING
import org.junit.Ignore
import org.junit.Test

class TypeBrowserTest: IntegrationTest() {

    @Test
    fun interactiveSchemaWritesAutomaticallyDisplayed() {
        studioTestWithRunner(composeRule) { address ->
            connectToTypeDB(composeRule, address)
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
        studioTestWithRunner(composeRule) { address ->
            connectToTypeDB(composeRule, address)
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

            StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)
            delayAndRecompose(composeRule, Delays.NETWORK_IO)

            composeRule.onAllNodesWithText("Project").get(0).performClick()
            composeRule.onAllNodesWithText("Project").get(1).performClick()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText(COLLAPSE_ICON_STRING).performClick()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText("commit-date").assertDoesNotExist()
        }
    }

    @Test
    fun collapseThenExpandTypes() {
        studioTestWithRunner(composeRule) { address ->
            connectToTypeDB(composeRule, address)
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

            StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)

            composeRule.onNodeWithText(COLLAPSE_ICON_STRING).performClick()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText("commit-date").assertDoesNotExist()

            composeRule.onNodeWithText(EXPAND_ICON_STRING).performClick()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText("commit-date").assertExists()
        }
    }

    // This test is ignored as the export schema button doesn't open a new file during testing.
    @Ignore
    @Test
    fun exportSchema() {
        studioTestWithRunner(composeRule) { address ->
            connectToTypeDB(composeRule, address)
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

            StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)
            delayAndRecompose(composeRule, Delays.NETWORK_IO)

            StudioState.schema.exportTypeSchemaAsync { schema ->
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