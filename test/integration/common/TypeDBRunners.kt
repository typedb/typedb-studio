/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.test.integration.common

import com.typedb.cloud.tool.runner.TypeDBCloudRunner
import com.typedb.core.tool.runner.TypeDBCoreRunner
import com.typedb.core.tool.runner.TypeDBRunner
import kotlin.io.path.Path

object TypeDBRunners {
    val CLOUD_RUNNER_SERVER_COUNT = 3
    val CLOUD_RUNNER_DATA_PATH = Path("cloud-runner-data-path")

    fun withTypeDB(runnerType: RunnerType = RunnerType.CORE, testFunction: (TypeDBRunner) -> Unit) {
        val typeDB = when (runnerType) {
            RunnerType.CORE -> TypeDBCoreRunner()
            RunnerType.CLOUD -> TypeDBCloudRunner.create(CLOUD_RUNNER_DATA_PATH, CLOUD_RUNNER_SERVER_COUNT)
        }
        typeDB.start()
        testFunction(typeDB)
        typeDB.stop()
    }

    enum class RunnerType {
        CORE,
        CLOUD
    }
}
