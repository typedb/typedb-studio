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

// We need to access private function Studio.MainWindow, this allows us to.
// Do not use this outside of tests anywhere. It is extremely dangerous to do so.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.vaticle.typedb.studio.test.integration

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.common.test.TypeDBSingleton
import com.vaticle.typedb.common.test.core.TypeDBCoreRunner
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import kotlinx.coroutines.delay
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Some of these tests use delay!
 *
 * The rationale for this is that substituting in stub classes/methods would create a lot of friction from release to
 * release as the tests would require updating to completely reflect all the internal state that changes with each
 * function. As a heavily state-driven application, duplicating all of this functionality and accurately verifying that
 * the duplicate is like-for-like is out of scope.
 *
 * The delays are:
 *  - used only when necessary (some data is travelling between the test and TypeDB)
 *  - generous with the amount of time for the required action.
 *
 * However, this is a source of non-determinism and a better and easier way may emerge.
 */
class Quickstart {
    // This test simulates the carrying out of the instructions found at https://docs.vaticle.com/docs/studio/quickstart
    @Test
    fun `Quickstart`() {
        val composeRule = createComposeRule()

        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 500, 0, 0))
            }
            var server: TypeDBCoreRunner
            try {
                server = TypeDBCoreRunner()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            server.start()
//            TypeDBSingleton.setTypeDBRunner(server)

            composeRule.waitForIdle()
            connectToTypeDB(composeRule)
            createDatabase(composeRule)
            openProject(composeRule)
            writeSchema(composeRule)
            writeData(composeRule)
            verifyAnswers(composeRule)
        }
    }

    suspend fun connectToTypeDB(composeRule: ComposeContentTestRule) {
        // This opens a dialog box (which we can't see through) so we assert that buttons with that text can be
        // clicked.
        composeRule.onAllNodesWithText("Connect to TypeDB").assertAll(hasClickAction())

        StudioState.client.tryConnectToTypeDB(DB_ADDRESS) {}
        // We wait to connect to TypeDB. This can be slow by default on macOS, so we wait a while.
        delay(10_000)
        // Order is important here! We delay allowing the connection to take place, then give the program
        // time to recompose. If we waitForIdle then delay, it recomposes before connecting failing the
        // next assertExists.
        composeRule.waitForIdle()
        assertTrue(StudioState.client.isConnected)

        composeRule.onNodeWithText(DB_ADDRESS).assertExists()
    }

    suspend fun createDatabase(composeRule: ComposeContentTestRule) {
        // Same as connecting to typedb, but we can't see dropdowns either.
        composeRule.onAllNodesWithText("Select Database").assertAll(hasClickAction())

        try {
            StudioState.client.tryDeleteDatabase(DB_NAME)
        }
        catch (_: Exception) {}
        delay(1_000)

        StudioState.client.tryCreateDatabase(DB_NAME) {}
        delay(1_000)

        StudioState.client.tryOpenSession(DB_NAME)
        delay(1_000)
    }

    suspend fun openProject(composeRule: ComposeContentTestRule) {
        // Could probably also store the file locally, include it in the test and open the file through the
        // project browser then use the GUI to operate.

        StudioState.project.tryOpenProject(File("test/data").toPath())
        StudioState.appData.project.path = File("test/data").toPath()
        composeRule.waitForIdle()

        // Attempting to click these throws an errors since we use a pointer system that requires existence of a
        // window/awt backed API, but we can't use windows/awt because of limitations in the testing framework.

        // But we can assert that they exist, which is a test unto itself.
        composeRule.onNodeWithText("schema_string.tql").assertExists()
        composeRule.onNodeWithText("data_string.tql").assertExists()
    }

    suspend fun writeSchema(composeRule: ComposeContentTestRule) {
        val schemaString = fileNameToString("test/data/schema_string.tql")

        composeRule.onNodeWithText("schema").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("write").performClick()
        composeRule.waitForIdle()

        StudioState.client.session.tryOpen("github", TypeDBSession.Type.SCHEMA)
        delay(1_000)
        StudioState.client.tryUpdateTransactionType(TypeDBTransaction.Type.WRITE)
        delay(1_000)
        StudioState.client.session.transaction.runQuery(schemaString)
        delay(1_000)

        // Commit the schema write.
        // Switch these two statements when we can use windows.
//            composeRule.onNodeWithText(CHECK_STRING).performClick()
        StudioState.client.session.transaction.commit()
        delay(1_000)
    }

    suspend fun writeData(composeRule: ComposeContentTestRule) {
        val dataString = fileNameToString("test/data/data_string.tql")

        composeRule.onNodeWithText("write").performClick()
        composeRule.waitForIdle()

        StudioState.client.session.tryOpen(DB_NAME, TypeDBSession.Type.DATA)
        delay(1_000)
        StudioState.client.session.transaction.runQuery(dataString)
        delay(1_000)
        StudioState.client.session.transaction.commit()
    }

    suspend fun verifyAnswers(composeRule: ComposeContentTestRule) {
        val queryString = fileNameToString("test/data/query_string.tql")

        composeRule.onNodeWithText("infer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("read").performClick()
        composeRule.waitForIdle()
        delay(1_000)

        TypeDB.coreClient(DB_ADDRESS).use { client ->
            client.session(DB_NAME, TypeDBSession.Type.DATA, TypeDBOptions.core().infer(true)).use { session ->
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

    companion object {
        val DB_ADDRESS = "localhost:1729"
        val DB_NAME = "github"

        val CLOSE_TRANSACTION_STRING = Char(0xf00du).toString()
        val ROLLBACK_STRING = Char(0xf2eau).toString()
        val CHECK_STRING = Char(0xf00cu).toString()

        val PLAY_STRING = Char(0xf04bu).toString()
        val BOLT_STRING = Char(0xf0e7u).toString()
    }
}