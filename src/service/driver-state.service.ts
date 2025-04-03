/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, map, switchMap, tap } from "rxjs";
import { ConnectionConfig, ConnectionParams, isBasicParams } from "../concept/connection";

export type DriverStatus = "connected" | "connecting" | "disconnected";

export interface DriverConfig {
    params: ConnectionParams;
}

function driverConfigOf(connectionConfig: ConnectionConfig): DriverConfig {
    return { params: connectionConfig.params };
}

export class Driver {

    private _status: DriverStatus = "disconnected";
    private _config?: DriverConfig;
    private api: Api;

    constructor(private _http: HttpClient) {}

    get status() {
        return this._status;
    }

    get config() {
        return this._config;
    }

    private set status(value: DriverStatus) {
        this._status = value;
    }

    private set config(value: DriverConfig | undefined) {
        this._config = value;
    }

    tryConnect(config: ConnectionConfig) {
        if (this.status !== "disconnected") throw `Internal error`; // TODO: error / report
        this.status = "connecting";
        this.config = driverConfigOf(config);
        return this.getAuthToken().pipe(
            tap((resp) => this.token = resp.token),
            switchMap(() => this.checkHealth()),
            catchError((err) => {
                this.status = "disconnected";
                throw err;
            }),
            tap(() => this.status = "connected")
        );
    }

    private getAuthToken() {
        if (!this.config) throw `Internal error`;
        return this.http.post<SignInResponse>(
            `${this.remoteOrigin}/v1/signin`,
            { username: this.config.params.username, password: this.config.params.password, }
        );
    }

    private checkHealth() {
        if (!this.config || !this.token) throw `Internal error`;
        return this.http.get(`${this.remoteOrigin}/v1/health`, {
            headers: { "Authorization": `Bearer ${this.token}` },
        });
    }

    private httpGet<RES = Object>(url: string, options?: { headers?: Record<string, string> }) {
        return this._http
    }

    private get remoteOrigin(): string {
        if (!this.config) throw `missing connection config`; // TODO: handle / report
        const params = this.config.params;
        if (isBasicParams(params)) return `http://${params.addresses[0]}`;
        else return `http://${params.translatedAddresses[0].external}`; // TODO: ???
    }
}

class Api {

    constructor(private driver: Driver, private http: HttpClient) {}


}

// private fun mayRunCommandAsync(function: () -> Unit) {
//     if (hasRunningCommandAtomic.compareAndSet(expected = false, new = true)) {
//         coroutines.launchAndHandle(notificationSrv, LOGGER) { function() }.invokeOnCompletion {
//             hasRunningCommandAtomic.set(false)
//         }
//     }
// }
//
// fun tryReconnectAsync(newPassword: String) {
//     close()
//     val username = dataSrv.connection.username!!
//     val tlsEnabled = dataSrv.connection.tlsEnabled!!
//     val caCertificate = dataSrv.connection.caCertificate!!
//     val onSuccess = {
//         notificationSrv.info(LOGGER, RECONNECTED_WITH_NEW_PASSWORD_SUCCESSFULLY)
//     }
//
//     val address = when (dataSrv.connection.server!!) {
//         TYPEDB_CORE -> dataSrv.connection.coreAddress!!
//         // Cloud features are not available, just get the first available address and return an error in worst case scenario
//         TYPEDB_CLOUD -> when {
//             dataSrv.connection.useCloudAddressTranslation!! -> dataSrv.connection.cloudAddressTranslation!!.first().first
//         else -> dataSrv.connection.cloudAddresses!!.first()
//         }
//     }
//     tryConnectToTypeDBAsync(
//         address, username, newPassword, tlsEnabled, caCertificate, onSuccess
//     )
// }
//
// fun sendStopSignal() {
//     transaction.sendStopSignal()
// }
//
// fun tryUpdateTransactionType(type: Transaction.Type) = mayRunCommandAsync {
//     if (transaction.type == type) return@mayRunCommandAsync
//     transaction.close()
//     transaction.type = type
// }
//
// fun refreshDatabaseList() = mayRunCommandAsync { refreshDatabaseListFn() }
//
// private fun refreshDatabaseListFn() {
//     if (System.currentTimeMillis() - databaseListRefreshedTime < DATABASE_LIST_REFRESH_RATE_MS) return
//     _driver?.let { c -> databaseList = c.databases().all().map { d -> d.name() }.sorted() }
//     databaseListRefreshedTime = System.currentTimeMillis()
// }
//
// fun run(content: String): QueryRunner? {
//     return if (!isReadyToRunQuery) null
// else if (isScriptMode) runScript(content)
// else if (isInteractiveMode) transaction.runQuery(content)
// else throw IllegalStateException("Unrecognised TypeDB Studio run mode")
// }
//
// private fun runScript(content: String): QueryRunner? {
//         return null // TODO
//     }
//
//     fun tryCreateDatabase(database: String, onSuccess: () -> Unit) = mayRunCommandAsync {
//     refreshDatabaseListFn()
//     if (!databaseList.contains(database)) {
//         try {
//             _driver?.databases()?.create(database)
//             refreshDatabaseListFn()
//             onSuccess()
//         } catch (e: Exception) {
//             notificationSrv.userError(LOGGER, FAILED_TO_CREATE_DATABASE, database, e.message ?: e.toString())
//         }
//     } else notificationSrv.userError(LOGGER, FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE, database)
// }
//
// fun tryDeleteDatabase(database: String) = mayRunCommandAsync {
//     try {
//         if (transaction.database == database) transaction.close()
//         _driver?.databases()?.get(database)?.delete()
//         refreshDatabaseListFn()
//     } catch (e: Exception) {
//         notificationSrv.userWarning(LOGGER, FAILED_TO_DELETE_DATABASE, database, e.message ?: e.toString())
//     }
// }
//
// fun tryFetchSchema(database: String): String? = _driver?.databases()?.get(database)?.schema()
//
//     fun tryFetchTypeSchema(database: String): String? = _driver?.databases()?.get(database)?.typeSchema()
//
//     fun commitTransaction() = mayRunCommandAsync { transaction.commit() }
//
// fun rollbackTransaction() = mayRunCommandAsync { transaction.rollback() }
//
// fun closeTransactionAsync(
//     message: Message? = null, vararg params: Any
// ) = coroutines.launchAndHandle(notificationSrv, LOGGER) { transaction.close(message, *params) }
//
// fun closeAsync() = coroutines.launchAndHandle(notificationSrv, LOGGER) { close() }
//
// fun close() {
//     if (
//         statusAtomic.compareAndSet(expected = CONNECTED, new = DISCONNECTED) ||
//         statusAtomic.compareAndSet(expected = CONNECTING, new = DISCONNECTED)
//     ) {
//         transaction.close()
//         _driver?.close()
//         _driver = null
//     }
// }
