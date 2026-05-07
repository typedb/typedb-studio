/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { AbstractControl, AsyncValidatorFn, FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { isApiErrorResponse } from "@typedb/driver-http";
import { combineLatest, filter, first, map, Observable, shareReplay, startWith, Subject } from "rxjs";
import { ResolvedSampleDatasetVersion, SampleDataset, SampleDatasetManifest } from "../../../concept/sample-dataset";
import { FormActionsComponent, FormComponent, FormInputComponent, patternValidator, requiredValidator } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { DriverState } from "../../../service/driver-state.service";
import { SampleDatasetLoaderService } from "../../../service/sample-dataset-loader.service";
import { SampleDatasetsService } from "../../../service/sample-datasets.service";
import { SnackbarService } from "../../../service/snackbar.service";

const NO_DATASET = "";

interface DatasetOptionData {
    name: string;
    displayName: string;
    description: string;
    resolved: ResolvedSampleDatasetVersion | null;
}

@Component({
    selector: "ts-database-create-dialog",
    templateUrl: "./database-create-dialog.component.html",
    styleUrls: ["./database-create-dialog.component.scss"],
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, MatFormFieldModule,
        MatInputModule, MatSelectModule, FormComponent, FormInputComponent, FormActionsComponent,
    ]
})
export class DatabaseCreateDialogComponent {

    private uniqueValidator: AsyncValidatorFn = (control: AbstractControl<string>) => {
        return this.driver.databaseList$.pipe(
            first(),
            map((databases) => databases?.some(x => x.name === control.value) ?? false),
            map((hasConflict) => hasConflict ? { errorText: `A database named '${control.value}' already exists` } : null)
        );
    }

    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        name: ["", [patternValidator(/^[\w-_]+$/, `Spaces and special characters are not allowed (except - and _)`), requiredValidator], [this.uniqueValidator]],
        datasetName: [NO_DATASET],
    });
    errorLines: string[] = [];

    readonly datasets$: Observable<DatasetOptionData[]> = combineLatest([
        this.datasetsService.manifest$(),
        this.driver.serverVersion$,
    ]).pipe(
        map(([manifest, sv]) => this.buildOptions(manifest, sv?.version ?? null)),
        shareReplay({ bufferSize: 1, refCount: true }),
    );

    readonly selectedDataset$: Observable<DatasetOptionData | null> = combineLatest([
        this.datasets$,
        this.form.controls.datasetName.valueChanges.pipe(startWith(this.form.controls.datasetName.value)),
    ]).pipe(
        map(([list, name]) => list.find(d => d.name === name) ?? null),
    );

    constructor(
        private dialogRef: MatDialogRef<DatabaseCreateDialogComponent>,
        private formBuilder: FormBuilder, private snackbar: SnackbarService, private driver: DriverState,
        private datasetsService: SampleDatasetsService, private loader: SampleDatasetLoaderService,
    ) {
        this.driver.databaseList$.pipe(filter(x => x != null), first()).subscribe(databases => {
            if (!databases.length) {
                this.form.controls.name.setValue("default");
            }
        });
    }

    private buildOptions(manifest: SampleDatasetManifest, serverVersion: string | null): DatasetOptionData[] {
        return Object.entries(manifest.datasets).map(([name, dataset]: [string, SampleDataset]) => ({
            name,
            displayName: dataset.displayName,
            description: dataset.description,
            resolved: serverVersion ? this.datasetsService.resolveForServer(manifest, name, serverVersion) : null,
        }));
    }

    submit() {
        const name = this.form.value.name!;
        const datasetName = this.form.value.datasetName ?? NO_DATASET;
        this.driver.createAndSelectDatabase(name).subscribe({
            next: () => {
                this.close();
                this.snackbar.success(`Created and connected to database '${name}'`);
                if (datasetName !== NO_DATASET) {
                    this.datasets$.pipe(first()).subscribe(list => {
                        const selected = list.find(d => d.name === datasetName);
                        if (selected?.resolved) this.loader.load(selected.resolved);
                    });
                }
            },
            error: (err) => {
                this.isSubmitting$.next(false);
                let error = ``;
                if (isApiErrorResponse(err)) {
                    error = err.err.message;
                } else {
                    error = err?.message ?? err?.toString();
                }
                this.errorLines = error.split(`\n`);
            },
        });
    }

    close() {
        this.dialogRef.close();
    }
}
