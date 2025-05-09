/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { BehaviorSubject, catchError, map, Observable, of, Subject, switchMap, takeUntil, tap } from "rxjs";
import { fromPromise } from "rxjs/internal/observable/innerFrom";
import { v4 as uuid } from "uuid";
import { DriverAction, QueryRunAction, queryRunActionOf, transactionOperationActionOf } from "../concept/action";
import { ConnectionConfig, databasesSortedByName, DEFAULT_DATABASE_NAME } from "../concept/connection";
import { Transaction } from "../concept/transaction";
import { TypeDBHttpDriver } from "../framework/typedb-driver";
import { Database } from "../framework/typedb-driver/database";
import { ApiResponse, isApiErrorResponse, isOkResponse, QueryResponse } from "../framework/typedb-driver/response";
import { requireValue } from "../framework/util/observable";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { AppData } from "./app-data.service";
import { TransactionType } from "../framework/typedb-driver/transaction";

export type DriverStatus = "disconnected" | "connecting" | "connected" | "reconnecting";

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

    private driver?: TypeDBHttpDriver;

    private _transactionType$ = new BehaviorSubject<TransactionType>("read");
    autoTransactionEnabled$ = new BehaviorSubject(true);
    transactionHasUncommittedChanges$ = this.transaction$.pipe(map(tx => tx?.hasUncommittedChanges ?? false));

    constructor(private appData: AppData) {
        (window as any)["driverState"] = this;
    }

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

    get transactionType$(): Observable<TransactionType> {
        return this._transactionType$;
    }

    private requireConnection(stack: string) {
        return requireValue(this._connection$, stack);
    }

    requireDatabase(stack: string) {
        return requireValue(this._database$, stack);
    }

    private requireDatabaseList(stack: string) {
        return requireValue(this._databaseList$, stack);
    }

    private requireTransaction(stack: string) {
        return requireValue(this._transaction$, stack);
    }

    private requireDriver(stack: string) {
        if (this.driver) return this.driver;
        else throw `${INTERNAL_ERROR}: ${stack}`;
    }

    tryConnect(config: ConnectionConfig): Observable<ConnectionConfig> {
        const lockId = uuid();
        return this.tryUseWriteLock(() => {
            const maybeTryDisconnect$ = this._status$.value === "connected" ? this.tryDisconnect(lockId) : of({});
            return maybeTryDisconnect$.pipe(
                tap(() => {
                    this._connection$.next(config);
                    this._status$.next("connecting");
                    this.driver = new TypeDBHttpDriver(config.params);
                }),
                switchMap(() => this.refreshDatabaseList()),
                switchMap((res) => {
                    if (isOkResponse(res)) {
                        if (!res.ok.databases.length) return this.setupDefaultDatabase(lockId).pipe(map(() =>
                            config.withDatabase(this.requireDatabase(`${this.constructor.name}.${this.tryConnect.name} > ${this.requireDatabase.name}`))
                        ));
                        if (res.ok.databases.length === 1) this.selectDatabase(res.ok.databases[0], lockId);
                        else if (config.params.database
                            && this.requireDatabaseList(`${this.constructor.name}.${this.tryConnect.name} > ${this.requireDatabaseList.name}`)
                                .some(x => x.name === config.params.database)) {
                            this.selectDatabase({ name: config.params.database }, lockId);
                        }
                        const database = this._database$.value;
                        return of(database ? config.withDatabase(database) : config);
                    } else throw res;
                }),
                tap(() => {
                    this._status$.next("connected");
                    this.appData.connections.push(config);
                }),
                catchError((err) => {
                    this._connection$.next(null); // TODO: revisit - is an 'errored' connection config still valid?
                    this._status$.next("disconnected");
                    throw err;
                }),
            );
        }, lockId);
    }

    tryDisconnect(lockId = uuid()) {
        if (this._status$.value === "disconnected") throw INTERNAL_ERROR;
        if (this._transaction$.value?.hasUncommittedChanges) throw INTERNAL_ERROR;
        this.appData.connections.clearStartupConnection();
        const maybeCloseTransaction$ = this._transaction$.value ? this.closeTransaction(lockId) : of({});
        return maybeCloseTransaction$.pipe(tap(() => this.tryUseWriteLock(() => {
            this._connection$.next(null);
            this._database$.next(null);
            this._status$.next("disconnected");
        }, lockId)));
    }

    refreshDatabaseList() {
        const driver = this.requireDriver(`${this.constructor.name}.${this.refreshDatabaseList.name} > ${this.requireDriver.name}`);
        return fromPromise(driver.listDatabases()).pipe(
            tap(res => {
                if (isOkResponse(res)) this._databaseList$.next(databasesSortedByName(res.ok.databases));
            }),
            takeUntil(this._stopSignal$)
        );
    }

    selectDatabase(database: Database, lockId = uuid()) {
        if (this._database$.value?.name === database.name) return;
        const savedDatabase = this._databaseList$.value?.find(x => x.name === database.name);
        if (!savedDatabase) throw INTERNAL_ERROR;
        else this.tryUseWriteLock(() => {
            this._database$.next(savedDatabase);
            const currentConnection = this.requireConnection(`${this.constructor.name}.${this.selectDatabase.name} > ${this.requireConnection.name}`);
            const connection = currentConnection.withDatabase(savedDatabase);
            this._connection$.next(connection);
            this.appData.connections.push(connection);
        }, lockId);
    }

    createAndSelectDatabase(name: string, lockId = uuid()) {
        const driver = this.requireDriver(`${this.constructor.name}.${this.createAndSelectDatabase.name} > ${this.requireDriver.name}`);
        return this.tryUseWriteLock(() => fromPromise(driver.createDatabase(name)).pipe(
            tap((res) => {
                if (isApiErrorResponse(res)) throw res.err;
                const databaseList = this.requireDatabaseList(`${this.constructor.name}.${this.createAndSelectDatabase.name} > ${this.requireDatabaseList.name}`);
                this._databaseList$.next(databasesSortedByName([...databaseList, { name }]));
                this.selectDatabase({ name }, lockId);
            }),
            takeUntil(this._stopSignal$)
        ), lockId);
    }

    openTransaction(type: TransactionType, lockId = uuid()) {
        const databaseName = this.requireDatabase(`${this.constructor.name}.${this.openTransaction.name} > ${this.requireDatabase.name}`).name;
        const action = transactionOperationActionOf("open");
        this._actionLog$.next(action);
        const driver = this.requireDriver(`${this.constructor.name}.${this.openTransaction.name} > ${this.requireDriver.name}`);
        return this.tryUseWriteLock(() => fromPromise(driver.openTransaction(databaseName, type)).pipe(
            tap((res) => {
                this.updateActionResult(action, res);
                if (isApiErrorResponse(res)) throw res.err;
                this._transaction$.next(new Transaction({ id: res.ok.transactionId, type: type }));
            }),
            takeUntil(this._stopSignal$),
            catchError((err) => {
                this.updateActionResultUnexpectedError(action, err);
                throw err;
            }),
        ), lockId);
    }

    commitTransaction(lockId = uuid()) {
        const transactionId = this.requireTransaction(`${this.constructor.name}.${this.commitTransaction.name} > ${this.requireTransaction.name}`).id;
        const action = transactionOperationActionOf("commit");
        this._actionLog$.next(action);
        const driver = this.requireDriver(`${this.constructor.name}.${this.commitTransaction.name} > ${this.requireDriver.name}`);
        return this.tryUseWriteLock(() => fromPromise(driver.commitTransaction(transactionId)).pipe(
            tap((res) => {
                this.updateActionResult(action, res);
                this._transaction$.next(null);
                if (isApiErrorResponse(res)) throw res.err;
            }),
            takeUntil(this._stopSignal$),
            catchError((err) => {
                this.updateActionResultUnexpectedError(action, err);
                throw err;
            }),
        ), lockId);
    }

    closeTransaction(lockId = uuid()) {
        const transactionId = this._transaction$.value?.id;
        if (transactionId == null) return of({});
        const action = transactionOperationActionOf("close");
        this._actionLog$.next(action);
        const driver = this.requireDriver(`${this.constructor.name}.${this.closeTransaction.name} > ${this.requireDriver.name}`);
        return this.tryUseWriteLock(() => fromPromise(driver.closeTransaction(transactionId)).pipe(
            tap((res) => {
                this.updateActionResult(action, res);
                if (isApiErrorResponse(res)) throw res.err;
                this._transaction$.next(null);
            }),
            takeUntil(this._stopSignal$),
            catchError((err) => {
                this.updateActionResultUnexpectedError(action, err);
                throw err;
            }),
        ), lockId);
    }

    selectTransactionType(transactionType: TransactionType) {
        // TODO: fail if has uncommitted changes
        return this.tryUseWriteLock(() => {
            this._transactionType$.next(transactionType);
        });
    }

    // TODO: convoluted logic
    query(query: string): Observable<ApiResponse<QueryResponse>> {
        const lockId = uuid();
        const maybeOpenTransaction$ = this.autoTransactionEnabled$.value
            ? this.openTransaction(this._transactionType$.value, lockId)
            : of({ ok: { transactionId: this.requireTransaction(`${this.constructor.name}.${this.query.name} > ${this.requireTransaction.name}`).id } });
        let queryRunAction: QueryRunAction;
        const driver = this.requireDriver(`${this.constructor.name}.${this.query.name} > ${this.requireDriver.name}`);
        return this.tryUseWriteLock(() => maybeOpenTransaction$.pipe(
            tap(() => {
                const transaction = this.requireTransaction(`${this.constructor.name}.${this.query.name} > ${this.requireTransaction.name}`);
                queryRunAction = queryRunActionOf(query);
                transaction.queryRuns.push(queryRunAction);
                this._actionLog$.next(queryRunAction);
            }),
            switchMap((res) => {
                if (isApiErrorResponse(res)) throw res;
                return fromPromise(driver.query(res.ok.transactionId, query));
            }),
            tap((res) => this.updateActionResult(queryRunAction, res)),
            // TODO: maybe extract TransactionStateService
            tap(() => {
                const transaction = this.requireTransaction(`${this.constructor.name}.${this.query.name} > ${this.requireTransaction.name}`);
                if (!this.autoTransactionEnabled$.value) this._transaction$.next(transaction);
            }),
            tap((res) => {
                if (this.autoTransactionEnabled$.value) {
                    if (this._transactionType$.value !== "read" && isOkResponse(res)) this.commitTransaction(lockId).subscribe();
                    else this.closeTransaction(lockId).subscribe();
                }
            }),
            catchError((err) => {
                if (queryRunAction) this.updateActionResultUnexpectedError(queryRunAction, err);
                if (this.autoTransactionEnabled$.value) this.closeTransaction(lockId).subscribe();
                throw err;
            }),
            takeUntil(this._stopSignal$)
        ), lockId);
    }

    checkHealth() {
        const driver = this.requireDriver(`${this.constructor.name}.${this.checkHealth.name} > ${this.requireDriver.name}`);
        return fromPromise(driver.health());
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
