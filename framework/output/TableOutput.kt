/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.output

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.typedb.studio.framework.material.Form
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.connection.TransactionState
import com.typedb.driver.api.answer.ConceptRow

internal class TableOutput constructor(val transaction: TransactionState, number: Int) : RunOutput() {

    override val name: String = Label.TABLE + " ($number)"
    override val icon: Icon = Icon.TABLE
    override val buttons: List<Form.IconButtonArg> = listOf()

    internal fun outputFn(row: ConceptRow): () -> Unit {
        return {} // TODO
    }

    @Composable
    override fun content(modifier: Modifier) {
        Box(modifier) // TODO
    }
}
