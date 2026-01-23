/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { BehaviorSubject, Observable, timer, throwError, defer } from "rxjs";
import { retry, tap } from "rxjs/operators";
import { fromPromise } from "rxjs/internal/observable/innerFrom";
import { INTERNAL_ERROR } from "./strings";

/**
 * Marker class to identify retryable 5xx errors thrown by fromPromiseWithRetry.
 * Wraps the original ApiErrorResponse to preserve the status for retry logic.
 */
class RetryableApiError extends Error {
    constructor(public readonly response: { status: number; err: { code: string; message: string } }) {
        super(response.err.message);
        this.name = "RetryableApiError";
    }
}

export function requireValue<T>(behaviorSubject: BehaviorSubject<T | null>): T {
    const value = behaviorSubject.value;
    if (!value) throw new Error(INTERNAL_ERROR);
    return value;
}

/**
 * Checks if an error is a retryable error.
 * Handles:
 * - RetryableApiError instances (5xx responses converted to errors)
 * - Network-level fetch failures ("Failed to fetch" TypeError)
 */
function isRetryableError(error: any): boolean {
    if (error instanceof RetryableApiError) {
        return true;
    }
    // Network-level fetch failures (connection reset, CORS, etc.)
    if (error instanceof TypeError && error.message === "Failed to fetch") {
        return true;
    }
    return false;
}

/**
 * Checks if a response value is an ApiErrorResponse with a 5xx status code.
 */
function isRetryableResponse(value: any): boolean {
    if (value && typeof value === "object" && "status" in value && "err" in value) {
        const status = value.status;
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

    return defer(() => fromPromise(promiseFn())).pipe(
        tap((value) => {
            // TypeDB driver returns ApiErrorResponse as resolved values, not rejections.
            // Convert 5xx responses to thrown errors so retry logic can intercept them.
            if (isRetryableResponse(value)) {
                throw new RetryableApiError(value as { status: number; err: { code: string; message: string } });
            }
        }),
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
                const errorDesc = error instanceof RetryableApiError
                    ? `status ${error.response.status}`
                    : error.message;
                console.warn(
                    `Server error (${errorDesc}), retrying in ${delay}ms (attempt ${retryCount}/${maxRetries})`
                );
                return timer(delay);
            },
        })
    );
}
