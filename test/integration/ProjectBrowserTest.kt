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

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.test.integration.Utils.COLLAPSE_ICON_STRING
import com.vaticle.typedb.studio.test.integration.Utils.cloneAndOpenProject
import com.vaticle.typedb.studio.test.integration.Utils.studioTest
import com.vaticle.typedb.studio.test.integration.Utils.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.Utils.SAMPLE_DATA_PATH
import com.vaticle.typedb.studio.test.integration.Utils.EXPAND_ICON_STRING
import org.junit.Test

class ProjectBrowserTest: IntegrationTest() {

    @Test
    fun createADirectory() {
        studioTest(composeRule) {
            val createdDirectoryName = "created"

            cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            StudioState.project.current!!.directory.asDirectory().tryCreateDirectory(createdDirectoryName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText(createdDirectoryName).assertExists()
        }
    }

    @Test
    fun createAFile() {
        studioTest(composeRule) {
            val createdFileName = "created"

            cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            StudioState.project.current!!.directory.asDirectory().tryCreateFile(createdFileName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText(createdFileName).assertExists()

        }
    }

    @Test
    fun renameAFile() {
        studioTest(composeRule) {
            val renamedFileName = "renamed"

            cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile()
                .tryRename(renamedFileName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText(renamedFileName).assertExists()
        }
    }

    @Test
    fun deleteAFile() {
        studioTest(composeRule) {
            cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile().tryDelete()
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            composeRule.onNodeWithText("file3").assertDoesNotExist()
        }
    }

    @Test
    fun expandFolders() {
        studioTest(composeRule) {
            cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            composeRule.onNodeWithText(EXPAND_ICON_STRING).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText("file1_2").assertExists()
        }
    }

    @Test
    fun expandThenCollapseFolders() {
        studioTest(composeRule) {
            cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            composeRule.onNodeWithText(EXPAND_ICON_STRING).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("file1_2").assertExists()

            composeRule.onNodeWithText(COLLAPSE_ICON_STRING).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText(testID).assertExists()
            composeRule.onNodeWithText("file1_2").assertDoesNotExist()
        }
    }
}