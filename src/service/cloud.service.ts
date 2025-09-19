/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { environment } from "../environments/environment";
import { HttpClient } from "@angular/common/http";

export interface VibeQueryResponse {
    response: string;
}

@Injectable({
    providedIn: "root",
})
export class CloudService {
    constructor(private http: HttpClient) {}

    vibeQuery(schema: string, prompt: string) {
        return this.http.post<VibeQueryResponse>(`${environment.cloudUrl}/agentic/vibe-query`, { schema, prompt });
    }
}
