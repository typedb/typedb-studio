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

package com.vaticle.typedb.studio.module.role

import androidx.compose.runtime.Composable
import com.vaticle.typedb.studio.framework.material.Browsers
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.common.util.Label

class RoleBrowser(isOpen: Boolean = false, order: Int) : Browsers.Browser(isOpen, order) {

    override val label: String = Label.ROLES
    override val icon: Icon = Icon.ROLES
    override val isActive: Boolean get() = false // TODO
    override val buttons: List<IconButtonArg> = listOf()

    @Composable
    override fun Content() {
    }
}
