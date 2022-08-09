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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.state.StudioState
import org.junit.Rule
import org.junit.Test

class ProjectBrowser {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun createADirectory() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            val createdDirectoryName = "created"

            cloneAndOpenProject(composeRule, SAMPLE_DATA_PATH, funcName)

            StudioState.project.current!!.directory.asDirectory().tryCreateDirectory(createdDirectoryName)
            wait(composeRule, 500)

            StudioState.project.current!!.reloadEntries()
            wait(composeRule, 500)

            composeRule.onNodeWithText(createdDirectoryName).assertExists()
        }
    }

    @Test
    fun createAFile() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            val createdFileName = "created"

            cloneAndOpenProject(composeRule, SAMPLE_DATA_PATH, funcName)

            StudioState.project.current!!.directory.asDirectory().tryCreateFile(createdFileName)
            wait(composeRule, 500)

            StudioState.project.current!!.reloadEntries()
            wait(composeRule, 500)

            composeRule.onNodeWithText(createdFileName).assertExists()

        }
    }

    @Test
    fun renameAFile() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            val renamedFileName = "renamed"

            cloneAndOpenProject(composeRule, SAMPLE_DATA_PATH, funcName)

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile()
                .tryRename(renamedFileName)
            wait(composeRule, 500)

            StudioState.project.current!!.reloadEntries()
            wait(composeRule, 500)

            composeRule.onNodeWithText(renamedFileName).assertExists()
        }
    }

    @Test
    fun deleteAFile() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            cloneAndOpenProject(composeRule, SAMPLE_DATA_PATH, funcName)

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile().delete()
            wait(composeRule, 500)

            StudioState.project.current!!.reloadEntries()
            wait(composeRule, 500)

            composeRule.onNodeWithText("file3").assertDoesNotExist()
        }
    }

    @Test
    fun expandFolders() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            cloneAndOpenProject(composeRule, SAMPLE_DATA_PATH, funcName)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_DOWN_ICON_STRING).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText("file1_2").assertExists()
        }
    }

    @Test
    fun expandThenCollapseFolders() {
        val funcName = object{}.javaClass.enclosingMethod.name
        studioTest(composeRule) {
            cloneAndOpenProject(composeRule, SAMPLE_DATA_PATH, funcName)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_DOWN_ICON_STRING).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("file1_2").assertExists()

            composeRule.onNodeWithText(DOUBLE_CHEVRON_UP_ICON_STRING).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText(funcName).assertExists()
            composeRule.onNodeWithText("file1_2").assertDoesNotExist()
        }
    }
}