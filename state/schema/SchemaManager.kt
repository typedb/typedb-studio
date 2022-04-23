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

package com.vaticle.typedb.studio.state.schema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.connection.SessionState
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SchemaManager(val session: SessionState, internal val notificationMgr: NotificationManager) {

    var root: TypeState? by mutableStateOf(null); private set
    var onRootChange: ((TypeState) -> Unit)? = null
    private val typeSchema: String? get() = session.database?.typeSchema()
    private val ruleSchema: String? get() = session.database?.ruleSchema()
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    init {
        session.onOpen { mayUpdateRoot() }
        session.transaction.onSchemaWrite {
            session.resetSchemaReadTx()
            mayUpdateRoot()
        }
    }

    private fun mayUpdateRoot() {
        session.transaction()?.let { tx ->
            root = TypeState(tx.concepts().rootThingType, null, this, true)
            onRootChange?.let { it(root!!) }
        }
    }

    fun exportTypeSchema(onSuccess: (String) -> Unit) = coroutineScope.launch {
        typeSchema?.let { onSuccess(it) }
    }
}