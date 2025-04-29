/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { version } from "../../../package.json";
import { AsyncPipe } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { ModalComponent, ModalCloseButtonComponent } from "../../framework/modal";
import { FormActionsComponent, FormComponent, FormInputComponent } from "../../framework/form";
import { ButtonComponent } from "../../framework/button/button.component";
import { SnackbarService } from "../../service/snackbar.service";

@Component({
    selector: "ts-app-info-dialog",
    templateUrl: "./app-info-dialog.component.html",
    styleUrls: ["./app-info-dialog.component.scss"],
    standalone: true,
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, ButtonComponent, MatFormFieldModule,
        MatInputModule, FormComponent, FormInputComponent, FormActionsComponent, ModalCloseButtonComponent
    ],
})
export class AppInfoDialogComponent {

    constructor(
        private dialogRef: MatDialogRef<AppInfoDialogComponent>,
        private snackbar: SnackbarService
    ) {
    }

    close() {
        this.dialogRef.close();
    }

    get version() {
        return version;
    }
}
