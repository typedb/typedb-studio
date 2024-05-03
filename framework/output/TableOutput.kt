/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.output

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaticle.typedb.driver.api.answer.ConceptMap
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.connection.TransactionState

internal class TableOutput constructor(val transaction: TransactionState, number: Int) : RunOutput() {

    override val name: String = Label.TABLE + " ($number)"
    override val icon: Icon = Icon.TABLE
    override val buttons: List<Form.IconButtonArg> = listOf()

    internal fun outputFn(conceptMap: ConceptMap): () -> Unit {
        return {} // TODO
    }

    @Composable
    override fun content(modifier: Modifier) {
        Box(modifier) // TODO
    }
}
