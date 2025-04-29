/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { Subject } from "rxjs";
import { Database } from "../../../concept/connection";
import { ButtonComponent } from "../../../framework/button/button.component";
import { FormActionsComponent, FormComponent, FormInputComponent, patternValidator, requiredValidator } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
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

    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        name: ["", [patternValidator(/\w+/, `Special characters are not allowed`), requiredValidator]],
    });

    constructor(
        private dialogRef: MatDialogRef<DatabaseCreateDialogComponent>,
        private formBuilder: FormBuilder, private snackbar: SnackbarService, private driver: DriverState,
    ) {
    }

    submit() {
        this.driver.createAndSelectDatabase(this.form.value.name!).subscribe({
            next: () => {
                this.close();
                this.snackbar.success("Database created successfully");
            },
            error: () => {
                this.isSubmitting$.next(false);
            },
        });
    }

    close() {
        this.dialogRef.close();
    }
}
