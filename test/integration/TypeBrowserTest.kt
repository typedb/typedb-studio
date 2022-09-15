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
import com.vaticle.typedb.studio.test.integration.common.Paths.SCHEMA_FILE_NAME
import com.vaticle.typedb.studio.test.integration.common.Paths.TQL_DATA_PATH
import com.vaticle.typedb.studio.test.integration.common.Delays
import com.vaticle.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createData
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeNotExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickAllInstancesOfIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TypeBrowserTest: IntegrationTest() {

    @Test
    fun interactiveSchemaWritesAutomaticallyDisplayed() {
        withTypeDB { typeDB ->
            runBlocking {
                connectToTypeDB(composeRule, typeDB.address())
                createData(source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

                // We can assert that the schema has been written successfully here as the schema
                // is shown in the type browser.

                assertNodeExistsWithText(composeRule, text = Label.ATTRIBUTE.lowercase())
                assertNodeExistsWithText(composeRule, text = "commit-date")
                assertNodeExistsWithText(composeRule, text = "commit-hash")
            }
        }
    }

    @Test
    fun collapseTypes() {
        withTypeDB { typeDB ->
            runBlocking {
                connectToTypeDB(composeRule, typeDB.address())
                createData(source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

                StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)
                delayAndRecompose(composeRule, Delays.NETWORK_IO)

                clickAllInstancesOfIcon(composeRule, Icon.Code.CHEVRONS_UP)

                delayAndRecompose(composeRule)

                assertNodeNotExistsWithText(composeRule, text = "commit-date")
            }
        }
    }

    @Test
    fun collapseThenExpandTypes() {
        withTypeDB { typeDB ->
            runBlocking {
                connectToTypeDB(composeRule, typeDB.address())
                createData(source = TQL_DATA_PATH, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SCHEMA_FILE_NAME)

                StudioState.client.session.tryOpen(database = testID, TypeDBSession.Type.DATA)

                clickAllInstancesOfIcon(composeRule, Icon.Code.CHEVRONS_UP)
                delayAndRecompose(composeRule)

                assertNodeNotExistsWithText(composeRule, text = "commit-date")

                clickAllInstancesOfIcon(composeRule, Icon.Code.CHEVRONS_DOWN)
                delayAndRecompose(composeRule)

                assertNodeExistsWithText(composeRule, text = "commit-date")
            }
        }
    }
}