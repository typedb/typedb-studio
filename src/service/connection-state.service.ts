/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { Connection } from "../concept";

@Injectable({
    providedIn: "root",
})
export class ConnectionStateService {

    status: "connected" | "connecting" | "disconnected";
    connection?: Connection;

    constructor() {
        this.status = "disconnected";
    }
}
