/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.module.rule

import androidx.compose.runtime.Composable
import com.vaticle.typedb.studio.framework.material.Browsers
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.common.util.Label

class RuleBrowser(isOpen: Boolean = false, order: Int) : Browsers.Browser(isOpen, order) {

    override val label: String = Label.RULES
    override val icon: Icon = Icon.RULES
    override val isActive: Boolean get() = false // TODO
    override val buttons: List<IconButtonArg> = listOf()

    @Composable
    override fun Content() {
    }
}
