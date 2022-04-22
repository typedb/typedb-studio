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

package com.vaticle.typedb.studio.state.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.common.AtomicBooleanState
import com.vaticle.typedb.studio.state.common.Message
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_OPEN_SESSION
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.SESSION_CLOSED_ON_SERVER
import com.vaticle.typedb.studio.state.connection.TransactionState.Companion.ONE_HOUR_IN_MILLS
import com.vaticle.typedb.studio.state.notification.NotificationManager
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

@OptIn(ExperimentalTime::class)
class SessionState constructor(
    private val connection: Connection,
    internal val notificationMgr: NotificationManager
) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val SCHEMA_TYPE_TX_WAIT_TIME = Duration.seconds(16)
    }

    var type: TypeDBSession.Type by mutableStateOf(TypeDBSession.Type.DATA); internal set
    val isSchema: Boolean get() = type == TypeDBSession.Type.SCHEMA
    val isData: Boolean get() = type == TypeDBSession.Type.DATA
    val isOpen get() = isOpenAtomic.state
    val database: String? get() = _session?.database()?.name()
    var transaction = TransactionState(this, notificationMgr)
    var rootSchemaType: SchemaType? by mutableStateOf(null)
    var onSessionChange: ((SchemaType) -> Unit)? = null
    var onSchemaWrite: (() -> Unit)? = null
    private var _session: TypeDBSession? by mutableStateOf(null)
    private val isOpenAtomic = AtomicBooleanState(false)
    private var schemaReadTx: AtomicReference<TypeDBTransaction> = AtomicReference()
    private val lastSchemaReadTxTime = AtomicLong(0)
    private val typeSchema: String? get() = _session?.database()?.typeSchema()
    private val ruleSchema: String? get() = _session?.database()?.ruleSchema()
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    internal fun tryOpen(database: String, type: TypeDBSession.Type) {
        if (isOpen && this.database == database && this.type == type) return
        close()
        try {
            _session = connection.client.session(database, type).apply { onClose { close(SESSION_CLOSED_ON_SERVER) } }
            this.type = type
            transaction()?.let {
                rootSchemaType = SchemaType(it.concepts().rootThingType, null, this, true)
                it.close()
            }
            resetSchemaReadTx()
            onSessionChange?.let { it(rootSchemaType!!) }
            isOpenAtomic.set(true)
        } catch (exception: TypeDBClientException) {
            notificationMgr.userError(LOGGER, FAILED_TO_OPEN_SESSION, type, database)
            isOpenAtomic.set(false)
        }
    }

    internal fun transaction(type: TypeDBTransaction.Type = READ, options: TypeDBOptions? = null): TypeDBTransaction? {
        return if (options != null) _session?.transaction(type, options) else _session?.transaction(type)
    }

    internal fun openOrGetSchemaReadTx(): TypeDBTransaction {
        synchronized(this) {
            lastSchemaReadTxTime.set(System.currentTimeMillis())
            val options = TypeDBOptions.core().transactionTimeoutMillis(ONE_HOUR_IN_MILLS)
            if (schemaReadTx.compareAndSet(null, transaction(options = options))) {
                schemaReadTx.get().onClose { closeSchemaReadTx() }
                coroutineScope.launch {
                    var duration = SCHEMA_TYPE_TX_WAIT_TIME
                    while (true) {
                        delay(duration)
                        val sinceLastTx = System.currentTimeMillis() - lastSchemaReadTxTime.get()
                        if (sinceLastTx >= SCHEMA_TYPE_TX_WAIT_TIME.inWholeMilliseconds) {
                            closeSchemaReadTx()
                            break
                        } else duration = SCHEMA_TYPE_TX_WAIT_TIME - Duration.milliseconds(sinceLastTx)
                    }
                }
            }
            return schemaReadTx.get()
        }
    }

    fun exportTypeSchema(onSuccess: (String) -> Unit) = coroutineScope.launch {
        typeSchema?.let { onSuccess(it) }
    }

    fun resetSchemaReadTx() {
        synchronized(this) {
            if (schemaReadTx.get() != null) {
                closeSchemaReadTx()
                openOrGetSchemaReadTx()
            }
        }
    }

    private fun closeSchemaReadTx() {
        synchronized(this) { schemaReadTx.getAndSet(null)?.close() }
    }

    internal fun close(message: Message? = null, vararg params: Any) {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            closeSchemaReadTx()
            transaction.close()
            _session?.close()
            _session = null
            message?.let { notificationMgr.userError(LOGGER, it, *params) }
        }
    }
}