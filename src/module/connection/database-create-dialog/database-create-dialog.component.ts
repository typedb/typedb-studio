/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { AbstractControl, AsyncValidatorFn, FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { combineLatest, first, map, Subject } from "rxjs";
import { ButtonComponent } from "../../../framework/button/button.component";
import { FormActionsComponent, FormComponent, FormInputComponent, patternValidator, requiredValidator } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { isApiErrorResponse } from "../../../framework/typedb-driver/response";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";

@Component({
    selector: "ts-database-create-dialog",
    templateUrl: "./database-create-dialog.component.html",
    styleUrls: ["./database-create-dialog.component.scss"],
    standalone: true,
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, ButtonComponent, MatFormFieldModule,
        MatInputModule, FormComponent, FormInputComponent, FormActionsComponent
    ],
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
    });
    errorLines: string[] = [];

    constructor(
        private dialogRef: MatDialogRef<DatabaseCreateDialogComponent>,
        private formBuilder: FormBuilder, private snackbar: SnackbarService, private driver: DriverState,
    ) {
    }

    submit() {
        this.driver.createAndSelectDatabase(this.form.value.name!).subscribe({
            next: () => {
                this.close();
                this.snackbar.success(`Database '${this.form.value.name}' created successfully`);
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
