/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// We need to access the private function StudioState.driver.session.tryOpen, this allows us to.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.typedb.studio.test.integration

import androidx.compose.ui.test.onNodeWithText
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.service.Service
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.test.integration.common.StudioActions.Delays
import com.typedb.studio.test.integration.common.StudioActions.clickAllInstancesOfIcon
import com.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.typedb.studio.test.integration.common.StudioActions.openProject
import com.typedb.studio.test.integration.common.StudioActions.waitUntilAssertionPasses
import com.typedb.studio.test.integration.common.StudioActions.waitUntilTrue
import com.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB
import com.typedb.studio.test.integration.data.Paths.SampleGitHubData
import com.vaticle.typedb.driver.api.TypeDBSession
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TypeBrowserTest : IntegrationTest() {

    @Test
    fun interactiveSchemaWritesAutomaticallyDisplayed() {
        withTypeDB { typeDB ->
            runBlocking {
                val commitDateAttributeName = "commit-date"
                val commitHashAttributeName = "commit-hash"
                connectToTypeDB(composeRule, typeDB.address())
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SampleGitHubData.schemaFile)

                waitUntilAssertionPasses(composeRule) {
                    composeRule.onNodeWithText(Label.ATTRIBUTE.lowercase()).assertExists()
                    composeRule.onNodeWithText(commitDateAttributeName).assertExists()
                    composeRule.onNodeWithText(commitHashAttributeName).assertExists()
                }
            }
        }
    }

    @Test
    fun collapseTypes() {
        withTypeDB { typeDB ->
            runBlocking {
                val commitDateAttributeName = "commit-date"

                connectToTypeDB(composeRule, typeDB.address())
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SampleGitHubData.schemaFile)

                Service.driver.session.tryOpen(
                    database = testID,
                    TypeDBSession.Type.DATA
                )

                waitUntilTrue(composeRule) {
                    Service.driver.session.type == TypeDBSession.Type.DATA
                }

                clickAllInstancesOfIcon(composeRule, Icon.COLLAPSE)

                delayAndRecompose(composeRule, Delays.NETWORK_IO)
                composeRule.onNodeWithText(commitDateAttributeName).assertDoesNotExist()
            }
        }
    }

    @Test
    fun collapseThenExpandTypes() {
        withTypeDB { typeDB ->
            runBlocking {
                val commitDateAttributeName = "commit-date"

                connectToTypeDB(composeRule, typeDB.address())
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                createDatabase(composeRule, dbName = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SampleGitHubData.schemaFile)

                Service.driver.session.tryOpen(
                    database = testID,
                    TypeDBSession.Type.DATA
                )

                waitUntilTrue(composeRule) {
                    Service.driver.session.type == TypeDBSession.Type.DATA
                }

                clickAllInstancesOfIcon(composeRule, Icon.COLLAPSE)

                delayAndRecompose(composeRule, Delays.NETWORK_IO)
                composeRule.onNodeWithText(commitDateAttributeName).assertDoesNotExist()

                clickAllInstancesOfIcon(composeRule, Icon.EXPAND)

                waitUntilAssertionPasses(composeRule) {
                    composeRule.onNodeWithText(commitDateAttributeName).assertExists()
                }
            }
        }
    }
}
