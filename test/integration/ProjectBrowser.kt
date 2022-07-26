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

// We need to access private function Studio.MainWindowColumn, this allows us to.
// Do not use this outside of tests anywhere. It is extremely dangerous to do so.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.vaticle.typedb.studio.test.integration

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.project.FileState
import com.vaticle.typedb.studio.state.project.PathState
import com.vaticle.typedb.studio.test.integration.runComposeRule
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectBrowser {
    companion object {
        private const val DB_ADDRESS = "localhost:1729"
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `Rename a File`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            StudioState.client.tryConnectToTypeDB(DB_ADDRESS) {}

            delay(10_000)
            composeRule.waitForIdle()

            assertTrue(StudioState.client.isConnected)

            composeRule.waitForIdle()

            StudioState.project.tryOpenProject(File("./test/data").toPath())
            StudioState.appData.project.path = File("./test/data").toPath()
            composeRule.waitForIdle()

            // Hard to tell if this is succeeding within Bazel.
            StudioState.project.current!!.directory.entries[1].asFile().tryRename("renamed.tql")
            delay(500)
            StudioState.project.current!!.reloadEntries()
            delay(500)
            composeRule.waitForIdle()

            composeRule.onNodeWithText("renamed.tql").assertExists()
        }
    }

    @Ignore
    @Test
    fun `Move a File`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()
        }
    }

    @Ignore
    @Test
    fun `Delete a File`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()
        }
    }

    @Ignore
    @Test
    fun `Create a Directory`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()
        }
    }

    @Ignore
    @Test
    fun `Delete a Directory`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()
        }
    }

    @Ignore
    @Test
    fun `Collapse Folders`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()
        }
    }

    @Ignore
    @Test
    fun `Collapse Then Expand Folders`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()
        }
    }
}