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
import { isApiErrorResponse } from "@typedb/driver-http";
import { Subject, switchMap } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";

@Component({
    selector: "ts-user-delete-dialog",
    templateUrl: "./user-delete-dialog.component.html",
    styleUrls: ["./user-delete-dialog.component.scss"],
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, MatFormFieldModule,
        MatInputModule, FormComponent, FormActionsComponent
    ]
})
export class UserDeleteDialogComponent {

    readonly form = this.formBuilder.nonNullable.group({});
    readonly isSubmitting$ = new Subject<boolean>();
    readonly data = inject<{ username: string }>(MAT_DIALOG_DATA);
    errorLines: string[] = [];

    constructor(
        private dialogRef: MatDialogRef<UserDeleteDialogComponent>,
        private snackbar: SnackbarService, private driver: DriverState, private formBuilder: FormBuilder
    ) {
    }

    submit() {
        this.driver.connection$.subscribe(connection => {
            if (!connection) {
                this.close();
                this.snackbar.errorPersistent(`No server connected - could not delete user`);
            }
        });
        this.driver.deleteUser(this.data.username).pipe(
            switchMap(() => this.driver.refreshUserList())
        ).subscribe({
            next: () => {
                this.close();
                this.snackbar.success(`User '${this.data.username}' deleted.`);
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
