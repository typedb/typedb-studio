/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

export interface SampleDatasetManifest {
    schemaVersion: number;
    datasets: Record<string, SampleDataset>;
}

export interface SampleDataset {
    displayName: string;
    description: string;
    homepage: string;
    versions: SampleDatasetVersion[];
}

export interface SampleDatasetVersion {
    version: string;
    compatibleServers: string;
    schemaFile: string;
    dataFile: string;
}

export interface ResolvedSampleDatasetVersion extends SampleDatasetVersion {
    datasetName: string;
    isExactMatch: boolean;
}
