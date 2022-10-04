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
import com.vaticle.typedb.studio.test.integration.data.Paths.SampleFileStructure
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeNotExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProjectBrowserTest: IntegrationTest() {

    @Test
    fun createADirectory() {
        runBlocking {
            val createdDirectoryName = "created"

            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            StudioState.project.current!!.directory.asDirectory().tryCreateDirectory(createdDirectoryName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            assertNodeExistsWithText(composeRule, text = createdDirectoryName)
        }
    }

    @Test
    fun createAFile() {
        runBlocking {
            val createdFileName = "created"

            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            StudioState.project.current!!.directory.asDirectory().tryCreateFile(createdFileName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            assertNodeExistsWithText(composeRule, text = createdFileName)
        }
    }

    @Test
    fun renameAFile() {
        runBlocking {
            val renamedFileName = "renamed"

            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile()
                .tryRename(renamedFileName)
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            assertNodeExistsWithText(composeRule, text = renamedFileName)
        }
    }

    @Test
    fun deleteAFile() {
        runBlocking {
            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            StudioState.project.current!!.directory.entries.find { it.name == "file3" }!!.asFile().tryDelete()
            delayAndRecompose(composeRule)

            StudioState.project.current!!.reloadEntries()
            delayAndRecompose(composeRule)

            assertNodeNotExistsWithText(composeRule, text = "file3")
        }
    }

    @Test
    fun expandFolders() {
        runBlocking {
            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            clickIcon(composeRule, Icon.EXPAND)

            assertNodeExistsWithText(composeRule, text = "file0")
        }
    }

    @Test
    fun expandThenCollapseFolders() {
        runBlocking {
            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            clickIcon(composeRule, Icon.EXPAND)

            assertNodeExistsWithText(composeRule, text = "file0")

            clickIcon(composeRule, Icon.COLLAPSE)

            assertNodeExistsWithText(composeRule, text = testID)
            assertNodeNotExistsWithText(composeRule, text = "file0")
        }
    }
}