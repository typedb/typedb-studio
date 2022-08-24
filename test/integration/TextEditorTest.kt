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

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.test.integration.Utils.studioTest
import com.vaticle.typedb.studio.test.integration.Utils.studioTestWithRunner
import com.vaticle.typedb.studio.test.integration.Utils.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.Utils.createDatabase
import com.vaticle.typedb.studio.test.integration.Utils.cloneAndOpenProject
import com.vaticle.typedb.studio.test.integration.Utils.waitAndRecompose
import com.vaticle.typedb.studio.test.integration.Utils.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.Utils.writeDataInteractively
import com.vaticle.typedb.studio.test.integration.Utils.verifyDataWrite
import com.vaticle.typedb.studio.test.integration.Utils.SCHEMA_FILE_NAME
import com.vaticle.typedb.studio.test.integration.Utils.DATA_FILE_NAME
import com.vaticle.typedb.studio.test.integration.Utils.QUERY_FILE_NAME
import com.vaticle.typedb.studio.test.integration.Utils.TQL_DATA_PATH
import com.vaticle.typedb.studio.test.integration.Utils.SAVE_ICON_STRING
import com.vaticle.typedb.studio.test.integration.Utils.PLAY_ICON_STRING
import com.vaticle.typedb.studio.test.integration.Utils.ROLLBACK_ICON_STRING
import com.vaticle.typedb.studio.test.integration.Utils.PLUS_ICON_STRING
import com.vaticle.typedb.studio.test.integration.Utils.SAMPLE_DATA_PATH
import com.vaticle.typedb.studio.test.integration.Utils.CHEVRON_UP_ICON_STRING
import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

class TextEditorTest: IntegrationTest() {

    @Test
    fun makeAFileAndSaveIt() {
        studioTest(composeRule) {
        // We have to open a project to enable the '+' to create a new file.
            val path = cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            composeRule.onNodeWithText(PLUS_ICON_STRING).performClick()
            waitAndRecompose(composeRule, Delays.RECOMPOSE)

            // This sets saveFileDialog.file!! to the current file, so even though we can't see the window it is useful.
            composeRule.onNodeWithText(SAVE_ICON_STRING).performClick()
            val filePath = File("$path/Untitled1.tql").toPath()
            StudioState.project.saveFileDialog.file!!.trySave(filePath, true)
            StudioState.project.current!!.reloadEntries()
            waitAndRecompose(composeRule, Delays.FILE_IO)

            assertTrue(File("$path/Untitled1.tql").exists())
        }
    }

    @Test
    fun schemaWriteAndCommit() {
        studioTestWithRunner(composeRule) { address ->
        // We have to open a project to enable the '+' to create a new file.
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            connectToTypeDB(composeRule, address)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

            StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)

            composeRule.onNodeWithText(CHEVRON_UP_ICON_STRING).performClick()
            waitAndRecompose(composeRule, Delays.RECOMPOSE)

            // We can assert that the schema has been written successfully here as the schema
            // is shown in the type browser.
            composeRule.onNodeWithText("commit-date").assertExists()

            StudioState.client.session.close()
        }
    }

    @Test
    fun dataWriteAndCommit() {
        studioTestWithRunner(composeRule) { address ->
        cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            connectToTypeDB(composeRule, address)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)
            writeDataInteractively(composeRule, dbName = testID, DATA_FILE_NAME)
            verifyDataWrite(composeRule, address, dbName = testID, "$testID/${QUERY_FILE_NAME}")
        }
    }

    @Test
    fun schemaWriteAndRollback() {
        studioTestWithRunner(composeRule) { address ->
            cloneAndOpenProject(composeRule, source = TQL_DATA_PATH, destination = testID)
            connectToTypeDB(composeRule, address)
            createDatabase(composeRule, dbName = testID)

            StudioState.client.session.tryOpen(testID, TypeDBSession.Type.SCHEMA)

            waitAndRecompose(composeRule, Delays.NETWORK_IO)

            composeRule.onNodeWithText("schema").performClick()
            composeRule.onNodeWithText("write").performClick()

            StudioState.project.current!!.directory.entries.find { it.name == SCHEMA_FILE_NAME }!!.asFile().tryOpen()

            composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
            waitAndRecompose(composeRule, Delays.NETWORK_IO)
            composeRule.onNodeWithText(ROLLBACK_ICON_STRING).performClick()
            waitAndRecompose(composeRule, Delays.NETWORK_IO)

            composeRule.onNodeWithText("repo-id").assertDoesNotExist()

            StudioState.client.session.close()
        }
    }
}