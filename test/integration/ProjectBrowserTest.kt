/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.test.integration

import androidx.compose.ui.test.onNodeWithText
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.test.integration.common.StudioActions.Delays
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.vaticle.typedb.studio.test.integration.common.StudioActions.delayAndRecompose
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilAssertionPasses
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilNodeWithTextExists
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilTrue
import com.vaticle.typedb.studio.test.integration.data.Paths.SampleFileStructure
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
