/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Injectable } from "@angular/core";

export type StorageWriteResult = { status: "ok" } | { status: "error"; error: unknown; };
const ok: StorageWriteResult = { status: "ok" };
const err: (err: unknown) => StorageWriteResult = (err: unknown) => ({ status: "error", error: err });

const TYPEDB_STUDIO = `typeDBStudio`;
const CAN_ACCESS = `storage.canAccess`;

@Injectable({
    providedIn: "root",
})
export class StorageService {

    readonly isAccessible: boolean;
    readonly writeError?: unknown;

    constructor() {
        const accessTestResult = this.write(CAN_ACCESS, true);
        if (accessTestResult.status === "ok") this.isAccessible = true;
        else [this.isAccessible, this.writeError] = [false, accessTestResult.error];
    }

    write(key: string, value: Object): StorageWriteResult {
        try {
            localStorage.setItem(`${TYPEDB_STUDIO}.${key}`, value.toString());
            return ok;
        } catch (e) {
            return err(e);
        }
    }

    read(key: string): string | null {
        return localStorage.getItem(`${TYPEDB_STUDIO}.${key}`);
    }
}
