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

    fun runComposeRule(compose: ComposeContentTestRule, rule: suspend ComposeContentTestRule.() -> Unit) {
        runBlocking { compose.rule() }
    }

    fun studioTest(compose: ComposeContentTestRule, funcBody: suspend () -> Unit) {
        runComposeRule(compose) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
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
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            funcBody(address)
        }
        typeDB.stop()
    }

    fun fileNameToString(fileName: String): String {
        return Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8).filter { line -> !line.startsWith('#') }
            .joinToString("")
    }

    fun cloneAndOpenProject(composeRule: ComposeContentTestRule, source: String, destination: String): Path {
        val absolute = File(File(destination).absolutePath)

        absolute.deleteRecursively()
        File(source).copyRecursively(overwrite = true, target = absolute)

        StudioState.project.tryOpenProject(absolute.toPath())

        composeRule.waitForIdle()
        return absolute.toPath()
    }

    /// Wait `timeMillis` milliseconds, then wait for all recompositions to finish.
    suspend fun wait(composeRule: ComposeContentTestRule, timeMillis: Int) {
        delay(timeMillis.toLong())
        composeRule.waitForIdle()
    }

    suspend fun connectToTypeDB(composeRule: ComposeContentTestRule, address: String) {
        // This opens a dialog box (which we can't see through) so we assert that buttons with that text can be
        // clicked.
        composeRule.onAllNodesWithText(Label.CONNECT_TO_TYPEDB).assertAll(hasClickAction())

        StudioState.client.tryConnectToTypeDB(address) {}

        wait(composeRule, 2_500)
        assertTrue(StudioState.client.isConnected)

        composeRule.onNodeWithText(address).assertExists()
    }

    suspend fun createDatabase(composeRule: ComposeContentTestRule, dbName: String) {
        composeRule.onAllNodesWithText(Label.SELECT_DATABASE).assertAll(hasClickAction())

        StudioState.client.tryDeleteDatabase(dbName)
        wait(composeRule, 750)

        StudioState.client.tryCreateDatabase(dbName) {}
        wait(composeRule, 750)
    }

    suspend fun writeSchemaInteractively(composeRule: ComposeContentTestRule, dbName: String, schemaFileName: String) {
        composeRule.onNodeWithText(PLUS_ICON_STRING).performClick()
        wait(composeRule, 750)

        StudioState.client.session.tryOpen(dbName, TypeDBSession.Type.SCHEMA)
        wait(composeRule, 750)

        StudioState.client.tryUpdateTransactionType(TypeDBTransaction.Type.WRITE)
        wait(composeRule, 750)

        composeRule.onNodeWithText("schema").performClick()
        composeRule.onNodeWithText("write").performClick()

        StudioState.project.current!!.directory.entries.find { it.name == schemaFileName }!!.asFile().tryOpen()

        composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
        wait(composeRule, 750)

        composeRule.onNodeWithText(CHECK_ICON_STRING).performClick()
        wait(composeRule, 1_500)

        StudioState.client.session.close()
    }

    suspend fun writeDataInteractively(composeRule: ComposeContentTestRule, dbName: String, dataFileName: String) {
        StudioState.client.session.tryOpen(dbName, TypeDBSession.Type.DATA)

        wait(composeRule, 750)

        composeRule.onNodeWithText("data").performClick()
        composeRule.onNodeWithText("write").performClick()

        StudioState.project.current!!.directory.entries.find { it.name == dataFileName }!!.asFile().tryOpen()

        composeRule.onNodeWithText(PLAY_ICON_STRING).performClick()
        wait(composeRule, 750)

        composeRule.onNodeWithText(CHECK_ICON_STRING).performClick()
        wait(composeRule, 1_500)

        StudioState.client.session.close()
    }

    suspend fun verifyDataWrite(composeRule: ComposeContentTestRule, address: String, dbName: String, queryFileName: String) {
        val queryString = fileNameToString(queryFileName)

        composeRule.onNodeWithText("infer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("read").performClick()
        wait(composeRule, 1_000)

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
}

