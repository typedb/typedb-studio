/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.module.role

import androidx.compose.runtime.Composable
import com.typedb.studio.framework.material.Browsers
import com.typedb.studio.framework.material.Form.IconButtonArg
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.service.common.util.Label

class RoleBrowser(isOpen: Boolean = false, order: Int) : Browsers.Browser(isOpen, order) {

    override val label: String = Label.ROLES
    override val icon: Icon = Icon.ROLES
    override val isActive: Boolean get() = false // TODO
    override val buttons: List<IconButtonArg> = listOf()

    @Composable
    override fun Content() {
    }
}
