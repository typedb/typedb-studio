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
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.test.integration.common.StudioActions
import com.vaticle.typedb.studio.test.integration.data.Paths.SampleFileStructure
import com.vaticle.typedb.studio.test.integration.common.StudioActions.Delays
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilNodeWithTextExists
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilAssertionPasses
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilTrue
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProjectBrowserTest : IntegrationTest() {

    @Test
    fun createADirectory() {
        runBlocking {
            val createdDirectoryName = "created"
            val path = copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            Service.project.current!!.directory.asDirectory().tryCreateDirectory(createdDirectoryName)

            val file = File("$path/$createdDirectoryName")
            waitUntilTrue(composeRule) {
                file.exists()
            }

            Service.project.current!!.reloadEntries()

            waitUntilAssertionPasses(composeRule) {
                composeRule.onNodeWithText(createdDirectoryName).assertExists()
            }
        }
    }

    @Test
    fun createAFile() {
        runBlocking {
            val createdFileName = "created"
            val path = copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            Service.project.current!!.directory.asDirectory().tryCreateFile(createdFileName)

            val file = File("$path/$createdFileName")
            waitUntilTrue(composeRule) {
                file.exists()
            }

            Service.project.current!!.reloadEntries()

            waitUntilAssertionPasses(composeRule) {
                composeRule.onNodeWithText(createdFileName).assertExists()
            }
        }
    }

    @Test
    fun renameAFile() {
        runBlocking {
            val renamedFileName = "renamed"
            val path = copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            Service.project.current!!.directory.entries.find { it.name == "file3" }!!
                .asFile()
                .tryRename(renamedFileName)

            val file = File("$path/$renamedFileName")
            waitUntilTrue(composeRule) {
                file.exists()
            }

            Service.project.current!!.reloadEntries()

            waitUntilAssertionPasses(composeRule) {
                composeRule.onNodeWithText(renamedFileName).assertExists()
            }
        }
    }

    @Test
    fun deleteAFile() {
        runBlocking {
            val deletedFileName = "file3"
            val path = copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            Service.project.current!!.directory.entries.find { it.name == deletedFileName }!!.asFile().tryDelete()

            val file = File("$path/$deletedFileName")
            waitUntilTrue(composeRule) {
                !file.exists()
            }

            Service.project.current!!.reloadEntries()

            delayAndRecompose(composeRule, Delays.FILE_IO)
            composeRule.onNodeWithText(deletedFileName).assertDoesNotExist()
        }
    }

    @Test
    fun expandFolders() {
        runBlocking {
            val fileName = "file0"
            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            clickIcon(composeRule, Icon.EXPAND)

            waitUntilAssertionPasses(composeRule) {
                composeRule.onNodeWithText(fileName).assertExists()
            }
        }
    }

    @Test
    fun expandThenCollapseFolders() {
        runBlocking {
            val fileName = "file0"
            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            clickIcon(composeRule, Icon.EXPAND)

            waitUntilNodeWithTextExists(composeRule, text = fileName)

            clickIcon(composeRule, Icon.COLLAPSE)

            waitUntilNodeWithTextExists(composeRule, text = testID)

            delayAndRecompose(composeRule, Delays.FILE_IO)
            composeRule.onNodeWithText(fileName).assertDoesNotExist()
        }
    }
}