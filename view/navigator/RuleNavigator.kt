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

package com.vaticle.typedb.studio.view.navigator

import androidx.compose.runtime.Composable
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Icon

internal class RuleNavigator(areaState: NavigatorArea.AreaState, initOpen: Boolean = false) :
    Navigator(areaState, initOpen) {

    override val icon: Icon.Code = Icon.Code.DIAGRAM_PROJECT
    override val label: String = Label.RULES
    override val buttons: List<ButtonArgs> = listOf()

    @Composable
    override fun Catalog() {

    }
}
