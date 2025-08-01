/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatDivider } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { Database, isApiErrorResponse } from "@samuel-butcher-typedb/typedb-http-driver";
import { Subject } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";

@Component({
    selector: "ts-database-delete-dialog",
    templateUrl: "./database-delete-dialog.component.html",
    styleUrls: ["./database-delete-dialog.component.scss"],
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, MatFormFieldModule,
        MatInputModule, FormComponent, FormInputComponent, FormActionsComponent, MatDivider,
    ]
})
export class DatabaseDeleteDialogComponent {

    readonly isSubmitting$ = new Subject<boolean>();
    readonly data = inject<{ db: Database }>(MAT_DIALOG_DATA);
    readonly form = this.formBuilder.nonNullable.group({
        confirmationText: [""],
    });
    errorLines: string[] = [];

    constructor(
        private dialogRef: MatDialogRef<DatabaseDeleteDialogComponent>,
        private formBuilder: FormBuilder, private snackbar: SnackbarService, private driver: DriverState,
    ) {
    }

    get strongConfirmationString(): string {
        return this.data.db.name;
    }

    get canSubmit() {
        return this.form.value.confirmationText === this.strongConfirmationString;
    }

    submit() {
        if (this.form.value.confirmationText !== this.strongConfirmationString) {
            return;
        }

        this.driver.deleteDatabase(this.data.db).subscribe({
            next: () => {
                this.close();
                this.snackbar.info(`Deleted database '${this.data.db.name}'`);
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
