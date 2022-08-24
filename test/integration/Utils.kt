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
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.vaticle.typedb.studio.test.integration

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
import com.vaticle.typedb.common.test.core.TypeDBCoreRunner
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

object Utils {
    val SAVE_ICON_STRING = Icon.Code.FLOPPY_DISK.unicode
    val PLUS_ICON_STRING = Icon.Code.PLUS.unicode
    val PLAY_ICON_STRING = Icon.Code.PLAY.unicode
    val CHECK_ICON_STRING = Icon.Code.CHECK.unicode
    val ROLLBACK_ICON_STRING = Icon.Code.ROTATE_LEFT.unicode
    val CHEVRON_UP_ICON_STRING = Icon.Code.CHEVRON_UP.unicode
    val DOUBLE_CHEVRON_DOWN_ICON_STRING = Icon.Code.CHEVRONS_DOWN.unicode
    val DOUBLE_CHEVRON_UP_ICON_STRING = Icon.Code.CHEVRONS_UP.unicode

    val SAMPLE_DATA_PATH = File("test/data/sample_file_structure").absolutePath
    val TQL_DATA_PATH = File("test/data").absolutePath

    const val QUERY_FILE_NAME = "query_string.tql"
    const val DATA_FILE_NAME = "data_string.tql"
    const val SCHEMA_FILE_NAME = "schema_string.tql"

    const val FAIL_CONNECT_TYPEDB = "Failed to connect to TypeDB."
    const val FAIL_CREATE_DATABASE = "Failed to create the database."
    const val FAIL_DATA_WRITE = "Failed to write the data."
    const val FAIL_SCHEMA_WRITE = "Failed to write the schema."

    fun runComposeRule(compose: ComposeContentTestRule, rule: suspend ComposeContentTestRule.() -> Unit) {
        runBlocking { compose.rule() }
    }

    fun studioTest(compose: ComposeContentTestRule, funcBody: suspend () -> Unit) {
        runComposeRule(compose) {
            setContent {
                Studio.MainWindowContent(WindowContext.Test(1000, 1000, 0, 0))
            }
            funcBody()
        }
}

    fun studioTestWithRunner(compose: ComposeContentTestRule, funcBody: suspend (String) -> Unit) {
        val typeDB = TypeDBCoreRunner()
        typeDB.start()
        val address = typeDB.address()
        runComposeRule(compose) {
            setContent {
                Studio.MainWindowContent(WindowContext.Test(1000, 1000, 0, 0))
            }
            funcBody(address)
        }
        typeDB.stop()
    }

    /// Wait `timeMillis` milliseconds, then wait for all recompositions to finish.
    suspend fun delayAndRecompose(composeRule: ComposeContentTestRule, timeMillis: Int = Delays.RECOMPOSE) {
        delay(timeMillis.toLong())
        composeRule.waitForIdle()
    }

    suspend fun waitForConditionAndRecompose(successCondition: () -> Boolean,
                                                     beforeRetry: () -> Unit, failMessage: String, context: ComposeContentTestRule) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!successCondition() && System.currentTimeMillis() < deadline) {
            beforeRetry()
            delayAndRecompose(context, 500)
        }
        if (!successCondition()) {
            fail(failMessage)
        }
    }

    fun cloneAndOpenProject(composeRule: ComposeContentTestRule, source: String, destination: String): Path {
        val absolute = File(File(destination).absolutePath)

        absolute.deleteRecursively()
        File(source).copyRecursively(overwrite = true, target = absolute)

        StudioState.project.tryOpenProject(absolute.toPath())

        composeRule.waitForIdle()
        return absolute.toPath()
    }

    suspend fun connectToTypeDB(composeRule: ComposeContentTestRule, address: String) {
        // This opens a dialog box (which we can't see through) so we assert that buttons with that text can be
        // clicked.
        composeRule.onAllNodesWithText(Label.CONNECT_TO_TYPEDB).assertAll(hasClickAction())

        StudioState.client.tryConnectToTypeDB(address) {}
        delayAndRecompose(composeRule, Delays.CONNECT_SERVER)

        waitForConditionAndRecompose({ StudioState.client.isConnected }, {}, FAIL_CONNECT_TYPEDB, composeRule)

        composeRule.onNodeWithText(address).assertExists()
    }

    suspend fun createDatabase(composeRule: ComposeContentTestRule, dbName: String) {
        composeRule.onAllNodesWithText(Label.SELECT_DATABASE).assertAll(hasClickAction())

        StudioState.client.tryDeleteDatabase(dbName)
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        StudioState.client.tryCreateDatabase(dbName) {}
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        StudioState.client.refreshDatabaseList()

        waitForConditionAndRecompose({ StudioState.client.databaseList.contains(dbName) },
            { StudioState.client.refreshDatabaseList() }, FAIL_CREATE_DATABASE, composeRule)
    }

    suspend fun writeSchemaInteractively(composeRule: ComposeContentTestRule, dbName: String, schemaFileName: String) {
        StudioState.notification.dismissAll()

        composeRule.onNodeWithText(PLUS_ICON_STRING).performClick()
        delayAndRecompose(composeRule)

        StudioState.client.session.tryOpen(dbName, TypeDBSession.Type.SCHEMA)
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        StudioState.client.tryUpdateTransactionType(TypeDBTransaction.Type.WRITE)
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        composeRule.onNodeWithText("schema").performClick()
        composeRule.onNodeWithText("write").performClick()

        StudioState.project.current!!.directory.entries.find { it.name == schemaFileName }!!.asFile().tryOpen()

        composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        composeRule.onNodeWithText(CHECK_ICON_STRING).performClick()
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        waitForConditionAndRecompose({ StudioState.notification.queue.last().code == "CNX10" }, {}, FAIL_SCHEMA_WRITE, composeRule)

        StudioState.client.session.close()
    }

    suspend fun writeDataInteractively(composeRule: ComposeContentTestRule, dbName: String, dataFileName: String) {
        StudioState.notification.dismissAll()
        delayAndRecompose(composeRule)

        StudioState.client.session.tryOpen(dbName, TypeDBSession.Type.DATA)
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        composeRule.onNodeWithText("data").performClick()
        composeRule.onNodeWithText("write").performClick()

        StudioState.project.current!!.directory.entries.find { it.name == dataFileName }!!.asFile().tryOpen()

        composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        composeRule.onNodeWithText(CHECK_ICON_STRING).performClick()
        delayAndRecompose(composeRule, Delays.NETWORK_IO)

        waitForConditionAndRecompose({ StudioState.notification.queue.last().code == "CNX10" }, {}, FAIL_DATA_WRITE, composeRule)

        StudioState.client.session.close()
    }

    suspend fun verifyDataWrite(composeRule: ComposeContentTestRule, address: String, dbName: String, queryFileName: String) {
        val queryString = fileNameToString(queryFileName)

        composeRule.onNodeWithText("infer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("read").performClick()
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

    private fun fileNameToString(fileName: String): String {
        return Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8).filter { line -> !line.startsWith('#') }
            .joinToString("")
    }
}

object Delays {
    const val RECOMPOSE = 500
    const val FILE_IO = 750
    const val NETWORK_IO = 1_500
    const val CONNECT_SERVER = 2_500
}
