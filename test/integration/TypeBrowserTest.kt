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
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.test.integration.common.Data.DOUBLE_CHEVRON_DOWN_ICON_STRING
import com.vaticle.typedb.studio.test.integration.common.Data.DOUBLE_CHEVRON_UP_ICON_STRING
import com.vaticle.typedb.studio.test.integration.common.Data.SCHEMA_FILE_NAME
import com.vaticle.typedb.studio.test.integration.common.Data.TQL_DATA_PATH
import com.vaticle.typedb.studio.test.integration.common.Delays.NETWORK_IO
import com.vaticle.typedb.studio.test.integration.common.StudioActions.cloneAndOpenProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.common.StudioTestHelpers.studioTestWithRunner
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
            composeRule.onNodeWithText(Label.ATTRIBUTE.lowercase()).assertExists()
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
            delayAndRecompose(composeRule, NETWORK_IO)

            composeRule.onAllNodesWithText(Label.PROJECT).get(0).performClick()
            composeRule.onAllNodesWithText(Label.PROJECT).get(1).performClick()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_UP_ICON_STRING).performClick()
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

            composeRule.onNodeWithText(DOUBLE_CHEVRON_UP_ICON_STRING).performClick()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText("commit-date").assertDoesNotExist()

            composeRule.onNodeWithText(DOUBLE_CHEVRON_DOWN_ICON_STRING).performClick()
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
            delayAndRecompose(composeRule, NETWORK_IO)

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