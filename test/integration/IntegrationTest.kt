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

import androidx.compose.ui.test.junit4.createComposeRule
import com.vaticle.typedb.studio.state.StudioState
import org.junit.Before
import org.junit.Rule
import java.util.UUID
import kotlinx.coroutines.delay

abstract class IntegrationTest {
    lateinit var testID: String

    @Before
    fun setupTest() {
        StudioState.init()
        testID = UUID.randomUUID().toString()
    }

    @get:Rule
    var composeRule = createComposeRule()
}