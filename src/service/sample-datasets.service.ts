/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { load as parseYaml } from "js-yaml";
import { map, Observable, shareReplay, tap } from "rxjs";
import { SampleDatasetManifest } from "../concept/sample-dataset";
import { environment } from "../environments/environment";

@Injectable({
    providedIn: "root",
})
export class SampleDatasetsService {

    private manifestCache$?: Observable<SampleDatasetManifest>;

    constructor(private http: HttpClient) {}

    manifest$(): Observable<SampleDatasetManifest> {
        if (!this.manifestCache$) {
            this.manifestCache$ = this.http
                .get(environment.sampleDatasetsManifestUrl, { responseType: "text" })
                .pipe(
                    map(yaml => parseYaml(yaml) as SampleDatasetManifest),
                    // Drop cache on error so the next subscriber triggers a fresh fetch.
                    tap({ error: () => { this.manifestCache$ = undefined; } }),
                    shareReplay({ bufferSize: 1, refCount: false }),
                );
        }
        return this.manifestCache$;
    }

    reload(): void {
        this.manifestCache$ = undefined;
    }
}
