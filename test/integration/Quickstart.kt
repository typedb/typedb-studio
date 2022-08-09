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

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import java.io.File
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
    companion object {
        private const val DB_NAME = "quickstart"

        private val QUERY_FILE_PATH = File("$TQL_DATA_PATH/$QUERY_FILE_NAME").absolutePath
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun Quickstart() {
        val funcName = object{}.javaClass.enclosingMethod.name
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 500, 0, 0))
            }

            composeRule.waitForIdle()
            connectToTypeDB(composeRule, DB_ADDRESS)
            createDatabase(composeRule, DB_NAME)
            cloneAndOpenProject(composeRule, TQL_DATA_PATH, funcName)
            writeSchemaInteractively(composeRule, DB_NAME, SCHEMA_FILE_NAME)
            writeDataInteractively(composeRule, DB_NAME, DATA_FILE_NAME)
            verifyAnswers(composeRule)
        }
    }

    private suspend fun verifyAnswers(composeRule: ComposeContentTestRule) {
        val queryString = fileNameToString(QUERY_FILE_PATH)

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
}