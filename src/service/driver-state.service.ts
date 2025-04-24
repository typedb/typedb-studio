/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, catchError, map, Observable, of, Subject, switchMap, takeUntil, tap } from "rxjs";
import { v4 as uuid } from "uuid";
import { ConnectionConfig, Database, databasesSortedByName, DEFAULT_DATABASE_NAME, remoteOrigin } from "../concept/connection";
import { QueryRun, Transaction, TransactionOperation, TransactionType } from "../concept/transaction";
import { requireValue } from "../framework/util/observable";
import { INTERNAL_ERROR } from "../framework/util/strings";

export type DriverStatus = "disconnected" | "connecting" | "connected" | "reconnecting";

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
    [varName: string]: Concept | undefined;
}

export type ConceptDocument = Object;

export type Answer = ConceptRow | ConceptDocument;

export type QueryResponse = OkQueryResponse | ConceptRowsQueryResponse | ConceptDocumentsQueryResponse;

interface Semaphore {
    id: string;
    count: number;
}

@Injectable({
    providedIn: "root",
})
export class DriverState {

    private _status$ = new BehaviorSubject<DriverStatus>("disconnected");
    private _connection$ = new BehaviorSubject<ConnectionConfig | null>(null);
    private _database$ = new BehaviorSubject<Database | null>(null);
    private _transaction$ = new BehaviorSubject<Transaction | null>(null);

    private _databaseList$ = new BehaviorSubject<Database[] | null>(null);
    private _actionLog$ = new Subject<DriverAction>();
    private _writeLock$ = new BehaviorSubject<Semaphore | null>(null);
    private _stopSignal$ = new Subject<void>();

    private token?: string;

    autoTransactionEnabled = true;
    transactionHasUncommittedChanges$ = this.transaction$.pipe(map(tx => tx?.hasUncommittedChanges ?? false));

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

    get actionLog$(): Observable<DriverAction> {
        return this._actionLog$;
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
        if (this._status$.value !== "disconnected") throw INTERNAL_ERROR;
        const lockId = uuid();
        return this.tryUseWriteLock(() => {
            this._connection$.next(config);
            this._status$.next("connecting");

            return this.refreshDatabaseList().pipe(
                switchMap((res) => {
                    if (!res.databases.length) return this.setupDefaultDatabase(lockId);
                    if (res.databases.length === 1) this.selectDatabase(res.databases[0]);
                    return of(undefined); // TODO: connect to designated default database for this server
                }),
                tap(() => this._status$.next("connected")),
                catchError((err) => {
                    this._status$.next("disconnected");
                    throw err;
                }),
            );
        }, lockId);
    }

    tryDisconnect() {
        if (this._status$.value === "disconnected") throw INTERNAL_ERROR;
        if (this._transaction$.value?.hasUncommittedChanges) throw INTERNAL_ERROR;
        const maybeCloseTransaction$ = this._transaction$.value ? this.closeTransaction() : of({});
        return maybeCloseTransaction$.pipe(tap(() => this.tryUseWriteLock(() => {
            this._connection$.next(null);
            this._database$.next(null);
            this._status$.next("disconnected");
        })));
    }

    refreshDatabaseList() {
        if (!this._connection$.value) throw INTERNAL_ERROR;
        return this.apiGet<DatabasesListResponse>(`/v1/databases`).pipe(
            tap((res) => this._databaseList$.next(databasesSortedByName(res.databases))),
        );
    }

    selectDatabase(database: Database, lockId?: string) {
        if (this._database$.value?.name === database.name) return;
        const savedDatabase = this._databaseList$.value?.find(x => x.name === database.name);
        if (!savedDatabase) throw INTERNAL_ERROR;
        else this.tryUseWriteLock(() => this._database$.next(savedDatabase), lockId);
    }

    createAndSelectDatabase(name: string, lockId?: string) {
        return this.tryUseWriteLock(() => this.apiPost(`/v1/databases/${name}`, {}).pipe(
            tap(() => {
                const databaseList = this.requireDatabaseList();
                this._databaseList$.next(databasesSortedByName([...databaseList, { name }]));
                this.selectDatabase({ name }, lockId);
            }),
        ), lockId);
    }

    openTransaction(type: TransactionType) {
        const databaseName = this.requireDatabase().name;
        const operation: TransactionOperation = { operationType: "open", status: "pending", startedAtTimestamp: Date.now() };
        const action = transactionOperationActionOf(operation);
        this._actionLog$.next(action);
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
        this._actionLog$.next(action);
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
        this._actionLog$.next(action);
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
                this._actionLog$.next(queryRunAction);
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

    sendStopSignal() {
        this._stopSignal$.next();
    }

    private checkHealth() {
        return this.apiGet(`/v1/health`);
    }

    private setupDefaultDatabase(lockId?: string) {
        return this.createAndSelectDatabase(DEFAULT_DATABASE_NAME, lockId);
    }

    private tryUseWriteLock<RES = any>(job: () => RES, lockId = uuid()): RES {
        if (this._writeLock$.value != null && this._writeLock$.value.id !== lockId) throw `Another operation is already in progress`;
        this._writeLock$.next({ id: lockId, count: (this._writeLock$.value?.count ?? 0) + 1 });
        const res = job();
        this._writeLock$.next(this._writeLock$.value!.count > 1 ? { id: this._writeLock$.value!.id, count: this._writeLock$.value!.count - 1 } : null);
        return res;
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
            takeUntil(this._stopSignal$), // TODO: test
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
            takeUntil(this._stopSignal$),
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
