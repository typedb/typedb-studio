/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { isApiErrorResponse } from "@typedb/driver-http";
import { Subject, switchMap } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent, FormPasswordInputComponent, requiredValidator } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";

@Component({
    selector: "ts-user-create-dialog",
    templateUrl: "./user-create-dialog.component.html",
    styleUrls: ["./user-create-dialog.component.scss"],
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, MatFormFieldModule,
        MatInputModule, FormComponent, FormActionsComponent, FormPasswordInputComponent, FormInputComponent,
    ]
})
export class UserCreateDialogComponent {

    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        username: ["", [requiredValidator]],
        password: ["", [requiredValidator]],
    });
    errorLines: string[] = [];

    constructor(
        private dialogRef: MatDialogRef<UserCreateDialogComponent>,
        private formBuilder: FormBuilder, private snackbar: SnackbarService, private driver: DriverState,
    ) {
    }

    submit() {
        this.driver.connection$.subscribe(connection => {
            if (!connection) {
                this.close();
                this.snackbar.errorPersistent(`No server connected - could not create user`);
            }
        });
        this.driver.createUser(this.form.value.username!, this.form.value.password!).pipe(
            switchMap(() => this.driver.refreshUserList())
        ).subscribe({
            next: () => {
                this.close();
                this.snackbar.success(`User '${this.form.value.username}' created.`);
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
