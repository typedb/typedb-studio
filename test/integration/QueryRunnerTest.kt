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
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilConditionIsTrue
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeDataInteractively
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB
import com.vaticle.typedb.studio.test.integration.data.Paths.SampleGitHubData
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

                Service.client.session.tryOpen(
                    database = testID,
                    TypeDBSession.Type.DATA
                )

                waitUntilConditionIsTrue(composeRule) {
                    Service.client.session.type == TypeDBSession.Type.DATA
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

                val sessionType = Service.client.session.type
                val transaction = Service.client.session.transaction.transaction!!
                val transactionType = transaction.type()
                val transactionIsInfer = transaction.options().infer().get()
                val transactionIsSnapshot =
                    Service.client.session.transaction.snapshot.value
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