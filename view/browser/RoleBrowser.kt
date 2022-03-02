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

package com.vaticle.typedb.studio.view.browser

import androidx.compose.runtime.Composable
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArgs
import com.vaticle.typedb.studio.view.common.component.Icon

internal class RoleBrowser(areaState: BrowserArea.State, order: Int, initOpen: Boolean = false) :
    Browser(areaState, order, initOpen) {

    override val label: String = Label.ROLES
    override val icon: Icon.Code = Icon.Code.USER_GROUP
    override val isActive: Boolean get() = false // TODO
    override val buttons: List<ButtonArgs> = listOf()

    @Composable
    override fun BrowserLayout() {

    }
}
