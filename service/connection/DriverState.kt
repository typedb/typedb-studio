/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.typedb.driver.TypeDB
import com.typedb.driver.api.Credentials
import com.typedb.driver.api.Driver
import com.typedb.driver.api.DriverOptions
import com.typedb.driver.api.Transaction
import com.typedb.driver.api.user.UserManager
import com.typedb.driver.common.exception.TypeDBDriverException
import com.typedb.studio.service.common.DataService
import com.typedb.studio.service.common.NotificationService
import com.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.typedb.studio.service.common.PreferenceService
import com.typedb.studio.service.common.atomic.AtomicBooleanState
import com.typedb.studio.service.common.atomic.AtomicReferenceState
import com.typedb.studio.service.common.util.DialogState
import com.typedb.studio.service.common.util.Message
import com.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_CREATE_DATABASE
import com.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE
import com.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_DELETE_DATABASE
import com.typedb.studio.service.common.util.Message.Connection.Companion.RECONNECTED_WITH_NEW_PASSWORD_SUCCESSFULLY
import com.typedb.studio.service.common.util.Message.Connection.Companion.UNABLE_TO_CONNECT
import com.typedb.studio.service.common.util.Message.Connection.Companion.UNEXPECTED_ERROR
import com.typedb.studio.service.common.util.Property.Server.TYPEDB_CLOUD
import com.typedb.studio.service.common.util.Property.Server.TYPEDB_CORE
import com.typedb.studio.service.connection.DriverState.Status.CONNECTED
import com.typedb.studio.service.connection.DriverState.Status.CONNECTING
import com.typedb.studio.service.connection.DriverState.Status.DISCONNECTED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mu.KotlinLogging

class DriverState(
    private val notificationSrv: NotificationService,
    preferenceSrv: PreferenceService,
    private val dataSrv: DataService
) {

    enum class Status { DISCONNECTED, CONNECTED, CONNECTING }
    enum class Mode { SCRIPT, INTERACTIVE }

    companion object {
        private const val DATABASE_LIST_REFRESH_RATE_MS = 100
        private val LOGGER = KotlinLogging.logger {}
    }

    val connectServerDialog = DialogState.Base()
    val selectDBDialog = DialogState.Base()
    val manageDatabasesDialog = DialogState.Base()
    val manageAddressesDialog = DialogState.Base()
    val status: Status get() = statusAtomic.state
    val isConnected: Boolean get() = status == CONNECTED
    val isConnecting: Boolean get() = status == CONNECTING
    val isDisconnected: Boolean get() = status == DISCONNECTED
    var connectionName: String? by mutableStateOf(null)
    var mode: Mode by mutableStateOf(Mode.INTERACTIVE)
    val isScriptMode: Boolean get() = mode == Mode.SCRIPT
    val isInteractiveMode: Boolean get() = mode == Mode.INTERACTIVE
    val hasRunningQuery get() = transaction.hasRunningQuery
    val hasRunningCommand get() = hasRunningCommandAtomic.state
    val isReadyToRunQuery get() = transaction.isOpen && !hasRunningQuery && !hasRunningCommand
    var databaseList: List<String> by mutableStateOf(emptyList()); private set
    val transaction = TransactionState(this, notificationSrv, preferenceSrv)
    val userManager: UserManager? get() = _driver?.users()
    private val statusAtomic = AtomicReferenceState(DISCONNECTED)
    private var _driver: Driver? by mutableStateOf(null)
    private var hasRunningCommandAtomic = AtomicBooleanState(false)
    private var databaseListRefreshedTime = System.currentTimeMillis()
    internal var isCloud: Boolean = false

    private val coroutines = CoroutineScope(Dispatchers.Default)

    private fun connectionName(username: String, address: String) = "$username@$address"

    internal fun tryGet(): Driver? = _driver

    fun tryConnectToTypeDBCoreAsync(
        address: String,
        username: String, password: String,
        tlsEnabled: Boolean, caCertificate: String,
        onSuccess: () -> Unit
    ): Any = tryConnectAsync(connectionName(username, address), onSuccess) {
        TypeDB.coreDriver(address, Credentials(username, password), DriverOptions(tlsEnabled, caCertificate))
    }

    fun tryConnectToTypeDBCloudAsync(
        addresses: Set<String>,
        username: String, password: String,
        tlsEnabled: Boolean, caCertificate: String,
        onSuccess: () -> Unit
    ): Any = tryConnectAsync(connectionName(username, addresses.first()), onSuccess) {
        TypeDB.cloudDriver(addresses, Credentials(username, password), DriverOptions(tlsEnabled, caCertificate))
    }

    fun tryConnectToTypeDBCloudAsync(
        addressTranslation: Map<String, String>,
        username: String, password: String,
        tlsEnabled: Boolean, caCertificate: String,
        onSuccess: () -> Unit
    ): Any = tryConnectAsync(connectionName(username, addressTranslation.values.first()), onSuccess) {
        TypeDB.cloudDriver(
            addressTranslation,
            Credentials(username, password),
            DriverOptions(tlsEnabled, caCertificate)
        )
    }

    private fun tryConnectAsync(
        newConnectionName: String, onSuccess: () -> Unit, driverConstructor: () -> Driver
    ): Any = coroutines.launchAndHandle(notificationSrv, LOGGER) {
        if (isConnecting || isConnected) return@launchAndHandle
        statusAtomic.set(CONNECTING)
        try {
            connectionName = newConnectionName
            _driver = driverConstructor()
            statusAtomic.set(CONNECTED)
            onSuccess()

        } catch (e: TypeDBDriverException) {
            statusAtomic.set(DISCONNECTED)
            notificationSrv.userError(LOGGER, UNABLE_TO_CONNECT, e.message ?: "")
        } catch (e: java.lang.Exception) {
            statusAtomic.set(DISCONNECTED)
            notificationSrv.systemError(LOGGER, e, UNEXPECTED_ERROR)
        }
    }

    private fun mayRunCommandAsync(function: () -> Unit) {
        if (hasRunningCommandAtomic.compareAndSet(expected = false, new = true)) {
            coroutines.launchAndHandle(notificationSrv, LOGGER) { function() }.invokeOnCompletion {
                hasRunningCommandAtomic.set(false)
            }
        }
    }

    fun tryReconnectAsync(newPassword: String) {
        close()
        val username = dataSrv.connection.username!!
        val tlsEnabled = dataSrv.connection.tlsEnabled!!
        val caCertificate = dataSrv.connection.caCertificate!!
        val onSuccess = {
            notificationSrv.info(LOGGER, RECONNECTED_WITH_NEW_PASSWORD_SUCCESSFULLY)
        }
        when (dataSrv.connection.server!!) {
            TYPEDB_CORE -> tryConnectToTypeDBCoreAsync(
                dataSrv.connection.coreAddress!!,
                username, newPassword, tlsEnabled, caCertificate, onSuccess
            )
            TYPEDB_CLOUD -> when {
                dataSrv.connection.useCloudAddressTranslation!! -> tryConnectToTypeDBCloudAsync(
                    dataSrv.connection.cloudAddressTranslation!!.toMap(),
                    username, newPassword, tlsEnabled, caCertificate, onSuccess
                )
                else -> tryConnectToTypeDBCloudAsync(
                    dataSrv.connection.cloudAddresses!!.toSet(),
                    username, newPassword, tlsEnabled, caCertificate, onSuccess
                )
            }
        }
    }

    fun sendStopSignal() {
        transaction.sendStopSignal()
    }

    fun tryUpdateTransactionType(type: Transaction.Type) = mayRunCommandAsync {
        if (transaction.type == type) return@mayRunCommandAsync
        transaction.close()
        transaction.type = type
    }

    fun refreshDatabaseList() = mayRunCommandAsync { refreshDatabaseListFn() }

    private fun refreshDatabaseListFn() {
        if (System.currentTimeMillis() - databaseListRefreshedTime < DATABASE_LIST_REFRESH_RATE_MS) return
        _driver?.let { c -> databaseList = c.databases().all().map { d -> d.name() }.sorted() }
        databaseListRefreshedTime = System.currentTimeMillis()
    }

    fun run(content: String): QueryRunner? {
        return if (!isReadyToRunQuery) null
        else if (isScriptMode) runScript(content)
        else if (isInteractiveMode) transaction.runQuery(content)
        else throw IllegalStateException("Unrecognised TypeDB Studio run mode")
    }

    private fun runScript(content: String): QueryRunner? {
        return null // TODO
    }

    fun tryCreateDatabase(database: String, onSuccess: () -> Unit) = mayRunCommandAsync {
        refreshDatabaseListFn()
        if (!databaseList.contains(database)) {
            try {
                _driver?.databases()?.create(database)
                refreshDatabaseListFn()
                onSuccess()
            } catch (e: Exception) {
                notificationSrv.userError(LOGGER, FAILED_TO_CREATE_DATABASE, database, e.message ?: e.toString())
            }
        } else notificationSrv.userError(LOGGER, FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE, database)
    }

    fun tryDeleteDatabase(database: String) = mayRunCommandAsync {
        try {
            if (transaction.database == database) transaction.close()
            _driver?.databases()?.get(database)?.delete()
            refreshDatabaseListFn()
        } catch (e: Exception) {
            notificationSrv.userWarning(LOGGER, FAILED_TO_DELETE_DATABASE, database, e.message ?: e.toString())
        }
    }

    fun tryFetchSchema(database: String): String? = _driver?.databases()?.get(database)?.schema()

    fun tryFetchTypeSchema(database: String): String? = _driver?.databases()?.get(database)?.typeSchema()

    fun commitTransaction() = mayRunCommandAsync { transaction.commit() }

    fun rollbackTransaction() = mayRunCommandAsync { transaction.rollback() }

    fun closeTransactionAsync(
        message: Message? = null, vararg params: Any
    ) = coroutines.launchAndHandle(notificationSrv, LOGGER) { transaction.close(message, *params) }

    fun closeAsync() = coroutines.launchAndHandle(notificationSrv, LOGGER) { close() }

    fun close() {
        if (
            statusAtomic.compareAndSet(expected = CONNECTED, new = DISCONNECTED) ||
            statusAtomic.compareAndSet(expected = CONNECTING, new = DISCONNECTED)
        ) {
            transaction.close()
            _driver?.close()
            _driver = null
        }
    }
}
