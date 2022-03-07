/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.studio.view.output

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.state.runner.Response
import com.vaticle.typedb.studio.view.common.component.Form

internal object TableOutput : RunOutput() {

    internal class State(response: Response.Table) : RunOutput.State() {

    }

    @Composable
    internal fun Layout(state: State) {
        super.Layout(toolbarButtons(state)) { modifier ->
            Content(state, modifier)
        }
    }

    private fun toolbarButtons(state: State): List<Form.ButtonArg> {
        return listOf()
    }

    @Composable
    private fun Content(state: State, modifier: Modifier) {
        Box(modifier) // TODO
    }
}
