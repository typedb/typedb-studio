/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { isApiErrorResponse } from "typedb-driver-http";
import { of, Subject, switchMap } from "rxjs";
import { FormActionsComponent, FormComponent, FormPasswordInputComponent, requiredValidator } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";

@Component({
    selector: "ts-user-change-password-dialog",
    templateUrl: "./user-change-password-dialog.component.html",
    styleUrls: ["./user-change-password-dialog.component.scss"],
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, MatFormFieldModule,
        MatInputModule, FormComponent, FormActionsComponent, FormPasswordInputComponent,
    ]
})
export class UserChangePasswordDialogComponent {

    readonly data = inject<{ username: string }>(MAT_DIALOG_DATA);
    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        newPassword: ["", [requiredValidator]],
    });
    errorLines: string[] = [];

    constructor(
        private dialogRef: MatDialogRef<UserChangePasswordDialogComponent>,
        private formBuilder: FormBuilder, private snackbar: SnackbarService, private driver: DriverState,
    ) {
    }

    get isEditingCurrentlyLoggedInUser() {
        return this.data.username === this.driver.connection$.value?.params.username;
    }

    submit() {
        this.driver.connection$.subscribe(connection => {
            if (!connection) {
                this.close();
                this.snackbar.errorPersistent(`No server connected - could not change password`);
            }
        });
        this.driver.updateUser(this.data.username, this.form.value.newPassword!).pipe(
            switchMap(() => {
                if (this.isEditingCurrentlyLoggedInUser) {
                    return this.driver.tryDisconnect();
                } else {
                    return of({});
                }
            })
        ).subscribe({
            next: () => {
                this.close();
                if (this.isEditingCurrentlyLoggedInUser) {
                    this.snackbar.info(`Password changed for user '${this.data.username}'. You can now reconnect with the new credentials.`);
                } else {
                    this.snackbar.success(`Password changed for user '${this.data.username}'.`);
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
