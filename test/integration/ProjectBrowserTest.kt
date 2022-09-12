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

import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.test.integration.common.Paths.SAMPLE_DATA_PATH
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createData
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.StudioActions.nodeWithTextDoesNotExist
import com.vaticle.typedb.studio.test.integration.common.StudioActions.nodeWithTextExists
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProjectBrowserTest: IntegrationTest() {

    @Test
    fun createADirectory() {
        runBlocking {
            val createdDirectoryName = "created"

            createData(composeRule, source = SAMPLE_DATA_PATH, destination = testID)
            openProject(composeRule, testID)

            StudioState.project.current!!.directory.asDirectory().tryCreateDirectory(createdDirectoryName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            nodeWithTextExists(composeRule, createdDirectoryName)
        }
    }

    @Test
    fun createAFile() {
        runBlocking {
            val createdFileName = "created"

            createData(composeRule, source = SAMPLE_DATA_PATH, destination = testID)
            openProject(composeRule, testID)

            StudioState.project.current!!.directory.asDirectory().tryCreateFile(createdFileName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            nodeWithTextExists(composeRule, createdFileName)
        }
    }

    @Test
    fun renameAFile() {
        runBlocking {
            val renamedFileName = "renamed"

            createData(composeRule, source = SAMPLE_DATA_PATH, destination = testID)
            openProject(composeRule, testID)

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile()
                .tryRename(renamedFileName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            nodeWithTextExists(composeRule, renamedFileName)
        }
    }

    @Test
    fun deleteAFile() {
        runBlocking {
            createData(composeRule, source = SAMPLE_DATA_PATH, destination = testID)
            openProject(composeRule, testID)

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile().tryDelete()
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            nodeWithTextDoesNotExist(composeRule, "file3")
        }
    }

    @Test
    fun expandFolders() {
        runBlocking {
            createData(composeRule, source = SAMPLE_DATA_PATH, destination = testID)
            openProject(composeRule, testID)

            clickIcon(composeRule, Icon.Code.CHEVRONS_DOWN)
            composeRule.waitForIdle()

            nodeWithTextExists(composeRule, "file1_2")
        }
    }

    @Test
    fun expandThenCollapseFolders() {
        runBlocking {
            createData(composeRule, source = SAMPLE_DATA_PATH, destination = testID)
            openProject(composeRule, testID)

            clickIcon(composeRule, Icon.Code.CHEVRONS_DOWN)
            composeRule.waitForIdle()
            nodeWithTextExists(composeRule, "file1_2")

            clickIcon(composeRule, Icon.Code.CHEVRONS_UP)
            composeRule.waitForIdle()

            nodeWithTextExists(composeRule, testID)
            nodeWithTextDoesNotExist(composeRule, "file1_2")
        }
    }
}