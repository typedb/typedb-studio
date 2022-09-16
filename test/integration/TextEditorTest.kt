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

import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.test.integration.common.Paths.DATA_FILE_NAME
import com.vaticle.typedb.studio.test.integration.common.Paths.QUERY_FILE_NAME
import com.vaticle.typedb.studio.test.integration.common.Paths.SAMPLE_DATA_PATH
import com.vaticle.typedb.studio.test.integration.common.Paths.SCHEMA_FILE_NAME
import com.vaticle.typedb.studio.test.integration.common.Paths.TQL_DATA_PATH
import com.vaticle.typedb.studio.test.integration.common.StudioActions.Delays
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeNotExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createData
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.verifyDataWrite
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeDataInteractively
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB
import java.io.File
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TextEditorTest: IntegrationTest() {

    @Test
    fun makeAFileAndSaveIt() {
        runBlocking {
            val path = createData(source = SAMPLE_DATA_PATH, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            clickIcon(composeRule, Icon.Code.PLUS)
            delayAndRecompose(composeRule)

            // This sets saveFileDialog.file!! to the current file, so even though we can't see the window it is useful.
            clickIcon(composeRule, Icon.Code.FLOPPY_DISK)
            val file = File("$path/Untitled1.tql")
            StudioState.project.saveFileDialog.file!!.trySave(file.toPath(), true)
            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule, Delays.FILE_IO)

            assertTrue(file.exists())
        }
    }

    @Test
    fun schemaWriteAndCommit() {
        withTypeDB { typeDB ->
            runBlocking {
                createData(source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

                StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)
                delayAndRecompose(composeRule, Delays.NETWORK_IO)

                StudioState.schema.reloadEntries()

                // We can assert that the schema has been written successfully here as the schema
                // is shown in the type browser.
                assertNodeExistsWithText(composeRule, text = "commit-date")
            }
        }
    }

    @Test
    fun dataWriteAndCommit() {
        withTypeDB {typeDB ->  
            runBlocking {
                createData(source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)
                writeDataInteractively(composeRule, dbName = testID, DATA_FILE_NAME)
                verifyDataWrite(composeRule, typeDB.address(), dbName = testID, "$testID/${QUERY_FILE_NAME}")
            }
        }
    }

    @Test
    fun schemaWriteAndRollback() {
        withTypeDB { typeDB ->
            runBlocking {
                createData(source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)

                StudioState.client.session.tryOpen(testID, TypeDBSession.Type.SCHEMA)
                delayAndRecompose(composeRule, Delays.NETWORK_IO)

                clickText(composeRule, Label.SCHEMA.lowercase())
                clickText(composeRule, Label.WRITE.lowercase())

                StudioState.project.current!!.directory.entries.find { it.name == SCHEMA_FILE_NAME }!!.asFile().tryOpen()

                clickIcon(composeRule, Icon.Code.PLAY)
                delayAndRecompose(composeRule, Delays.NETWORK_IO)
                clickIcon(composeRule, Icon.Code.ROTATE)
                delayAndRecompose(composeRule, Delays.NETWORK_IO)

                assertNodeNotExistsWithText(composeRule, text = "repo-id")
            }
        }
    }
}