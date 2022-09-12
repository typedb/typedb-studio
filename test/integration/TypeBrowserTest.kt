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

// We need to access the private function StudioState.client.session.tryOpen, this allows us to.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.vaticle.typedb.studio.test.integration

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.test.integration.common.Paths.SCHEMA_FILE_NAME
import com.vaticle.typedb.studio.test.integration.common.Paths.TQL_DATA_PATH
import com.vaticle.typedb.studio.test.integration.common.Delays
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createData
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.StudioActions.nodeWithTextDoesNotExist
import com.vaticle.typedb.studio.test.integration.common.StudioActions.nodeWithTextExists
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

class TypeBrowserTest: IntegrationTest() {

    @Test
    fun interactiveSchemaWritesAutomaticallyDisplayed() {
        withTypeDB { address ->
            runBlocking {
                connectToTypeDB(composeRule, address)
                createData(composeRule, source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

                // We can assert that the schema has been written successfully here as the schema
                // is shown in the type browser.

                nodeWithTextExists(composeRule, Label.ATTRIBUTE.lowercase())
                nodeWithTextExists(composeRule, "commit-date")
                nodeWithTextExists(composeRule, "commit-hash")
            }
        }
    }

    @Test
    fun collapseTypes() {
        withTypeDB { address ->
            runBlocking {
                connectToTypeDB(composeRule, address)
                createData(composeRule, source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

                StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)
                delayAndRecompose(composeRule, Delays.NETWORK_IO)

                composeRule.onAllNodesWithText(Label.PROJECT).get(0).performClick()
                composeRule.onAllNodesWithText(Label.PROJECT).get(1).performClick()
                delayAndRecompose(composeRule)

                clickIcon(composeRule, Icon.Code.CHEVRONS_DOWN)
                delayAndRecompose(composeRule)

                nodeWithTextExists(composeRule, "commit-date")
            }
        }
    }

    @Test
    fun collapseThenExpandTypes() {
        withTypeDB { address ->
            runBlocking {
                connectToTypeDB(composeRule, address)
                createData(composeRule, source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

                StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)

                clickIcon(composeRule, Icon.Code.CHEVRONS_UP)
                delayAndRecompose(composeRule)

                nodeWithTextDoesNotExist(composeRule, "commit-date")

                clickIcon(composeRule, Icon.Code.CHEVRONS_DOWN)
                delayAndRecompose(composeRule)

                nodeWithTextExists(composeRule, "commit-date")
            }
        }
    }

    // This test is ignored as the export schema button doesn't open a new file during testing.
    @Ignore
    @Test
    fun exportSchema() {
        withTypeDB { address ->
            runBlocking {
                connectToTypeDB(composeRule, address)
                createData(composeRule, source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

                StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)
                delayAndRecompose(composeRule, Delays.NETWORK_IO)

                StudioState.schema.exportTypeSchema { schema ->
                    StudioState.project.current!!.reloadEntries()
                    StudioState.project.tryCreateUntitledFile()?.let { file ->
                        file.content(schema)
                        file.tryOpen()
                    }
                }
                composeRule.waitForIdle()

                nodeWithTextExists(composeRule, "define")
                nodeWithTextDoesNotExist(composeRule, "# This program is free software: you can redistribute it and/or modify")
            }
        }
    }
}