/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.test.integration

import com.vaticle.typedb.studio.test.integration.common.StudioActions.connectToTypeDB
import com.vaticle.typedb.studio.test.integration.common.StudioActions.copyFolder
import com.vaticle.typedb.studio.test.integration.common.StudioActions.createDatabase
import com.vaticle.typedb.studio.test.integration.common.StudioActions.openProject
import com.vaticle.typedb.studio.test.integration.common.StudioActions.verifyDataWrite
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeDataInteractively
import com.vaticle.typedb.studio.test.integration.common.StudioActions.writeSchemaInteractively
import com.vaticle.typedb.studio.test.integration.common.TypeDBRunners.withTypeDB
import com.vaticle.typedb.studio.test.integration.data.Paths.SampleGitHubData
import kotlinx.coroutines.runBlocking
import org.junit.Test

class QuickstartTest : IntegrationTest() {

    @Test
    fun quickstart() {
        withTypeDB { typeDB ->
            runBlocking {
                connectToTypeDB(composeRule, typeDB.address())
                createDatabase(composeRule, dbName = testID)
                copyFolder(source = SampleGitHubData.path, destination = testID)
                openProject(composeRule, projectDirectory = testID)
                writeSchemaInteractively(composeRule, dbName = testID, SampleGitHubData.schemaFile)
                writeDataInteractively(composeRule, dbName = testID, SampleGitHubData.dataFile)
                verifyDataWrite(
                    typeDB.address(), dbName = testID, "$testID/${SampleGitHubData.collaboratorsQueryFile}"
                )
            }
        }
    }
}
