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
import com.vaticle.typedb.studio.test.integration.Utils.wait
import com.vaticle.typedb.studio.test.integration.Utils.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.Utils.writeDataInteractively
import com.vaticle.typedb.studio.test.integration.Utils.verifyDataWrite
import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

class TextEditorTest: IntegrationTest() {

    @Test
    fun makeAFileAndSaveIt() {
        studioTest(composeRule) {
        // We have to open a project to enable the '+' to create a new file.
            val path = cloneAndOpenProject(composeRule, source = Utils.SAMPLE_DATA_PATH, destination = testID)

            composeRule.onNodeWithText(Utils.PLUS_ICON_STRING).performClick()
            wait(composeRule, 500)

            // This sets saveFileDialog.file!! to the current file, so even though we can't see the window it is useful.
            composeRule.onNodeWithText(Utils.SAVE_ICON_STRING).performClick()
            val filePath = File("$path/Untitled1.tql").toPath()
            StudioState.project.saveFileDialog.file!!.trySave(filePath, true)
            StudioState.project.current!!.reloadEntries()
            wait(composeRule, 500)

            assertTrue(File("$path/Untitled1.tql").exists())
        }
    }

    @Test
    fun schemaWriteAndCommit() {
        println("Starting schemaWriteAndCommit.")
        studioTestWithRunner(composeRule) { address ->
        // We have to open a project to enable the '+' to create a new file.
            cloneAndOpenProject(composeRule, source = Utils.TQL_DATA_PATH, destination = testID)
            connectToTypeDB(composeRule, address)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, Utils.SCHEMA_FILE_NAME)

            StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)

            composeRule.onNodeWithText(Utils.CHEVRON_UP_ICON_STRING).performClick()
            wait(composeRule, 500)

            // We can assert that the schema has been written successfully here as the schema
            // is shown in the type browser.
            composeRule.onNodeWithText("commit-date").assertExists()

            StudioState.client.session.close()
        }
        println("Finishing schemaWriteAndCommit.")
    }

    @Test
    fun dataWriteAndCommit() {
        println("Starting dataWriteAndCommit.")
        studioTestWithRunner(composeRule) { address ->
        cloneAndOpenProject(composeRule, source = Utils.TQL_DATA_PATH, destination = testID)
            connectToTypeDB(composeRule, address)
            createDatabase(composeRule, dbName = testID)
            writeSchemaInteractively(composeRule, dbName = testID, Utils.SCHEMA_FILE_NAME)
            writeDataInteractively(composeRule, dbName = testID, Utils.DATA_FILE_NAME)
            verifyDataWrite(composeRule, address, dbName = testID, "$testID/${Utils.QUERY_FILE_NAME}")
        }
        println("Finishing dataWriteAndCommit.")
    }

    @Test
    fun schemaWriteAndRollback() {
        println("Starting schemaWriteAndRollback.")
        studioTestWithRunner(composeRule) { address ->
            cloneAndOpenProject(composeRule, source = Utils.TQL_DATA_PATH, destination = testID)
            connectToTypeDB(composeRule, address)
            createDatabase(composeRule, dbName = testID)

            StudioState.client.session.tryOpen(testID, TypeDBSession.Type.SCHEMA)

            wait(composeRule, 500)

            composeRule.onNodeWithText("schema").performClick()
            composeRule.onNodeWithText("write").performClick()

            StudioState.project.current!!.directory.entries.find { it.name == Utils.SCHEMA_FILE_NAME }!!.asFile().tryOpen()

            composeRule.onNodeWithText(Utils.PLAY_ICON_STRING).performClick()
            wait(composeRule, 500)
            composeRule.onNodeWithText(Utils.ROLLBACK_ICON_STRING).performClick()
            wait(composeRule, 500)

            composeRule.onNodeWithText("repo-id").assertDoesNotExist()

            StudioState.client.session.close()
        }
        println("Finishing schemaWriteAndRollback.")
    }
}