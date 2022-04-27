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

package com.vaticle.typedb.studio.view.page

import androidx.compose.runtime.Composable
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.schema.TypeState
import com.vaticle.typedb.studio.view.common.Util.typeIcon
import com.vaticle.typedb.studio.view.common.component.Form

class TypePage constructor(private var type: TypeState) : Page(type) {

    override val name: String = type.name
    override val icon: Form.IconArg = typeIcon(type)

    override fun updateResourceInner(resource: Resource) {
        type = resource as TypeState
    }

    @Composable
    override fun Content() {
        // TODO
    }
}