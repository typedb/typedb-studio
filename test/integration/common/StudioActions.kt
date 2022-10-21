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

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertAll
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
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.delay
import mu.KotlinLogging

object StudioActions {
    private val LOGGER = KotlinLogging.logger {}

    suspend fun clickIcon(composeRule: ComposeContentTestRule, icon: Icon, delayMillis: Int = Delays.RECOMPOSE) {
        clickText(composeRule, icon.unicode, delayMillis)
    }

    suspend fun clickText(composeRule: ComposeContentTestRule, text: String, delayMillis: Int = Delays.RECOMPOSE) {
        composeRule.onNodeWithText(text).performClick()
        delayAndRecompose(composeRule, delayMillis)
    }

    fun clickAllInstancesOfText(composeRule: ComposeContentTestRule, text: String) {
        val length = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size
        for (i in 0 until length) {
            composeRule.onAllNodesWithText(text)[i].performClick()
        }
    }

    fun clickAllInstancesOfIcon(composeRule: ComposeContentTestRule, icon: Icon) {
        clickAllInstancesOfText(composeRule, icon.unicode)
    }

    fun assertNodeExistsWithText(composeRule: ComposeContentTestRule, text: String): SemanticsNodeInteraction {
        return composeRule.onNodeWithText(text).assertExists()
    }

    fun assertNodeNotExistsWithText(composeRule: ComposeContentTestRule, text: String) {
        return composeRule.onNodeWithText(text).assertDoesNotExist()
    }

    fun copyFolder(source: String, destination: String): Path {
        val absoluteDestination = File(File(destination).absolutePath)

        absoluteDestination.deleteRecursively()
        File(source).copyRecursively(overwrite = true, target = absoluteDestination)

        return absoluteDestination.toPath()
    }

    /// Wait `timeMillis` milliseconds, then wait for all recompositions to finish.
    suspend fun delayAndRecompose(composeRule: ComposeContentTestRule, timeMillis: Int = Delays.RECOMPOSE) {
        delay(timeMillis.toLong())
        composeRule.waitForIdle()
    }

    suspend fun waitForConditionAndRecompose(
        context: ComposeContentTestRule,
        failMessage: String,
        beforeRetry: (() -> Unit) = {},
        successCondition: () -> Boolean
    ) {
        var success = false
        var deadline = 10_000
        while (!success && deadline > 0) {
            try {
                if (successCondition()) {
                    success = true
                } else {
                    beforeRetry()
                    delayAndRecompose(context, 500)
                    deadline -= 500
                }
            } catch (e: Exception) {
                LOGGER.error(e.stackTraceToString())
            }
        }

        if (!successCondition()) {
            fail(failMessage)
        }
    }

    suspend fun openProject(composeRule: ComposeContentTestRule, projectDirectory: String) {
        val projectPath = File(File(projectDirectory).absolutePath).toPath()
        StudioState.project.tryOpenProject(projectPath)

        delayAndRecompose(composeRule)
    }

    suspend fun connectToTypeDB(composeRule: ComposeContentTestRule, address: String) {
        // This opens a dialog box (which we can't see through) so we assert that buttons with that text can be
        // clicked.
        composeRule.onAllNodesWithText(Label.CONNECT_TO_TYPEDB).assertAll(hasClickAction())

        StudioState.client.tryConnectToTypeDBAsync(address) {}
        delayAndRecompose(composeRule, Delays.CONNECT_SERVER)

        waitForConditionAndRecompose(composeRule, Errors.CONNECT_TYPEDB_FAILED) { StudioState.client.isConnected }

        assertNodeExistsWithText(composeRule, text = address)
    }

    suspend fun createDatabase(composeRule: ComposeContentTestRule, dbName: String) {
        composeRule.onAllNodesWithText(Label.SELECT_DATABASE).assertAll(hasClickAction())

        StudioState.client.tryDeleteDatabase(dbName)
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        StudioState.client.tryCreateDatabase(dbName) {}
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        StudioState.client.refreshDatabaseList()

        waitForConditionAndRecompose(
            context = composeRule,
            failMessage = Errors.CREATE_DATABASE_FAILED,
            beforeRetry = { StudioState.client.refreshDatabaseList() }
        ) { StudioState.client.databaseList.contains(dbName) }
    }

    suspend fun writeSchemaInteractively(composeRule: ComposeContentTestRule, dbName: String, schemaFileName: String) {
        StudioState.notification.dismissAll()

        clickIcon(composeRule, Icon.ADD)

        StudioState.client.session.tryOpen(dbName, TypeDBSession.Type.SCHEMA)
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        StudioState.client.tryUpdateTransactionType(TypeDBTransaction.Type.WRITE)
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        clickText(composeRule, Label.SCHEMA.lowercase())
        clickText(composeRule, Label.WRITE.lowercase())

        StudioState.project.current!!.directory.entries.find { it.name == schemaFileName }!!.asFile().tryOpen()

        clickIcon(composeRule, Icon.RUN, delayMillis = Delays.NETWORK_IO)

        clickIcon(composeRule, Icon.COMMIT, delayMillis = Delays.NETWORK_IO)

        waitForConditionAndRecompose(composeRule, Errors.SCHEMA_WRITE_FAILED) {
            StudioState.notification.queue.last().code == Message.Connection.TRANSACTION_COMMIT_SUCCESSFULLY.code()
        }
    }

    suspend fun writeDataInteractively(composeRule: ComposeContentTestRule, dbName: String, dataFileName: String) {
        StudioState.notification.dismissAll()
        delayAndRecompose(composeRule)

        StudioState.client.session.tryOpen(dbName, TypeDBSession.Type.DATA)
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        clickText(composeRule, Label.DATA.lowercase())
        clickText(composeRule, Label.WRITE.lowercase())

        StudioState.project.current!!.directory.entries.find { it.name == dataFileName }!!.asFile().tryOpen()

        clickIcon(composeRule, Icon.RUN, delayMillis = Delays.NETWORK_IO)

        clickIcon(composeRule, Icon.COMMIT, delayMillis = Delays.NETWORK_IO)

        waitForConditionAndRecompose(composeRule, Errors.DATA_WRITE_FAILED) {
            StudioState.notification.queue.last().code == Message.Connection.TRANSACTION_COMMIT_SUCCESSFULLY.code()
        }
    }

    suspend fun verifyDataWrite(composeRule: ComposeContentTestRule, address: String, dbName: String, queryFileName: String) {
        val queryString = readQueryFileToString(queryFileName)

        clickText(composeRule, Label.INFER.lowercase())
        clickText(composeRule, Label.READ.lowercase())

        delayAndRecompose(composeRule)

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

    object Errors {
        private const val CONNECT_TYPEDB_FAILED = "Failed to connect to TypeDB."
        private const val CREATE_DATABASE_FAILED = "Failed to create the database."
        private const val DATA_WRITE_FAILED = "Failed to write the data."
        private const val SCHEMA_WRITE_FAILED = "Failed to write the schema."
    }

    object Delays {
        const val RECOMPOSE = 500
        const val FILE_IO = 750
        const val NETWORK_IO = 1_500
        const val CONNECT_SERVER = 2_500
    }
}