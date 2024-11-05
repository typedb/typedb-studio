/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.test.integration.data

import java.io.File

object Paths {
    object SampleFileStructure {
        val path = File("test/integration/data/sample_file_structure").absolutePath
    }

    object SampleGitHubData {
        val path = File("test/integration/data/sample_github_data").absolutePath
        val collaboratorsQueryFile = "github_collaborators_query.tql"
        val dataFile = "github_data.tql"
        val schemaFile = "github_schema.tql"
    }
}
