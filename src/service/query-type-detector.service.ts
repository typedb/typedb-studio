/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { Observable, of, throwError } from "rxjs";
import { catchError, switchMap } from "rxjs/operators";
import { isApiErrorResponse, TransactionType } from "@typedb/driver-http";

/**
 * Result of executing a query with auto type detection.
 */
export interface AutoTypeResult<T> {
    type: TransactionType;
    result: T;
}

/**
 * Service for detecting and selecting the required transaction type for queries.
 *
 * Uses retry escalation: read -> write -> schema
 * Catches "wrong transaction type" errors and automatically retries with the next type.
 */
@Injectable({
    providedIn: "root",
})
export class QueryTypeDetector {

    /** Transaction type escalation order: most restrictive to most permissive */
    private readonly escalationOrder: TransactionType[] = ["read", "write", "schema"];

    /**
     * Execute an operation with automatic transaction type detection.
     *
     * 1. First tries the auto-detected type (if any)
     * 2. If that fails, goes through escalation order from read -> write -> schema
     *
     * @param query The query string (available for pre-detection via regex/parser/driver analysis)
     * @param executeWithType Function that executes the operation with a given transaction type.
     *                        Should throw/error if the transaction type is wrong.
     * @returns Observable with the successful transaction type and result
     */
    runWithAutoType<T>(
        query: string,
        executeWithType: (type: TransactionType) => Observable<T>
    ): Observable<AutoTypeResult<T>> {
        const detectedType = this.detectQueryType(query);
        if (detectedType) {
            // Try detected type first, then fall back to full escalation from read
            return this.tryOnce(detectedType, executeWithType).pipe(
                catchError(err => {
                    if (this.isWrongTransactionTypeError(err)) {
                        return this.tryWithEscalation(this.escalationOrder[0], executeWithType);
                    }
                    return throwError(() => err);
                })
            );
        }
        return this.tryWithEscalation(this.escalationOrder[0], executeWithType);
    }

    /**
     * Attempt to pre-detect the transaction type from the query.
     * Returns null if unable to determine, falling back to retry escalation.
     */
    private detectQueryType(query: string): TransactionType | null {
        // TODO: Add pre-detection logic here (regex, parser, driver analysis)
        return null;
    }

    /**
     * Try executing once with a specific transaction type, no retry.
     */
    private tryOnce<T>(
        type: TransactionType,
        executeWithType: (type: TransactionType) => Observable<T>
    ): Observable<AutoTypeResult<T>> {
        return executeWithType(type).pipe(
            switchMap(result => of({ type, result }))
        );
    }

    /**
     * Try executing with escalation through transaction types on wrong type errors.
     */
    private tryWithEscalation<T>(
        type: TransactionType,
        executeWithType: (type: TransactionType) => Observable<T>
    ): Observable<AutoTypeResult<T>> {
        return executeWithType(type).pipe(
            switchMap(result => of({ type, result })),
            catchError(err => {
                const nextType = this.getNextTransactionType(type);
                if (nextType && this.isWrongTransactionTypeError(err)) {
                    return this.tryWithEscalation(nextType, executeWithType);
                }
                return throwError(() => err);
            })
        );
    }

    /**
     * Get the next transaction type to try after a failure.
     */
    private getNextTransactionType(current: TransactionType): TransactionType | null {
        const currentIndex = this.escalationOrder.indexOf(current);
        if (currentIndex === -1 || currentIndex >= this.escalationOrder.length - 1) {
            return null;
        }
        return this.escalationOrder[currentIndex + 1];
    }

    /**
     * Check if an error indicates the wrong transaction type was used.
     * TSV8: Schema modification queries require schema transactions
     * TSV9: Data modification queries require write transactions
     */
    private isWrongTransactionTypeError(err: any): boolean {
        if (isApiErrorResponse(err)) {
            const errString = JSON.stringify(err);
            return errString.includes("TSV8") || errString.includes("TSV9");
        }
        return false;
    }
}
