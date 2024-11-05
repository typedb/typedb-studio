/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
// We need to access the private function StudioState.driver.session.tryOpen, this allows us to.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.typedb.studio.test.integration

import com.typedb.studio.service.Service
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.test.integration.common.StudioActions.clickText
import com.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.typedb.studio.test.integration.common.StudioActions.openProject
import com.typedb.studio.test.integration.common.StudioActions.waitUntilTrue
import com.typedb.studio.test.integration.common.StudioActions.writeDataInteractively
import com.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB
import com.typedb.studio.test.integration.data.Paths.SampleGitHubData
import com.vaticle.typedb.driver.api.TypeDBSession
import com.vaticle.typedb.driver.api.TypeDBTransaction
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class QueryRunnerTest : IntegrationTest() {

    @Test
    fun toolbarTogglesSetQueryOptionsCorrectly() {
        withTypeDB { typeDB ->
            runBlocking {
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SampleGitHubData.schemaFile)
                writeDataInteractively(composeRule, dbName = testID, SampleGitHubData.dataFile)

                Service.driver.session.tryOpen(
                    database = testID,
                    TypeDBSession.Type.DATA
                )

                waitUntilTrue(composeRule) {
                    Service.driver.session.type == TypeDBSession.Type.DATA
                }

                Service.project.current!!.directory.entries.find {
                    it.name == SampleGitHubData.collaboratorsQueryFile
                }!!.asFile().tryOpen()

                clickText(composeRule, Label.DATA.lowercase())
                clickText(composeRule, Label.READ.lowercase())
                clickText(composeRule, Label.SNAPSHOT.lowercase())
                clickText(composeRule, Label.INFER.lowercase())

                Service.pages.active?.let {
                    if (it.isRunnable) it.asRunnable().mayOpenAndRun()
                }

                val sessionType = Service.driver.session.type
                val transaction = Service.driver.session.transaction.transaction!!
                val transactionType = transaction.type()
                val transactionIsInfer = transaction.options().infer().get()
                val transactionIsSnapshot =
                    Service.driver.session.transaction.snapshot.value
                val transactionIsNotExplain = !transaction.options().explain().get()

                assertEquals(sessionType, TypeDBSession.Type.DATA)
                assertEquals(transactionType, TypeDBTransaction.Type.READ)
                assert(transactionIsInfer)
                assert(transactionIsSnapshot)
                assert(transactionIsNotExplain)
            }
        }
    }
}
