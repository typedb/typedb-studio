/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, inject, OnInit } from "@angular/core";
import { AbstractControl, AsyncValidatorFn, FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { isApiErrorResponse } from "@typedb/driver-http";
import { combineLatest, first, map, Observable, shareReplay, startWith, Subject } from "rxjs";
import { ResolvedSampleDatasetVersion, SampleDataset, SampleDatasetManifest } from "../../../concept/sample-dataset";
import { FormActionsComponent, FormComponent, FormInputComponent, patternValidator, requiredValidator } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { DriverState } from "../../../service/driver-state.service";
import { SampleDatasetLoaderService } from "../../../service/sample-dataset-loader.service";
import { SampleDatasetsService } from "../../../service/sample-datasets.service";
import { SnackbarService } from "../../../service/snackbar.service";

interface DatasetOptionData {
    name: string;
    displayName: string;
    description: string;
    resolved: ResolvedSampleDatasetVersion | null;
}

@Component({
    selector: "ts-sample-dataset-dialog",
    templateUrl: "./sample-dataset-dialog.component.html",
    styleUrls: ["./sample-dataset-dialog.component.scss"],
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule,
        MatFormFieldModule, MatInputModule, MatSelectModule,
        FormComponent, FormInputComponent, FormActionsComponent,
    ]
})
export class SampleDatasetDialogComponent implements OnInit {

    private formBuilder = inject(FormBuilder);
    private dialogRef = inject(MatDialogRef<SampleDatasetDialogComponent>);
    private driver = inject(DriverState);
    private snackbar = inject(SnackbarService);
    private datasetsService = inject(SampleDatasetsService);
    private loader = inject(SampleDatasetLoaderService);

    private uniqueValidator: AsyncValidatorFn = (control: AbstractControl<string>) => {
        return this.driver.databaseList$.pipe(
            first(),
            map((databases) => databases?.some(x => x.name === control.value) ?? false),
            map((hasConflict) => hasConflict ? { errorText: `A database named '${control.value}' already exists. Please choose a different name.` } : null)
        );
    }

    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        name: ["", [patternValidator(/^[\w-_]+$/, `Spaces and special characters are not allowed (except - and _)`), requiredValidator], [this.uniqueValidator]],
        datasetName: [""],
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

    readonly errorMessage$ = this.datasets$.pipe(
        map(list => list.length === 0 ? `No sample datasets available for this server.` : null),
    );

    loadError: string | null = null;

    ngOnInit() {
        this.selectedDataset$.subscribe(selected => {
            this.form.controls.name.setValue(selected?.name ?? "");
            // Mark as touched so the async unique validator's error (if any) is shown immediately.
            this.form.controls.name.markAsTouched();
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
        const datasetName = this.form.value.datasetName!;
        this.datasets$.pipe(first()).subscribe(list => {
            const selected = list.find(d => d.name === datasetName);
            if (!selected?.resolved) {
                this.loadError = `No installable version of '${datasetName}' is available.`;
                return;
            }
            this.driver.createAndSelectDatabase(name).subscribe({
                next: () => {
                    this.close();
                    this.snackbar.success(`Created and connected to database '${name}'`);
                    this.loader.load(selected.resolved!);
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
        });
    }

    close() {
        this.dialogRef.close();
    }
}
