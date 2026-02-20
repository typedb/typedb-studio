/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { ApiResponse, QueryResponse } from "@typedb/driver-http";
import { TransactionOperation } from "./transaction";

interface DriverActionBase {
    actionType: string;
    startedAtTimestamp: number;
    completedAtTimestamp?: number;
    status: "pending" | "success" | "error";
    result?: Object;
}

export interface QueryRunAction extends DriverActionBase {
    actionType: "queryRun";
    query: string;
    autoCommitted?: boolean;
    result?: ApiResponse<QueryResponse>;
}

export interface TransactionOperationAction extends DriverActionBase {
    actionType: "transactionOperation";
    operation: TransactionOperation;
}

export function queryRunActionOf(query: string): QueryRunAction {
    return { actionType: "queryRun", query: query, status: "pending", startedAtTimestamp: Date.now() };
}

export function transactionOperationActionOf(operation: TransactionOperation): TransactionOperationAction {
    return { actionType: "transactionOperation", operation: operation, status: "pending", startedAtTimestamp: Date.now() };
}

export type DriverAction = QueryRunAction | TransactionOperationAction;

export function isQueryRun(entry: DriverAction): entry is QueryRunAction {
    return entry.actionType === "queryRun";
}

export function isTransactionOperation(entry: DriverAction): entry is TransactionOperationAction {
    return entry.actionType === "transactionOperation";
}
