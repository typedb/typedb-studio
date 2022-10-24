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
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeNotExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.clickIcon
import com.vaticle.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.vaticle.typedb.studio.test.integration.common.StudioActions.assertNodeNotExistsWithText
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilNodeWithTextExists
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.waitUntilNodeWithTextNotExists
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProjectBrowserTest : IntegrationTest() {

    @Test
    fun createADirectory() {
        runBlocking {
            val createdDirectoryName = "created"

            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            Service.project.current!!.directory.asDirectory()
                .tryCreateDirectory(createdDirectoryName)

            Service.project.current!!.reloadEntries()

            waitUntilNodeWithTextExists(composeRule, text = createdDirectoryName)
        }
    }

    @Test
    fun createAFile() {
        runBlocking {
            val createdFileName = "created"

            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            Service.project.current!!.directory.asDirectory()
                .tryCreateFile(createdFileName)

            Service.project.current!!.reloadEntries()

            waitUntilNodeWithTextExists(composeRule, text = createdFileName)
        }
    }

    @Test
    fun renameAFile() {
        runBlocking {
            val renamedFileName = "renamed"

            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            Service.project.current!!.directory.entries.find { it.name == "file3" }!!
                .asFile()
                .tryRename(renamedFileName)

            Service.project.current!!.reloadEntries()

            waitUntilNodeWithTextExists(composeRule, text = renamedFileName)
        }
    }

    @Test
    fun deleteAFile() {
        runBlocking {
            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            Service.project.current!!.directory.entries.find { it.name == "file3" }!!
                .asFile().tryDelete()

            Service.project.current!!.reloadEntries()

            waitUntilNodeWithTextNotExists(composeRule, text = "file3")
        }
    }

    @Test
    fun expandFolders() {
        runBlocking {
            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            clickIcon(composeRule, Icon.EXPAND)

            waitUntilNodeWithTextExists(composeRule, text = "file0")
        }
    }

    @Test
    fun expandThenCollapseFolders() {
        runBlocking {
            copyFolder(source = SampleFileStructure.path, destination = testID)
            openProject(composeRule, projectDirectory = testID)

            clickIcon(composeRule, Icon.EXPAND)

            waitUntilNodeWithTextExists(composeRule, text = "file0")

            clickIcon(composeRule, Icon.COLLAPSE)

            waitUntilNodeWithTextExists(composeRule, text = testID)
            waitUntilNodeWithTextNotExists(composeRule, text = "file0")
        }
    }
}