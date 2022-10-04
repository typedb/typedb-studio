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
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.test.integration.common.StudioActions
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickText
import com.vaticle.typedb.studio.test.integration.data.Paths.SampleGitHubData
import com.vaticle.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeDataInteractively
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB

import kotlinx.coroutines.runBlocking
import org.junit.Test

class QueryRunnerTest: IntegrationTest() {

    @Test
    fun ExplainNotInferRegression() {
        withTypeDB { typeDB ->
            runBlocking {
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SampleGitHubData.schemaFile)
                writeDataInteractively(composeRule, dbName = testID, SampleGitHubData.dataFile)

                StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)
                StudioActions.delayAndRecompose(composeRule, StudioActions.Delays.NETWORK_IO)

                StudioState.project.current!!.directory.entries.find {
                    it.name == SampleGitHubData.collaboratorsQueryFile
                }!!.asFile().tryOpen()

                clickText(composeRule, Label.DATA.lowercase())
                clickText(composeRule, Label.READ.lowercase())
                clickText(composeRule, Label.INFER.lowercase())

                clickIcon(composeRule, Icon.RUN, delayMillis = 0)

                val priorTransactionInfer = StudioState.client.session.transaction.transaction!!.options().infer().get()
                val priorTransactionExplain = StudioState.client.session.transaction.transaction!!.options().explain().get()

                assert(priorTransactionInfer && !priorTransactionExplain)
            }
        }
    }
}