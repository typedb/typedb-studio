/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { BehaviorSubject, catchError, concatMap, distinctUntilChanged, filter, finalize, from, map, Observable, of, shareReplay, startWith, Subject, switchMap, takeUntil, tap, throwError } from "rxjs";
import { v4 as uuid } from "uuid";
import { DriverAction, QueryRunAction, queryRunActionOf, transactionOperationActionOf } from "../concept/action";
import { ConnectionConfig, databasesSortedByName, DEFAULT_DATABASE_NAME } from "../concept/connection";
import { OperationMode, Transaction } from "../concept/transaction";
import { fromPromiseWithRetry, requireValue } from "../framework/util/observable";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { AppData } from "./app-data.service";
import {
    ApiOkResponse, ApiResponse, Database, isApiErrorResponse, isOkResponse, QueryOptions, QueryResponse, TransactionType,
    TypeDBHttpDriver, User, VersionResponse
} from "@typedb/driver-http";
import { FormBuilder } from "@angular/forms";
import { QueryTypeDetector } from "./query-type-detector.service";

export type DriverStatus = "disconnected" | "connecting" | "connected" | "reconnecting";

interface Semaphore {
    id: string;
    count: number;
}

interface ServerVersion {
    major: number;
    minor: number;
    patch: number;
    suffix?: string;
}

@Injectable({
    providedIn: "root",
})
export class DriverState {

    private _status$ = new BehaviorSubject<DriverStatus>("disconnected");
    connection$ = new BehaviorSubject<ConnectionConfig | null>(null);
    serverVersion$ = new BehaviorSubject<VersionResponse | null>(null);
    database$ = new BehaviorSubject<Database | null>(null);
    private _transaction$ = new BehaviorSubject<Transaction | null>(null);

    private _databaseList$ = new BehaviorSubject<Database[] | null>(null);
    userList$ = new BehaviorSubject<User[] | null>(null);
    private _actionLog$ = new Subject<DriverAction>();
    private _writeLock$ = new BehaviorSubject<Semaphore | null>(null);
    private _stopSignal$ = new Subject<void>();

    private driver?: TypeDBHttpDriver;

    transactionControls = this.formBuilder.nonNullable.group({
        type: ["read" as TransactionType, []],
        operationMode: [this.appData.preferences.transactionMode() as OperationMode, []],
    });
    autoTransactionEnabled$ = new BehaviorSubject(this.appData.preferences.transactionMode() === "auto");
    transactionHasUncommittedChanges$ = this.transaction$.pipe(map(tx => tx?.hasUncommittedChanges ?? false));
    transactionTypeChanges$ = this.transactionControls.valueChanges.pipe(
        filter((changes) => !!changes.type),
        map((changes) => changes.type!),
        startWith(this.transactionControls.value.type!),
        distinctUntilChanged(),
        shareReplay(1),
    );
    transactionOperationModeChanges$ = this.transactionControls.valueChanges.pipe(
        filter(changes => !!changes.operationMode),
        map(changes => changes.operationMode!),
        startWith(this.transactionControls.value.operationMode!),
        distinctUntilChanged(),
        shareReplay(1),
    );

    constructor(private appData: AppData, private formBuilder: FormBuilder, private queryTypeDetector: QueryTypeDetector) {
        (window as any)["driverState"] = this;
    }

    get status$(): Observable<DriverStatus> {
        return this._status$;
    }

    get databaseList$(): Observable<Database[] | null> {
        return this._databaseList$;
    }

    get transaction$(): Observable<Transaction | null> {
        return this._transaction$;
    }

    get transactionOpen(): boolean {
        return this._transaction$.value != null;
    }

    get currentTransaction(): Transaction | null {
        return this._transaction$.value;
    }

    get actionLog$(): Observable<DriverAction> {
        return this._actionLog$;
    }

    private requireConnection() {
        return requireValue(this.connection$);
    }

    requireDatabase() {
        return requireValue(this.database$);
    }

    private requireDatabaseList() {
        return requireValue(this._databaseList$);
    }

    private requireTransaction() {
        return requireValue(this._transaction$);
    }

    private requireDriver() {
        if (this.driver) return this.driver;
        else throw new Error(INTERNAL_ERROR);
    }

    tryConnect(config: ConnectionConfig): Observable<ConnectionConfig> {
        const lockId = uuid();
        return this.tryUseWriteLock(() => {
            const maybeTryDisconnect$ = this._status$.value === "connected" ? this.tryDisconnect(lockId) : of({});
            return maybeTryDisconnect$.pipe(
                tap(() => {
                    this.connection$.next(config);
                    this._status$.next("connecting");
                    this.driver = new TypeDBHttpDriver(config.params);
                }),
                switchMap(() => this.checkServerVersion()),
                tap((res) => {
                    if (isOkResponse(res) && res.ok != null) {
                        const rawVersion = (res.ok as Partial<VersionResponse>).version;
                        this.serverVersion$.next(res.ok as VersionResponse);
                        const parsedVersion = this.parseServerVersionOrNull(rawVersion);
                        if (parsedVersion?.major === 3 && parsedVersion.minor >= 3) return;
                        else if (parsedVersion == null) throw { customError: `Unsupported TypeDB server version.\nTypeDB Studio supports TypeDB 3.3.0 and above.` };
                        else throw { customError: `Unsupported TypeDB server version: ${rawVersion}.\nTypeDB Studio supports TypeDB 3.3.0 and above.` };
                    } else throw res;
                }),
                switchMap(() => this.refreshDatabaseList()),
                switchMap((res) => {
                    if (isOkResponse(res)) {
                        if (!res.ok.databases.length) return this.setupDefaultDatabase(lockId).pipe(map(() =>
                            config.withDatabase(this.requireDatabase())
                        ));
                        if (res.ok.databases.length === 1) this.selectDatabase(res.ok.databases[0], lockId);
                        else if (config.params.database && this.requireDatabaseList().some(x => x.name === config.params.database)) {
                            this.selectDatabase({ name: config.params.database }, lockId);
                        }
                        const database = this.database$.value;
                        return of(database ? config.withDatabase(database) : config);
                    } else throw res;
                }),
                tap(() => {
                    this._status$.next("connected");
                    this.appData.connections.push(config);
                    this.refreshUserList().subscribe();
                }),
                catchError((err) => {
                    this.connection$.next(null); // TODO: revisit - is an 'errored' connection config still valid?
                    this._status$.next("disconnected");
                    return throwError(() => err);
                }),
            );
        }, lockId);
    }

    tryDisconnect(lockId = uuid()) {
        if (this._status$.value === "disconnected") throw new Error(INTERNAL_ERROR);
        if (this._transaction$.value?.hasUncommittedChanges) throw new Error(INTERNAL_ERROR);
        this.appData.connections.clearStartupConnection();
        const maybeCloseTransaction$ = this._transaction$.value ? this.closeTransaction(lockId) : of({});
        return maybeCloseTransaction$.pipe(tap(() => this.tryUseWriteLock(() => {
            this.connection$.next(null);
            this.serverVersion$.next(null);
            this.database$.next(null);
            this._databaseList$.next(null);
            this.userList$.next(null);
            this._status$.next("disconnected");
        }, lockId)));
    }

    refreshDatabaseList() {
        const driver = this.requireDriver();
        return fromPromiseWithRetry(() => driver.getDatabases()).pipe(
            tap(res => {
                if (isOkResponse(res)) this._databaseList$.next(databasesSortedByName(res.ok.databases));
            }),
            takeUntil(this._stopSignal$)
        );
    }

    refreshUserList() {
        const driver = this.requireDriver();
        return fromPromiseWithRetry(() => driver.getUsers()).pipe(
            tap(res => {
                if (isOkResponse(res)) this.userList$.next(res.ok.users);
            }),
            takeUntil(this._stopSignal$)
        );
    }

    selectDatabase(database: Database | null, lockId = uuid()) {
        if (this.database$.value?.name === database?.name) return;

        if (database == null) {
            this.database$.next(null);
            const currentConnection = this.requireConnection();
            const connection = currentConnection.withDatabase(null);
            this.connection$.next(connection);
            this.appData.connections.push(connection);
            return;
        }

        const savedDatabase = this._databaseList$.value?.find(x => x.name === database.name);
        if (!savedDatabase) throw new Error(INTERNAL_ERROR);
        else this.tryUseWriteLock(() => {
            this.database$.next(savedDatabase);
            const currentConnection = this.requireConnection();
            const connection = currentConnection.withDatabase(savedDatabase);
            this.connection$.next(connection);
            this.appData.connections.push(connection);
        }, lockId);
    }

    createAndSelectDatabase(name: string, lockId = uuid()) {
        const driver = this.requireDriver();
        return this.tryUseWriteLock(() => fromPromiseWithRetry(() => driver.createDatabase(name)).pipe(
            tap((res) => {
                if (isApiErrorResponse(res)) throw res.err;
                const databaseList = this.requireDatabaseList();
                this._databaseList$.next(databasesSortedByName([...databaseList, { name }]));
                this.selectDatabase({ name }, lockId);
            }),
            takeUntil(this._stopSignal$)
        ), lockId);
    }

    deleteDatabase(database: Database, lockId = uuid()) {
        const driver = this.requireDriver();
        return this.tryUseWriteLock(() => fromPromiseWithRetry(() => driver.deleteDatabase(database.name)).pipe(
            tap((res) => {
                if (isApiErrorResponse(res)) throw res.err;
                const databaseList = this.requireDatabaseList();
                this.selectDatabase(null, lockId);
                this._databaseList$.next(databaseList.filter(x => x.name !== database.name));
            }),
            takeUntil(this._stopSignal$)
        ), lockId);
    }

    openTransaction(type: TransactionType, lockId = uuid()) {
        const databaseName = this.requireDatabase().name;
        const driver = this.requireDriver();
        return this.tryUseWriteLock(() => fromPromiseWithRetry(() => driver.openTransaction(databaseName, type)).pipe(
            tap((res) => {
                if (isApiErrorResponse(res)) throw res.err;
                this._transaction$.next(new Transaction({ id: res.ok.transactionId, type: type }));
            }),
            takeUntil(this._stopSignal$),
        ), lockId);
    }

    commitTransaction(lockId = uuid()) {
        const transactionId = this.requireTransaction().id;
        const action = transactionOperationActionOf("commit");
        this._actionLog$.next(action);
        const driver = this.requireDriver();
        return this.tryUseWriteLock(() => fromPromiseWithRetry(() => driver.commitTransaction(transactionId)).pipe(
            tap((res) => {
                this.updateActionResult(action, res);
                this._transaction$.next(null);
                if (isApiErrorResponse(res)) throw res.err;
            }),
            takeUntil(this._stopSignal$),
            catchError((err) => {
                this.updateActionResultUnexpectedError(action, err);
                return throwError(() => err);
            }),
        ), lockId);
    }

    closeTransaction(lockId = uuid()) {
        const transactionId = this._transaction$.value?.id;
        if (transactionId == null) return of({});
        const driver = this.requireDriver();
        return this.tryUseWriteLock(() => fromPromiseWithRetry(() => driver.closeTransaction(transactionId)).pipe(
            tap((res) => {
                if (isApiErrorResponse(res)) throw res.err;
                this._transaction$.next(null);
            }),
            takeUntil(this._stopSignal$),
        ), lockId);
    }

    query(query: string, queryOptions?: QueryOptions): Observable<ApiResponse<QueryResponse>> {
        if (this.autoTransactionEnabled$.value) {
            return this.autoQuery(query, queryOptions);
        } else {
            return this.manualQuery(query, queryOptions);
        }
    }

    /**
     * Execute a query in auto mode with automatic transaction type detection.
     * Uses oneShotQuery for better performance (single HTTP call handles transaction lifecycle).
     * Delegates to QueryTypeDetector which handles retry escalation internally.
     *
     * Only logs the final successful action to history (not failed retry attempts).
     */
    private autoQuery(query: string, queryOptions?: QueryOptions): Observable<ApiResponse<QueryResponse>> {
        const queryAction = queryRunActionOf(query);
        const databaseName = this.requireDatabase().name;

        return this.queryTypeDetector.runWithAutoType(
            query,
            (transactionType: TransactionType) => this.executeOneShotQuery(query, databaseName, transactionType, queryOptions)
        ).pipe(
            tap(({ type, result }) => {
                queryAction.autoCommitted = type !== "read";
                this.updateActionResult(queryAction, result);
                this._actionLog$.next(queryAction);
            }),
            map(({ result }) => result),
            catchError((err) => {
                this.updateActionResultUnexpectedError(queryAction, err);
                this._actionLog$.next(queryAction);
                return throwError(() => err);
            }),
            takeUntil(this._stopSignal$)
        );
    }

    /**
     * Execute a oneshot query with a specific transaction type.
     * Uses oneShotQuery which handles the full transaction lifecycle in one call.
     * Commits for write/schema transactions, closes for read transactions.
     */
    private executeOneShotQuery(query: string, databaseName: string, transactionType: TransactionType, queryOptions?: QueryOptions): Observable<ApiResponse<QueryResponse>> {
        const driver = this.requireDriver();
        const shouldCommit = transactionType !== "read";

        return fromPromiseWithRetry(() => driver.oneShotQuery(query, shouldCommit, databaseName, transactionType, undefined, queryOptions)).pipe(
            tap((res) => {
                if (isApiErrorResponse(res)) throw res;
            }),
        );
    }

    /**
     * Execute a query in manual mode using the existing open transaction.
     */
    private manualQuery(query: string, queryOptions?: QueryOptions): Observable<ApiResponse<QueryResponse>> {
        let queryRunAction: QueryRunAction;
        const driver = this.requireDriver();
        const lockId = uuid();

        return this.tryUseWriteLock(() => of({ ok: { transactionId: this.requireTransaction().id } }).pipe(
            tap(() => {
                const transaction = this.requireTransaction();
                queryRunAction = queryRunActionOf(query);
                transaction.queryRuns.push(queryRunAction);
                this._actionLog$.next(queryRunAction);
            }),
            switchMap((res) => {
                return fromPromiseWithRetry(() => driver.query(res.ok.transactionId, query, queryOptions));
            }),
            tap((res) => this.updateActionResult(queryRunAction, res)),
            tap(() => {
                const transaction = this.requireTransaction();
                this._transaction$.next(transaction);
            }),
            catchError((err) => {
                if (queryRunAction) this.updateActionResultUnexpectedError(queryRunAction, err);
                return throwError(() => err);
            }),
            takeUntil(this._stopSignal$)
        ), lockId);
    }

    runBackgroundReadQueries(queries: string[]): Observable<ApiOkResponse<QueryResponse>> {
        const driver = this.requireDriver();
        const databaseName = this.requireDatabase().name;
        return fromPromiseWithRetry(() => driver.openTransaction(databaseName, "read")).pipe(
            switchMap((res) => {
                if (isApiErrorResponse(res)) throw res.err;
                return from(queries).pipe(
                    concatMap(x => fromPromiseWithRetry(() => driver.query(res.ok.transactionId, x)).pipe(
                        map(x => {
                            if (isApiErrorResponse(x)) throw x;
                            else return x;
                        })
                    )),
                    finalize(() => fromPromiseWithRetry(() => driver.closeTransaction(res.ok.transactionId)).subscribe()),
                );
            }),
        );
    }

    createUser(username: string, password: string) {
        const driver = this.requireDriver();
        return fromPromiseWithRetry(() => driver.createUser(username, password));
    }

    updateUser(username: string, password: string) {
        const driver = this.requireDriver();
        return fromPromiseWithRetry(() => driver.updateUser(username, password));
    }

    deleteUser(username: string) {
        const driver = this.requireDriver();
        return fromPromiseWithRetry(() => driver.deleteUser(username));
    }

    getDatabaseSchemaText(): Observable<ApiResponse<string>> {
        const driver = this.requireDriver();
        return fromPromiseWithRetry(() => driver.getDatabaseSchema(this.requireDatabase().name));
    }

    checkHealth() {
        const driver = this.requireDriver();
        return fromPromiseWithRetry(() => driver.health());
    }

    checkServerVersion() {
        const driver = this.requireDriver();
        return fromPromiseWithRetry(() => driver.version());
    }

    private parseServerVersionOrNull(raw: string | undefined): ServerVersion | null {
        if (!raw?.length) return null;
        const [body, suffix] = raw.split(`-`) as [string, string?];
        const [major, minor, patch] = body.split(`.`).map(x => parseInt(x));
        return { major, minor, patch, suffix };
    }

    sendStopSignal() {
        this._stopSignal$.next();
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

    private updateActionResult(action: DriverAction, res: ApiResponse) {
        action.completedAtTimestamp = Date.now();
        action.status = isApiErrorResponse(res) ? "error" : "success";
        action.result = res;
    }

    private updateActionResultUnexpectedError(action: DriverAction, err: any) {
        action.completedAtTimestamp = Date.now();
        action.status = "error";
        action.result = err;
    }

}
