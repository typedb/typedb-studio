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
import com.vaticle.typedb.studio.framework.material.Navigator
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

    @Test
    fun `Rename a File`() {
        val composeRule = createComposeRule()
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            openProject(composeRule, "./test/sample_file_structure")

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile().tryRename("file3_0")
            delay(500)
            StudioState.project.current!!.reloadEntries()
            delay(500)
            composeRule.waitForIdle()

            composeRule.onNodeWithText("file3_0").assertExists()
            StudioState.project.current!!.directory.entries.find { it.name == "file3_0" }!!.asFile().tryRename("file3")
        }
    }

    @Test
    fun `Delete a File`() {
        val composeRule = createComposeRule()
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            openProject(composeRule, "./test/sample_file_structure")

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile().initiateDelete { }
            delay(500)
            StudioState.project.current!!.reloadEntries()
            delay(500)
            composeRule.waitForIdle()

            composeRule.onNodeWithText("file3").assertDoesNotExist()

            StudioState.project.current!!.directory.entries.find { it.name == "sample_file_structure" }!!.asDirectory()
                .tryCreateFile("file3")
            delay(500)
        }
    }

    @Test
    fun `Create a Directory`() {
        val composeRule = createComposeRule()
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            openProject(composeRule, "./test/sample_file_structure")

            StudioState.project.current!!.directory.entries[0].asDirectory().tryCreateDirectory("create_a_directory")
            delay(500)
            StudioState.project.current!!.reloadEntries()
            delay(500)
            composeRule.waitForIdle()

            composeRule.onNodeWithText("create_a_directory").assertExists()

            StudioState.project.current!!.directory.entries.find { it.name == "create_a_directory" }!!.asDirectory()
                .initiateDelete { }
        }
    }

    @Test
    fun `Create a File`() {
        val composeRule = createComposeRule()
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            openProject(composeRule, "./test/sample_file_structure")

            StudioState.project.current!!.directory.entries.find{it.name=="sample_file_structure"}!!.asDirectory().tryCreateFile("file4")
            delay(500)
            StudioState.project.current!!.reloadEntries()
            delay(500)
            composeRule.waitForIdle()

            composeRule.onNodeWithText("file4").assertExists()

            StudioState.project.current!!.directory.entries.find { it.name == "file4" }!!.asFile()
                .initiateDelete { }
        }
    }

    @Ignore
    @Test
    fun `Collapse Folders`() {
        val composeRule = createComposeRule()
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent(WindowContext(1000, 1000, 0, 0))
            }
            composeRule.waitForIdle()

            openProject(composeRule, "./test/sample_file_structure")

//            StudioState.project.current!!.directory.entries[0].asDirectory().isExpandable
            delay(500)
            StudioState.project.current!!.reloadEntries()
            delay(500)
            composeRule.waitForIdle()
        }
    }
}