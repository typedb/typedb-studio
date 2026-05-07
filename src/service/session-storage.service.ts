/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";

export type StorageWriteResult = { status: "ok" } | { status: "error"; error: unknown; };
const ok: StorageWriteResult = { status: "ok" };
const err: (err: unknown) => StorageWriteResult = (err: unknown) => ({ status: "error", error: err });

const TYPEDB_STUDIO = `typeDBStudio`;
const CAN_ACCESS = `sessionStorage.canAccess`;

@Injectable({
    providedIn: "root",
})
export class SessionStorageService {

    readonly isAccessible: boolean;
    readonly writeError?: unknown;

    constructor() {
        const accessTestResult = this.write(CAN_ACCESS, true);
        if (accessTestResult.status === "ok") {
            this.isAccessible = true;
            this.remove(CAN_ACCESS);
        } else [this.isAccessible, this.writeError] = [false, accessTestResult.error];
    }

    write(key: string, value: Object): StorageWriteResult {
        try {
            sessionStorage.setItem(`${TYPEDB_STUDIO}.${key}`, JSON.stringify(value));
            return ok;
        } catch (e) {
            return err(e);
        }
    }

    read<OBJ = Object>(key: string, deserializeFn: (obj: Object | null) => OBJ): OBJ {
        try {
            const raw = sessionStorage.getItem(`${TYPEDB_STUDIO}.${key}`);
            return deserializeFn(raw != null ? JSON.parse(raw) : null);
        } catch (e) {
            console.warn(e);
            sessionStorage.removeItem(`${TYPEDB_STUDIO}.${key}`);
            return deserializeFn(null);
        }
    }

    remove(key: string) {
        try {
            sessionStorage.removeItem(`${TYPEDB_STUDIO}.${key}`);
        } catch {
            // sessionStorage unavailable; nothing to do.
        }
    }
}
