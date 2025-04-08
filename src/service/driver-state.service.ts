/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, catchError, map, Observable, of, switchMap, tap } from "rxjs";
import { ConnectionConfig, Database, databasesSortedByName, DEFAULT_DATABASE_NAME, remoteOrigin } from "../concept/connection";
import { Transaction, TransactionType } from "../concept/transaction";
import { requireValue } from "../framework/util/observable";
import { INTERNAL_ERROR } from "../framework/util/strings";

export type DriverStatus = "initial" | "connecting" | "connected" | "reconnecting";

interface SignInResponse {
    token: string;
}

interface DatabasesListResponse {
    databases: Database[];
}

interface TransactionOpenResponse {
    transactionId: string;
}

const HTTP_UNAUTHORIZED = 401;

@Injectable({
    providedIn: "root",
})
export class DriverStateService {

    private _config$ = new BehaviorSubject<ConnectionConfig | null>(null);
    private _status$ = new BehaviorSubject<DriverStatus>("initial");
    private token?: string;
    private _database$ = new BehaviorSubject<Database | null>(null);
    private _databaseList$ = new BehaviorSubject<Database[] | null>(null);
    private _transaction$ = new BehaviorSubject<Transaction | null>(null);

    constructor(private http: HttpClient) {}

    get status$(): Observable<DriverStatus> {
        return this._status$;
    }

    get config$(): Observable<ConnectionConfig | null> {
        return this._config$;
    }

    get database$(): Observable<Database | null> {
        return this._database$;
    }

    get databaseList$(): Observable<Database[] | null> {
        return this._databaseList$;
    }

    get transaction$(): Observable<Transaction | null> {
        return this._transaction$;
    }

    private requireConfig() {
        return requireValue(this._config$);
    }

    private requireDatabase() {
        return requireValue(this._database$);
    }

    private requireDatabaseList() {
        return requireValue(this._databaseList$);
    }

    private requireTransaction() {
        return requireValue(this._transaction$);
    }

    tryConnectAndSetupDatabases(config: ConnectionConfig) {
        if (this._status$.value !== "initial") throw INTERNAL_ERROR;
        this._config$.next(config);
        this._status$.next("connecting");

        return this.refreshDatabaseList().pipe(
            switchMap((res) => {
                if (!res.databases.length) return this.setupDefaultDatabase();
                if (res.databases.length === 1) this.selectDatabase(res.databases[0]);
                return of(undefined); // TODO: connect to designated default database for this server
            }),
            tap(() => this._status$.next("connected")),
            catchError((err) => {
                this._status$.next("initial");
                throw err;
            }),
        );
    }

    private checkHealth() {
        return this.apiGet(`/v1/health`);
    }

    private refreshDatabaseList() {
        return this.apiGet<DatabasesListResponse>(`/v1/databases`).pipe(
            tap((res) => this._databaseList$.next(databasesSortedByName(res.databases))),
        );
    }

    private createDatabase(name: string) {
        return this.apiPost(`/v1/databases/${name}`, {}).pipe(
            tap(() => {
                const databaseList = this.requireDatabaseList();
                this._databaseList$.next(databasesSortedByName([...databaseList, { name }]));
            }),
        )
    }

    private setupDefaultDatabase() {
        return this.createDatabase(DEFAULT_DATABASE_NAME).pipe(
            tap(() => {
                const databaseList = this.requireDatabaseList();
                this.selectDatabase(databaseList[0])
            })
        );
    }

    selectDatabase(database: Database) {
        const savedDatabase = this._databaseList$.value?.find(x => x.name === database.name);
        if (!savedDatabase) throw INTERNAL_ERROR;
        this._database$.next(savedDatabase);
    }

    openTransaction(type: TransactionType) {
        const databaseName = this.requireDatabase().name;
        return this.apiPost<TransactionOpenResponse>(`/v1/transactions/open`, { databaseName, transactionType: type }).pipe(
            tap((res) => {
                this._transaction$.next(new Transaction({ id: res.transactionId, type: type }));
            })
        );
    }

    commitTransaction() {
        const transactionId = this.requireTransaction().id;
        return this.apiPost(`/v1/transactions/${transactionId}/commit`, {}).pipe(
            tap(() => {
                this._transaction$.next(null);
            })
        );
    }

    closeTransaction() {
        const transactionId = this.requireTransaction().id;
        return this.apiPost(`/v1/transactions/${transactionId}/close`, {}).pipe(
            tap(() => {
                this._transaction$.next(null);
            })
        );
    }

    private apiGet<RES = Object>(path: string, options?: { headers?: Record<string, string> }) {
        const { params } = this.requireConfig();
        const url = `${remoteOrigin(params)}${path}`;
        return this.getToken().pipe(
            switchMap((resp) => {
                const opts = { headers: Object.assign({ "Authorization": `Bearer ${resp.token}` }, options?.headers || {})};
                return this.http.get<RES>(url, opts);
            }),
            catchError((err) => {
                if (err instanceof HttpErrorResponse && err.status === HTTP_UNAUTHORIZED) {
                    return this.refreshToken().pipe(switchMap((resp) => {
                        const opts = { headers: Object.assign({ "Authorization": `Bearer ${resp.token}` }, options?.headers || {})};
                        return this.http.get<RES>(url, opts);
                    }));
                } else throw err;
            }),
        );
    }

    private apiPost<RES = Object, BODY = Object>(path: string, body: BODY, options?: { headers?: Record<string, string>}) {
        const { params } = this.requireConfig();
        const url = `${remoteOrigin(params)}${path}`;
        return this.getToken().pipe(
            switchMap((resp) => {
                const opts = { headers: Object.assign({ "Authorization": `Bearer ${resp.token}` }, options?.headers || {})};
                return this.http.post<RES>(url, body, opts);
            }),
            catchError((err) => {
                if (err instanceof HttpErrorResponse && err.status === HTTP_UNAUTHORIZED) {
                    return this.refreshToken().pipe(switchMap((resp) => {
                        const opts = { headers: Object.assign({ "Authorization": `Bearer ${resp.token}` }, options?.headers || {})};
                        return this.http.post<RES>(url, body, opts);
                    }));
                } else throw err;
            }),
        );
    }

    private getToken() {
        if (this.token) {
            const resp: SignInResponse = { token: this.token };
            return of(resp);
        } else return this.refreshToken();
    }

    private refreshToken() {
        const { params } = this.requireConfig();
        return this.http.post<SignInResponse>(
            `${remoteOrigin(params)}/v1/signin`, { username: params.username, password: params.password }
        ).pipe(
            tap((resp) => this.token = resp.token)
        );
    }
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
