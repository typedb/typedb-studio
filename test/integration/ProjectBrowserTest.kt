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

package com.vaticle.typedb.studio.test.integration

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.test.integration.common.StudioActions.cloneAndOpenProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.Data.DOUBLE_CHEVRON_DOWN_ICON_STRING
import com.vaticle.typedb.studio.test.integration.common.Data.DOUBLE_CHEVRON_UP_ICON_STRING
import com.vaticle.typedb.studio.test.integration.common.Data.SAMPLE_DATA_PATH
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProjectBrowserTest: IntegrationTest() {

    @Test
    fun createADirectory() {
        runBlocking {
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
        runBlocking {
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
        runBlocking {
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
        runBlocking {
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
        runBlocking {
            cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_DOWN_ICON_STRING).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText("file1_2").assertExists()
        }
    }

    @Test
    fun expandThenCollapseFolders() {
        runBlocking {
            cloneAndOpenProject(composeRule, source = SAMPLE_DATA_PATH, destination = testID)

            composeRule.onNodeWithText(DOUBLE_CHEVRON_DOWN_ICON_STRING).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("file1_2").assertExists()

            composeRule.onNodeWithText(DOUBLE_CHEVRON_UP_ICON_STRING).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText(testID).assertExists()
            composeRule.onNodeWithText("file1_2").assertDoesNotExist()
        }
    }
}