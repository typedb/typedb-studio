/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { SessionStorageService } from "./session-storage.service";

const KEY = "startupMessage";

export type StartupMessageKind = "password-changed" | "user-deleted" | "signed-out";

export interface StartupMessage {
    kind: StartupMessageKind;
    username?: string;
}

@Injectable({
    providedIn: "root",
})
export class StartupMessageService {

    constructor(private storage: SessionStorageService) {}

    set(message: StartupMessage) {
        this.storage.write(KEY, message);
    }

    consume(): StartupMessage | null {
        const message = this.storage.read<StartupMessage | null>(KEY, (raw) => raw as StartupMessage | null);
        if (message) this.storage.remove(KEY);
        return message;
    }
}
