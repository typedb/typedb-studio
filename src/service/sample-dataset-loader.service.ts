/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Router } from "@angular/router";
import { forkJoin, lastValueFrom } from "rxjs";
import { first } from "rxjs/operators";
import { ResolvedSampleDatasetVersion } from "../concept/sample-dataset";
import { DriverState } from "./driver-state.service";
import { QueryPageState } from "./query-page-state.service";
import { QueryTabsState } from "./query-tabs-state.service";
import { SnackbarService } from "./snackbar.service";

@Injectable({
    providedIn: "root",
})
export class SampleDatasetLoaderService {

    private http = inject(HttpClient);
    private router = inject(Router);
    private driver = inject(DriverState);
    private queryTabs = inject(QueryTabsState);
    private queryPageState = inject(QueryPageState);
    private snackbar = inject(SnackbarService);

    async load(resolved: ResolvedSampleDatasetVersion): Promise<void> {
        const { datasetName } = resolved;
        const progress = this.snackbar.infoPersistent(
            `Loading '${datasetName}' sample dataset, please wait…`
        );

        try {
            await this.router.navigate(["/query"]);

            await lastValueFrom(this.driver.closeTransaction());
            this.driver.transactionControls.controls.operationMode.setValue("auto");

            const [schemaTql, dataTql] = await lastValueFrom(
                forkJoin([
                    this.http.get(resolved.schemaFile, { responseType: "text" }),
                    this.http.get(resolved.dataFile, { responseType: "text" }),
                ])
            );

            const schemaOk = await this.runInNewTab(`${datasetName} schema`, schemaTql);
            if (!schemaOk) {
                progress.dismiss();
                this.snackbar.errorPersistent(
                    `Failed to load '${datasetName}' schema. Data load aborted. See query output for details.`
                );
                return;
            }

            const dataOk = await this.runInNewTab(`${datasetName} data`, dataTql);
            progress.dismiss();
            if (!dataOk) {
                this.snackbar.errorPersistent(
                    `Loaded '${datasetName}' schema, but data load failed. See query output for details.`
                );
                return;
            }

            this.snackbar.success(`Loaded '${datasetName}' sample dataset.`);
        } catch (err) {
            progress.dismiss();
            console.error("Sample dataset load failed:", err);
            this.snackbar.errorPersistent(
                `Failed to load '${datasetName}' sample dataset: ${this.formatError(err)}`
            );
        }
    }

    private formatError(err: unknown): string {
        if (err instanceof HttpErrorResponse) {
            return `${err.status} ${err.statusText} (${err.url})`;
        }
        if (err instanceof Error) {
            return err.message;
        }
        if (typeof err === "object" && err !== null) {
            const apiMessage = (err as any)?.err?.message;
            if (typeof apiMessage === "string") return apiMessage;
            try { return JSON.stringify(err); } catch { /* fall through */ }
        }
        return String(err);
    }

    private async runInNewTab(tabName: string, query: string): Promise<boolean> {
        const tab = this.queryTabs.newTab();
        this.queryTabs.renameTab(tab, tabName);
        this.queryTabs.getTabControl(tab).setValue(query);
        const result = await lastValueFrom(this.queryPageState.runQuery(query).pipe(first()));
        return result.success;
    }
}
