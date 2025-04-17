/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, catchError, map, Observable, of, Subject, switchMap, tap } from "rxjs";
import { ConnectionConfig, Database, databasesSortedByName, DEFAULT_DATABASE_NAME, remoteOrigin } from "../concept/connection";
import { QueryRun, Transaction, TransactionOperation, TransactionType } from "../concept/transaction";
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

interface DriverActionBase {
    timestamp: number;
    actionType: string;
}

export interface QueryRunAction extends DriverActionBase {
    actionType: "queryRun";
    queryRun: QueryRun;
}

export interface TransactionOperationAction extends DriverActionBase {
    actionType: "transactionOperation";
    transactionOperation: TransactionOperation;
}

function queryRunActionOf(queryRun: QueryRun): QueryRunAction {
    return { actionType: "queryRun", queryRun: queryRun, timestamp: Date.now() };
}

function transactionOperationActionOf(operation: TransactionOperation): TransactionOperationAction {
    return { actionType: "transactionOperation", transactionOperation: operation, timestamp: Date.now() };
}

export type DriverAction = QueryRunAction | TransactionOperationAction;

export function isQueryRun(entry: DriverAction): entry is QueryRunAction {
    return entry.actionType === "queryRun";
}

export function isTransactionOperation(entry: DriverAction): entry is TransactionOperationAction {
    return entry.actionType === "transactionOperation";
}

export type TypeKind = "entityType" | "relationType" | "attributeType" | "roleType";

export type ThingKind = "entity" | "relation" | "attribute";

export type ValueKind = "value";

export type ValueType = "boolean" | "integer" | "double" | "decimal" | "date" | "datetime" | "datetime-tz" | "duration" | "string" | "struct";

export interface EntityType {
    kind: "entityType";
    label: string;
}

export interface RelationType {
    kind: "relationType";
    label: string;
}

export interface RoleType {
    kind: "roleType";
    label: string;
}

export type AttributeType = {
    label: string,
    kind: "attributeType";
    valueType: ValueType;
}

export type Type = EntityType | RelationType | RoleType | AttributeType;

export interface Entity {
    kind: "entity";
    iid: string,
    type: EntityType;
}

export interface Relation {
    kind: "relation";
    iid: string;
    type: RelationType;
}

export interface Attribute {
    kind: "attribute";
    iid: string;
    value: any;
    valueType: ValueType;
    type: AttributeType;
}

export interface Value {
    kind: ValueKind;
    value: any;
    valueType: ValueType;
}

export type Concept = Type | Entity | Relation | Attribute | Value;

export interface QueryResponseBase {
    queryType: QueryType;
    answerType: AnswerType;
    comment: string | null;
}

export interface OkQueryResponse extends QueryResponseBase {
    answerType: "ok";
}

export interface ConceptRowsQueryResponse extends QueryResponseBase {
    answerType: "conceptRows";
    answers: ConceptRow[];
}

export interface ConceptDocumentsQueryResponse extends QueryResponseBase {
    answerType: "conceptDocuments";
    answers: ConceptDocument[];
}

export type QueryType = "read" | "write" | "schema";

export type AnswerType = "ok" | "conceptRows" | "conceptDocuments";

export interface ConceptRow {
    [varName: string]: Concept;
}

export type ConceptDocument = Object;

export type Answer = ConceptRow | ConceptDocument;

export type QueryResponse = OkQueryResponse | ConceptRowsQueryResponse | ConceptDocumentsQueryResponse;

@Injectable({
    providedIn: "root",
})
export class DriverState {

    private _connection$ = new BehaviorSubject<ConnectionConfig | null>(null);
    private _status$ = new BehaviorSubject<DriverStatus>("initial");
    private token?: string;
    private _database$ = new BehaviorSubject<Database | null>(null);
    private _databaseList$ = new BehaviorSubject<Database[] | null>(null);
    private _transaction$ = new BehaviorSubject<Transaction | null>(null);
    autoTransactionEnabled = true;
    private _actions$ = new Subject<DriverAction>();

    constructor(private http: HttpClient) {}

    get status$(): Observable<DriverStatus> {
        return this._status$;
    }

    get connection$(): Observable<ConnectionConfig | null> {
        return this._connection$;
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

    get actions$(): Observable<DriverAction> {
        return this._actions$;
    }

    private requireConfig() {
        return requireValue(this._connection$);
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
        this._connection$.next(config);
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
        const operation: TransactionOperation = { operationType: "open", status: "pending", startedAtTimestamp: Date.now() };
        const action = transactionOperationActionOf(operation);
        this._actions$.next(action);
        return this.apiPost<TransactionOpenResponse>(`/v1/transactions/open`, { databaseName, transactionType: type }).pipe(
            tap((res) => {
                this.updateTransactionOperationResult(operation, res);
                this._transaction$.next(new Transaction({ id: res.transactionId, type: type }));
            })
        );
    }

    commitTransaction() {
        const transactionId = this.requireTransaction().id;
        const operation: TransactionOperation = { operationType: "commit", status: "pending", startedAtTimestamp: Date.now() };
        const action = transactionOperationActionOf(operation);
        this._actions$.next(action);
        return this.apiPost(`/v1/transactions/${transactionId}/commit`, {}).pipe(
            tap((res) => {
                this.updateTransactionOperationResult(operation, res);
                this._transaction$.next(null);
            })
        );
    }

    closeTransaction() {
        const transactionId = this._transaction$.value?.id;
        if (transactionId == null) return of({});
        const operation: TransactionOperation = { operationType: "close", status: "pending", startedAtTimestamp: Date.now() };
        const action = transactionOperationActionOf(operation);
        this._actions$.next(action);
        return this.apiPost(`/v1/transactions/${transactionId}/close`, {}).pipe(
            tap((res) => {
                this.updateTransactionOperationResult(operation, res);
                this._transaction$.next(null);
            })
        );
    }

    query(query: string) {
        const maybeOpenTransaction$ = this.autoTransactionEnabled ? this.openTransaction("read") : of({ transactionId: this.requireTransaction().id });
        let queryRun: QueryRun;
        return maybeOpenTransaction$.pipe(
            tap(() => {
                const transaction = this.requireTransaction();
                queryRun = { query: query, status: "pending", startedAtTimestamp: Date.now() };
                transaction.queryRuns.push(queryRun);
                const queryRunAction: QueryRunAction = queryRunActionOf(queryRun);
                this._actions$.next(queryRunAction);
            }),
            switchMap((res) => this.apiPost<QueryResponse>(`/v1/transactions/${res.transactionId}/query`, { query })),
            tap((res) => this.updateQueryRunResult(queryRun, res)),
            // TODO: maybe extract TransactionStateService
            tap(() => {
                const transaction = this.requireTransaction();
                if (!this.autoTransactionEnabled) this._transaction$.next(transaction);
            }),
            tap(() => {
                if (this.autoTransactionEnabled) this.closeTransaction().subscribe();
            }),
            catchError((err) => {
                if (this.autoTransactionEnabled) this.closeTransaction().subscribe();
                throw err;
            }),
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

    private updateQueryRunResult(queryRun: QueryRun, result: Object) {
        queryRun.completedAtTimestamp = Date.now();
        queryRun.status = "success";
        queryRun.result = result;
    }

    private updateTransactionOperationResult(operation: TransactionOperation, result: Object) {
        operation.completedAtTimestamp = Date.now();
        operation.status = "success";
        operation.result = result;
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
