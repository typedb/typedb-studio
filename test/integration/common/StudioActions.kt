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

package com.vaticle.typedb.studio.test.integration.common

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Message
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

object StudioActions {
    private val LOGGER = KotlinLogging.logger {}

    suspend fun clickIcon(composeRule: ComposeContentTestRule, icon: Icon) {
        clickText(composeRule, icon.unicode)
    }

    suspend fun clickText(composeRule: ComposeContentTestRule, text: String) {
        waitUntilNodeWithTextIsClickable(composeRule, text)
        composeRule.onNodeWithText(text).performClick()
        delayAndRecompose(composeRule)
    }

    suspend fun clickAllInstancesOfText(composeRule: ComposeContentTestRule, text: String) {
        val length = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size
        for (i in 0 until length) {
            composeRule.onAllNodesWithText(text)[i].performClick()
        }
        delayAndRecompose(composeRule)
    }

    suspend fun clickAllInstancesOfIcon(composeRule: ComposeContentTestRule, icon: Icon) {
        clickAllInstancesOfText(composeRule, icon.unicode)
    }

    suspend fun waitUntilNodeWithTextIsClickable(composeRule: ComposeContentTestRule, text: String) {
        waitUntilAssertionPasses(composeRule) {
            composeRule.onNodeWithText(text).assertHasClickAction()
        }
    }

    suspend fun waitUntilNodeWithTextExists(composeRule: ComposeContentTestRule, text: String) {
        waitUntilAssertionPasses(composeRule) {
            composeRule.onNodeWithText(text).assertExists()
        }
    }

    suspend fun waitUntilConditionIsTrue(composeRule: ComposeContentTestRule, condition: () -> Boolean) {
        composeRule.waitUntil(Delays.WAIT_TIMEOUT) {
            runBlocking {
                delayAndRecompose(composeRule)
            }
            try {
                return@waitUntil condition()
            } catch (e: Exception) {
                return@waitUntil false
            }
        }
    }

    suspend fun waitUntilAssertionPasses(composeRule: ComposeContentTestRule, assertion: () -> Any) {
        composeRule.waitUntil(Delays.WAIT_TIMEOUT) {
            runBlocking {
                delayAndRecompose(composeRule)
            }
            try {
                assertion()
                return@waitUntil true
            } catch (e: Exception) {
                return@waitUntil false
            }
        }
    }

    fun copyFolder(source: String, destination: String): Path {
        val absoluteDestination = File(File(destination).absolutePath)

        absoluteDestination.deleteRecursively()
        File(source).copyRecursively(overwrite = true, target = absoluteDestination)

        return absoluteDestination.toPath()
    }

    suspend fun delayAndRecompose(composeRule: ComposeContentTestRule, timeMillis: Int = Delays.RECOMPOSE) {
        delay(timeMillis.toLong())
        composeRule.awaitIdle()
    }

    suspend fun openProject(composeRule: ComposeContentTestRule, projectDirectory: String) {
        val projectPath = File(File(projectDirectory).absolutePath).toPath()
        Service.project.tryOpenProject(projectPath)

        waitUntilConditionIsTrue(composeRule) {
            Service.project.current != null
        }
    }

    suspend fun connectToTypeDB(composeRule: ComposeContentTestRule, address: String) {
        // This opens a dialog box (which we can't see through) so we assert that buttons with that text can be
        // clicked.
        composeRule.onAllNodesWithText(Label.CONNECT_TO_TYPEDB).assertAll(hasClickAction())

        Service.client.tryConnectToTypeDBAsync(address) {}

        waitUntilConditionIsTrue(composeRule) {
            Service.client.isConnected
        }
    }

    suspend fun createDatabase(composeRule: ComposeContentTestRule, dbName: String) {
        composeRule.onAllNodesWithText(Label.SELECT_DATABASE).assertAll(hasClickAction())

        Service.client.tryCreateDatabase(dbName) {}

        waitUntilConditionIsTrue(composeRule) {
            Service.client.refreshDatabaseList()
            Service.client.databaseList.contains(dbName)
        }
    }

    suspend fun waitForFileToBeFullyLoaded(composeRule: ComposeContentTestRule) {
        delayAndRecompose(composeRule, Delays.FILE_IO)
    }

    suspend fun writeSchemaInteractively(composeRule: ComposeContentTestRule, dbName: String, schemaFileName: String) {
        Service.notification.dismissAll()

        clickIcon(composeRule, Icon.ADD)

        Service.client.session.tryOpen(dbName, TypeDBSession.Type.SCHEMA)
        Service.client.tryUpdateTransactionType(TypeDBTransaction.Type.WRITE)

        clickText(composeRule, Label.SCHEMA.lowercase())
        clickText(composeRule, Label.WRITE.lowercase())

        waitUntilConditionIsTrue(composeRule) {
            Service.client.session.type == TypeDBSession.Type.SCHEMA &&
                    Service.client.session.transaction.type == TypeDBTransaction.Type.WRITE
        }

        waitUntilConditionIsTrue(composeRule) {
            Service.project.current!!.directory.entries.find { it.name == schemaFileName }!!.asFile().tryOpen()
        }
        waitForFileToBeFullyLoaded(composeRule)

        clickIcon(composeRule, Icon.RUN)
        waitUntilConditionIsTrue(composeRule) {
            !Service.client.session.transaction.hasRunningQuery
        }

        clickIcon(composeRule, Icon.COMMIT)
        waitUntilConditionIsTrue(composeRule) {
            Service.notification.queue.last().code == Message.Connection.TRANSACTION_COMMIT_SUCCESSFULLY.code()
        }
    }

    suspend fun writeDataInteractively(composeRule: ComposeContentTestRule, dbName: String, dataFileName: String) {
        Service.notification.dismissAll()

        clickIcon(composeRule, Icon.ADD)

        Service.client.session.tryOpen(dbName, TypeDBSession.Type.DATA)
        Service.client.tryUpdateTransactionType(TypeDBTransaction.Type.WRITE)

        clickText(composeRule, Label.DATA.lowercase())
        clickText(composeRule, Label.WRITE.lowercase())

        waitUntilConditionIsTrue(composeRule) {
            Service.client.session.type == TypeDBSession.Type.DATA &&
                    Service.client.session.transaction.type == TypeDBTransaction.Type.WRITE
        }

        waitUntilConditionIsTrue(composeRule) {
            Service.project.current!!.directory.entries.find { it.name == dataFileName }!!.asFile().tryOpen()
        }
        waitForFileToBeFullyLoaded(composeRule)

        clickIcon(composeRule, Icon.RUN)
        waitUntilConditionIsTrue(composeRule) {
            !Service.client.session.transaction.hasRunningQuery
        }

        clickIcon(composeRule, Icon.COMMIT)
        waitUntilConditionIsTrue(composeRule) {
            Service.notification.queue.last().code == Message.Connection.TRANSACTION_COMMIT_SUCCESSFULLY.code()
        }
    }

    fun verifyDataWrite(
        address: String, dbName: String, queryFileName: String) {
        val queryString = readQueryFileToString(queryFileName)

        TypeDB.coreClient(address).use { client ->
            client.session(dbName, TypeDBSession.Type.DATA, TypeDBOptions.core().infer(true)).use { session ->
                session.transaction(TypeDBTransaction.Type.READ).use { transaction ->
                    val results = ArrayList<String>()
                    val query = TypeQL.parseQuery<TypeQLMatch>(queryString)
                    transaction.query().match(query).forEach { result ->
                        results.add(
                            result.get("user-name").asAttribute().value.toString()
                        )
                    }
                    assertEquals(2, results.size)
                    assertTrue(results.contains("jmsfltchr"))
                    assertTrue(results.contains("krishnangovindraj"))
                }
            }
        }
    }

    private fun readQueryFileToString(queryFileName: String): String {
        return Files.readAllLines(Paths.get(queryFileName), StandardCharsets.UTF_8)
            .joinToString("\n")
    }

    object Delays {
        const val RECOMPOSE = 500
        const val FILE_IO = 750
        const val NETWORK_IO = 1_500
        const val WAIT_TIMEOUT: Long = 30_000
    }
}