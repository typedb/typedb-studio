/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
