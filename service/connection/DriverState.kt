/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.driver.TypeDB
import com.vaticle.typedb.driver.api.TypeDBCredential
import com.vaticle.typedb.driver.api.TypeDBDriver
import com.vaticle.typedb.driver.api.TypeDBSession
import com.vaticle.typedb.driver.api.TypeDBSession.Type.DATA
import com.vaticle.typedb.driver.api.TypeDBTransaction
import com.vaticle.typedb.driver.common.exception.TypeDBDriverException
import com.vaticle.typedb.studio.service.common.DataService
import com.vaticle.typedb.studio.service.common.NotificationService
import com.vaticle.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.service.common.PreferenceService
import com.vaticle.typedb.studio.service.common.atomic.AtomicBooleanState
import com.vaticle.typedb.studio.service.common.atomic.AtomicReferenceState
import com.vaticle.typedb.studio.service.common.util.DialogState
import com.vaticle.typedb.studio.service.common.util.Message
import com.vaticle.typedb.studio.service.common.util.Message.Connection.Companion.CREDENTIALS_EXPIRE_SOON_HOURS
import com.vaticle.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_CREATE_DATABASE
import com.vaticle.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_DELETE_DATABASE
import com.vaticle.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_UPDATE_PASSWORD
import com.vaticle.typedb.studio.service.common.util.Message.Connection.Companion.PASSWORD_UPDATED_SUCCESSFULLY
import com.vaticle.typedb.studio.service.common.util.Message.Connection.Companion.UNABLE_TO_CONNECT
import com.vaticle.typedb.studio.service.common.util.Message.Connection.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.service.connection.DriverState.Status.CONNECTED
import com.vaticle.typedb.studio.service.connection.DriverState.Status.CONNECTING
import com.vaticle.typedb.studio.service.connection.DriverState.Status.DISCONNECTED
import java.nio.file.Path
import java.time.Duration
import java.util.Optional
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
        private val PASSWORD_EXPIRY_WARN_DURATION = Duration.ofDays(7)
        private val LOGGER = KotlinLogging.logger {}
    }

    class ChangeInitialPasswordDialog(val driver: DriverState) : DialogState() {

        var onCancel : (() -> Unit)? by mutableStateOf(null); private set
        var onSubmit : ((old: String, new: String) -> Unit)? by mutableStateOf(null); private set

        internal fun open(onCancel: (() -> Unit)?, onSubmit: ((old: String, new: String) -> Unit)?) {
            this.onCancel = onCancel
            this.onSubmit = onSubmit
            isOpen = true
        }

        override fun close() {
            isOpen = false
        }

        fun cancel() {
            onCancel?.invoke()
            close()
        }

        fun submit(old: String, new: String) {
            onSubmit?.invoke(old, new)
            close()
        }
    }

    val connectServerDialog = DialogState.Base()
    val selectDBDialog = DialogState.Base()
    val manageDatabasesDialog = DialogState.Base()
    val manageAddressesDialog = DialogState.Base()
    val updateDefaultPasswordDialog = ChangeInitialPasswordDialog(this)
    val status: Status get() = statusAtomic.state
    val isConnected: Boolean get() = status == CONNECTED
    val isConnecting: Boolean get() = status == CONNECTING
    val isDisconnected: Boolean get() = status == DISCONNECTED
    var connectionName: String? by mutableStateOf(null)
    var mode: Mode by mutableStateOf(Mode.INTERACTIVE)
    val isScriptMode: Boolean get() = mode == Mode.SCRIPT
    val isInteractiveMode: Boolean get() = mode == Mode.INTERACTIVE
    val hasRunningQuery get() = session.transaction.hasRunningQuery
    val hasRunningCommand get() = hasRunningCommandAtomic.state
    val isReadyToRunQuery get() = session.isOpen && !hasRunningQuery && !hasRunningCommand
    var databaseList: List<String> by mutableStateOf(emptyList()); private set
    val session = SessionState(this, notificationSrv, preferenceSrv)
    private val statusAtomic = AtomicReferenceState(DISCONNECTED)
    private var _driver: TypeDBDriver? by mutableStateOf(null)
    private var hasRunningCommandAtomic = AtomicBooleanState(false)
    private var databaseListRefreshedTime = System.currentTimeMillis()
    internal var isCloud: Boolean = false

    private val coroutines = CoroutineScope(Dispatchers.Default)

    fun tryConnectToTypeDBCoreAsync(
        address: String, onSuccess: () -> Unit
    ) = tryConnectAsync(newConnectionName = address, onSuccess = onSuccess) { TypeDB.coreDriver(address) }

    fun tryConnectToTypeDBCloudAsync(
        connectionName: String, addresses: Set<String>, credentials: TypeDBCredential, onSuccess: (() -> Unit)? = null
    ) {
        val postLoginFn = {
            onSuccess?.invoke()
            if (needsToChangeDefaultPassword()) forcePasswordUpdate()
            else mayWarnPasswordExpiry()
        }
        tryConnectAsync(newConnectionName = connectionName, postLoginFn) { TypeDB.cloudDriver(addresses, credentials) }
    }

    fun tryConnectToTypeDBCloudAsync(
        connectionName: String, addressTranslation: Map<String, String>, credentials: TypeDBCredential, onSuccess: (() -> Unit)? = null
    ) {
        val postLoginFn = {
            onSuccess?.invoke()
            if (needsToChangeDefaultPassword()) forcePasswordUpdate()
            else mayWarnPasswordExpiry()
        }
        tryConnectAsync(newConnectionName = connectionName, postLoginFn) { TypeDB.cloudDriver(addressTranslation, credentials) }
    }

    private fun forcePasswordUpdate() = updateDefaultPasswordDialog.open(
        onCancel = {
            close()
            connectServerDialog.open()
        }
    ) { old, new ->
        tryUpdateUserPassword(old, new) {
            updateDefaultPasswordDialog.close()
            close()

            val username = dataSrv.connection.username!!
            val password = new
            val credentials = if (dataSrv.connection.caCertificate!!.isBlank()) TypeDBCredential(username, password, dataSrv.connection.tlsEnabled!!)
                else TypeDBCredential(username, password, Path.of(dataSrv.connection.caCertificate!!))
            val onSuccess = { notificationSrv.info(LOGGER, Message.Connection.RECONNECTED_WITH_NEW_PASSWORD_SUCCESSFULLY) }

            if (dataSrv.connection.useCloudAddressTranslation == true)
                tryConnectToTypeDBCloudAsync(connectionName!!, dataSrv.connection.cloudAddressTranslation!!.associate { it }, credentials, onSuccess)
            else
                tryConnectToTypeDBCloudAsync(connectionName!!, dataSrv.connection.cloudAddresses!!.toSet(), credentials, onSuccess)
        }
    }

    private fun tryConnectAsync(
        newConnectionName: String, onSuccess: () -> Unit, driverConstructor: () -> TypeDBDriver
    ) = coroutines.launchAndHandle(notificationSrv, LOGGER) {
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

    private fun mayWarnPasswordExpiry() {
        if (!this.isCloud) return
        val passwordExpiryDurationOptional: Optional<Duration>? =
            _driver?.user()?.passwordExpirySeconds()?.map { Duration.ofSeconds(it) }
        if (passwordExpiryDurationOptional?.isPresent != true) return
        val passwordExpiryDuration = passwordExpiryDurationOptional.get()
        if (passwordExpiryDuration.minus(PASSWORD_EXPIRY_WARN_DURATION).isNegative) {
            notificationSrv.userWarning(LOGGER, CREDENTIALS_EXPIRE_SOON_HOURS, passwordExpiryDuration.toHours() + 1)
        }
    }

    // TODO: We need a proper way to check if default password needs to changed
    fun needsToChangeDefaultPassword(): Boolean {
        try {
            _driver?.databases()?.all()
        } catch (e: TypeDBDriverException) {
            val errorString = e.toString()
            return errorString.contains("ENT21") || errorString.contains("CLS21")
        }
        return false
    }

    fun tryUpdateUserPassword(passwordOld: String, passwordNew: String, onSuccess: (() -> Unit)?) {
        try {
            _driver?.user()?.passwordUpdate(passwordOld, passwordNew)
            notificationSrv.info(LOGGER, PASSWORD_UPDATED_SUCCESSFULLY)
            onSuccess?.invoke()
        } catch (e: TypeDBDriverException) {
            notificationSrv.userError(LOGGER, FAILED_TO_UPDATE_PASSWORD, e.message ?: e.toString())
        }
    }

    fun sendStopSignal() {
        session.transaction.sendStopSignal()
    }

    fun tryUpdateTransactionType(type: TypeDBTransaction.Type) = mayRunCommandAsync {
        if (session.transaction.type == type) return@mayRunCommandAsync
        session.transaction.close()
        session.transaction.type = type
    }

    fun tryUpdateSessionType(type: TypeDBSession.Type) {
        if (session.type == type) return
        tryOpenSession(session.database!!, type)
    }

    fun tryOpenSession(database: String) = tryOpenSession(database, session.type)

    private fun tryOpenSession(database: String, type: TypeDBSession.Type) = mayRunCommandAsync {
        session.tryOpen(database, type)
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
        else if (isInteractiveMode) session.transaction.runQuery(content)
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
            if (session.database == database) session.close()
            _driver?.databases()?.get(database)?.delete()
            refreshDatabaseListFn()
        } catch (e: Exception) {
            notificationSrv.userWarning(LOGGER, FAILED_TO_DELETE_DATABASE, database, e.message ?: e.toString())
        }
    }

    fun tryFetchSchema(database: String): String? = _driver?.databases()?.get(database)?.schema()

    fun tryFetchTypeSchema(database: String): String? = _driver?.databases()?.get(database)?.typeSchema()

    fun session(database: String, type: TypeDBSession.Type = DATA): TypeDBSession? {
        return _driver?.session(database, type)
    }

    fun commitTransaction() = mayRunCommandAsync { session.transaction.commit() }

    fun rollbackTransaction() = mayRunCommandAsync { session.transaction.rollback() }

    fun closeSession() = coroutines.launchAndHandle(notificationSrv, LOGGER) { session.close() }

    fun closeTransactionAsync(
        message: Message? = null, vararg params: Any
    ) = coroutines.launchAndHandle(notificationSrv, LOGGER) { session.transaction.close(message, *params) }

    fun closeAsync() = coroutines.launchAndHandle(notificationSrv, LOGGER) { close() }

    fun close() {
        if (
            statusAtomic.compareAndSet(expected = CONNECTED, new = DISCONNECTED) ||
            statusAtomic.compareAndSet(expected = CONNECTING, new = DISCONNECTED)
        ) {
            session.close()
            _driver?.close()
            _driver = null
        }
    }
}
