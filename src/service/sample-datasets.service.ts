/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { load as parseYaml } from "js-yaml";
import { map, Observable, shareReplay, tap } from "rxjs";
import { compare as semverCompare, satisfies } from "semver";
import { ResolvedSampleDatasetVersion, SampleDatasetManifest, SampleDatasetVersion } from "../concept/sample-dataset";
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

    /**
     * Picks the release of `datasetName` best suited to `serverVersion`:
     *   - the newest release whose `compatibleServers` range is satisfied (exact match), or
     *   - if none match, the newest release overall (flagged !isExactMatch).
     * Pre-release server versions are always included when evaluating ranges.
     */
    resolveForServer(
        manifest: SampleDatasetManifest, datasetName: string, serverVersion: string,
    ): ResolvedSampleDatasetVersion | null {
        const dataset = manifest.datasets[datasetName];
        if (!dataset || dataset.versions.length === 0) return null;

        const matching = dataset.versions.filter(v => satisfies(serverVersion, v.compatibleServers, { includePrerelease: true }));
        if (matching.length > 0) {
            const newest = newestByVersion(matching);
            return { ...newest, datasetName, isExactMatch: true };
        }

        const newest = newestByVersion(dataset.versions);
        return { ...newest, datasetName, isExactMatch: false };
    }
}

function newestByVersion(versions: SampleDatasetVersion[]): SampleDatasetVersion {
    return [...versions].sort((a, b) => semverCompare(b.version, a.version))[0];
}
