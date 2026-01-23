/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { BehaviorSubject, Observable, timer, throwError } from "rxjs";
import { retry } from "rxjs/operators";
import { fromPromise } from "rxjs/internal/observable/innerFrom";
import { INTERNAL_ERROR } from "./strings";

export function requireValue<T>(behaviorSubject: BehaviorSubject<T | null>): T {
    const value = behaviorSubject.value;
    if (!value) throw new Error(INTERNAL_ERROR);
    return value;
}

/**
 * Checks if an error is a retryable server error (5xx HTTP status).
 * Handles both ApiErrorResponse objects and raw Error objects with status codes.
 */
function isRetryableError(error: any): boolean {
    // Check for ApiErrorResponse structure with status field
    if (error && typeof error === "object" && "status" in error) {
        const status = error.status;
        return typeof status === "number" && status >= 500 && status < 600;
    }
    // Check for Error objects that might have a status property
    if (error instanceof Error && "status" in error) {
        const status = (error as any).status;
        return typeof status === "number" && status >= 500 && status < 600;
    }
    return false;
}

export interface RetryConfig {
    maxRetries?: number;
    initialDelayMs?: number;
    maxDelayMs?: number;
    backoffMultiplier?: number;
}

const DEFAULT_RETRY_CONFIG: Required<RetryConfig> = {
    maxRetries: 3,
    initialDelayMs: 500,
    maxDelayMs: 3000,
    backoffMultiplier: 2,
};

/**
 * Wraps a Promise-returning function with retry logic for 5xx server errors.
 * Uses exponential backoff with configurable delays.
 * Total maximum delay with defaults: ~500 + 1000 + 2000 = 3500ms (plus request time)
 *
 * @param promiseFn - Function that returns a Promise to retry
 * @param config - Optional retry configuration
 * @returns Observable that retries on 5xx errors
 */
export function fromPromiseWithRetry<T>(
    promiseFn: () => Promise<T>,
    config: RetryConfig = {}
): Observable<T> {
    const { maxRetries, initialDelayMs, maxDelayMs, backoffMultiplier } = {
        ...DEFAULT_RETRY_CONFIG,
        ...config,
    };

    return fromPromise(promiseFn()).pipe(
        retry({
            count: maxRetries,
            delay: (error, retryCount) => {
                if (!isRetryableError(error)) {
                    return throwError(() => error);
                }
                const delay = Math.min(
                    initialDelayMs * Math.pow(backoffMultiplier, retryCount - 1),
                    maxDelayMs
                );
                console.warn(
                    `Server error (status ${error?.status}), retrying in ${delay}ms (attempt ${retryCount}/${maxRetries})`
                );
                return timer(delay);
            },
        })
    );
}
