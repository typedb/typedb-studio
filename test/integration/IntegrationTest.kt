/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// We need to access the private function Studio.MainWindowColumn, this allows us to.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.typedb.studio.test.integration

import androidx.compose.ui.test.junit4.createComposeRule
import com.typedb.studio.Studio
import com.typedb.studio.framework.common.WindowContext
import com.typedb.studio.service.Service
import java.util.UUID
import org.junit.Before
import org.junit.Rule

/**
 * Some of these tests use delay!
 *
 * The rationale for this is that substituting in stub classes/methods would create a lot of friction from release to
 * release as the tests would require updating to completely reflect all the internal state that changes with each
 * function. As a heavily state-driven application, duplicating all of this functionality and accurately verifying that
 * the duplicate is like-for-like is out of scope.
 *
 * The delays are:
 *  - used only when necessary (some data is travelling between the test and TypeDB)
 *  - generous with the amount of time for the required action.
 *
 * However, this is a source of non-determinism and a better and easier way may emerge.
 */
abstract class IntegrationTest {
    lateinit var testID: String

    @Before
    fun setupTest() {
        Service.init()
        testID = UUID.randomUUID().toString()
        composeRule.setContent { Studio.MainWindowContent(WindowContext.Test(1000, 1000, 0, 0)) }
    }

    @get:Rule
    var composeRule = createComposeRule()
}
