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

package com.vaticle.typedb.studio.test.integration.common

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.vaticle.typedb.common.test.core.TypeDBCoreRunner
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.framework.common.WindowContext
import kotlinx.coroutines.runBlocking

object StudioTestHelpers {
    fun studioTest(compose: ComposeContentTestRule, funcBody: suspend () -> Unit) {
        runComposeRule(compose) {
            setContent { Studio.MainWindowContent(WindowContext.Test(1000, 1000, 0, 0)) }
            funcBody()
        }
    }

    fun studioTestWithRunner(compose: ComposeContentTestRule, funcBody: suspend (String) -> Unit) {
        val typeDB = TypeDBCoreRunner()
        typeDB.start()
        val address = typeDB.address()
        runComposeRule(compose) {
            setContent { Studio.MainWindowContent(WindowContext.Test(1000, 1000, 0, 0)) }
            funcBody(address)
        }
        typeDB.stop()
    }

    private fun runComposeRule(compose: ComposeContentTestRule, rule: suspend ComposeContentTestRule.() -> Unit) {
        runBlocking { compose.rule() }
    }
}

