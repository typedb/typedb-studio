/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable, inject } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { AppData, PersistedSavedQuery } from "./app-data.service";

/**
 * Reactive layer over `AppData.savedQueries`. Pages and dialogs subscribe
 * to `entries$`; all mutation flows through here so every subscriber sees a
 * fresh snapshot after each add/remove/rename. Entries are kept sorted by
 * `savedAt` descending (newest first) the same way the page renders them.
 */
@Injectable({ providedIn: "root" })
export class SavedQueriesState {
    private appData = inject(AppData);
    readonly entries$ = new BehaviorSubject<PersistedSavedQuery[]>(this.read());

    private read(): PersistedSavedQuery[] {
        return this.appData.savedQueries.list().sort((a, b) => b.savedAt - a.savedAt);
    }

    private refresh() {
        this.entries$.next(this.read());
    }

    add(name: string, query: string): PersistedSavedQuery {
        const entry = this.appData.savedQueries.add(name, query);
        this.refresh();
        return entry;
    }

    remove(id: string): void {
        this.appData.savedQueries.remove(id);
        this.refresh();
    }

    rename(id: string, name: string): void {
        this.appData.savedQueries.rename(id, name);
        this.refresh();
    }
}

/** Auto-generated query-tab names look like `Query 1`, `Query 12`, etc.
 *  When the user hasn't picked a name yet we prompt before saving. */
export function isDefaultQueryTabName(name: string): boolean {
    return /^Query \d+$/.test(name);
}
