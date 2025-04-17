/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

export class Transaction {

    readonly id: string;
    readonly type: TransactionType;
    readonly openedAtTimestamp: number;
    readonly committed: boolean;
    readonly closedAtTimestamp?: number;
    readonly queryRuns: QueryRun[];

    constructor(props: { id: string; type: TransactionType }) {
        this.id = props.id;
        this.type = props.type;
        this.openedAtTimestamp = Date.now();
        this.committed = false;
        this.closedAtTimestamp = undefined;
        this.queryRuns = [];
    }

    get hasUncommittedChanges(): boolean {
        return !!this.queryRuns.length && this.type !== "read";
    }

    get displayName(): string {
        return `${this.type}(id=${this.id})`;
    }

    // static fromApiJSONOrNull(json: Partial<TransactionApiJson>): Transaction | null {
    //     if (!json.name || !json.url || !json.preferences) return null;
    //     const params = parseConnectionUrlOrNull(json.url) || null;
    //     if (!params) return null;
    //     return new ConnectionConfig({
    //         name: json.name,
    //         params: params,
    //         preferences: json.preferences,
    //     });
    // }
}

export type TransactionType = "read" | "write" | "schema";

export type ReadMode = "auto" | "manual";

export interface QueryRun {
    query: string;
    status: "pending" | "success" | "error";
    result?: Object;
    startedAtTimestamp: number;
    completedAtTimestamp?: number;
}

export type TransactionOperationType = "open" | "commit" | "close";

export interface TransactionOperation {
    operationType: TransactionOperationType;
    status: "pending" | "success" | "error";
    result?: Object;
    startedAtTimestamp: number;
    completedAtTimestamp?: number;
}

// export interface TransactionApiJson {
//     name: string;
//     url: string;
//     preferences: ConnectionPreferences;
// }
