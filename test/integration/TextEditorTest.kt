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

// We need to access the private function StudioState.driver.session.tryOpen, this allows us to.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.vaticle.typedb.studio.test.integration

import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Message
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openSchemaWriteTransaction
import com.vaticle.typedb.studio.test.integration.common.StudioActions.verifyDataWrite
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilAssertionPasses
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeDataInteractively
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB
import com.vaticle.typedb.studio.test.integration.data.Paths.SampleFileStructure
import com.vaticle.typedb.studio.test.integration.data.Paths.SampleGitHubData
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TextEditorTest : IntegrationTest() {

    @Test
    fun makeAFileAndSaveIt() {
        runBlocking {
            val defaultUntitledFileName = "Untitled1.tql"
            val path = copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            clickIcon(composeRule, Icon.ADD)

            // This sets saveFileDialog.file!! to the current file, so even though we can't see the window it is useful.
            clickIcon(composeRule, Icon.SAVE)
            val file = File("$path/$defaultUntitledFileName")
            Service.project.saveFileDialog.file!!.trySave(file.toPath(), true)
            Service.project.current!!.reloadEntries()

            waitUntilAssertionPasses(composeRule) {
                assert(file.exists())
            }
        }
    }

    @Test
    fun schemaWriteAndCommit() {
        withTypeDB { typeDB ->
            runBlocking {
                val commitDateAttributeName = "commit-date"
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SampleGitHubData.schemaFile)

                waitUntilAssertionPasses(composeRule) {
                    assert(Service.driver.session.typeSchema()!!.contains(commitDateAttributeName))
                }

                waitUntilAssertionPasses(composeRule) {
                    assertEquals(
                        Service.notification.queue.last().code,
                        Message.Connection.TRANSACTION_COMMIT_SUCCESSFULLY.code()
                    )
                }
            }
        }
    }

    @Test
    fun dataWriteAndCommit() {
        withTypeDB { typeDB ->
            runBlocking {
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SampleGitHubData.schemaFile)
                writeDataInteractively(composeRule, dbName = testID, SampleGitHubData.dataFile)
                verifyDataWrite(
                    typeDB.address(),
                    dbName = testID,
                    "$testID/${SampleGitHubData.collaboratorsQueryFile}"
                )
            }
        }
    }

    @Test
    fun schemaWriteAndRollback() {
        withTypeDB { typeDB ->
            runBlocking {
                Service.notification.dismissAll()

                val commitDateAttributeName = "commit-date"
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)

                openSchemaWriteTransaction(testID, composeRule)

                Service.project.current!!.directory.entries.find { it.name == SampleGitHubData.schemaFile }!!
                    .asFile().tryOpen()

                clickIcon(composeRule, Icon.RUN)

                waitUntilAssertionPasses(composeRule) {
                    assertNotEquals(
                        Service.driver.session.transaction.transaction!!.concepts()
                            .getAttributeType(commitDateAttributeName),
                        null
                    )
                }

                clickIcon(composeRule, Icon.ROLLBACK)

                waitUntilAssertionPasses(composeRule) {
                    assertEquals(
                        Service.driver.session.transaction.transaction!!.concepts()
                            .getAttributeType(commitDateAttributeName),
                        null
                    )
                }

                waitUntilAssertionPasses(composeRule) {
                    assertEquals(Service.notification.queue.last().code, Message.Connection.TRANSACTION_ROLLBACK.code())
                }
            }
        }
    }
}
