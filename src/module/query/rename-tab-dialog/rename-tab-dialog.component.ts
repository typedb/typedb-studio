/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { Subject } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent, requiredValidator } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";

export interface RenameTabDialogData {
    currentName: string;
}

@Component({
    selector: "ts-rename-tab-dialog",
    templateUrl: "./rename-tab-dialog.component.html",
    styleUrls: ["./rename-tab-dialog.component.scss"],
    imports: [
        ModalComponent, FormsModule, ReactiveFormsModule, AsyncPipe,
        FormComponent, FormInputComponent, FormActionsComponent,
    ]
})
export class RenameTabDialogComponent {

    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        name: [this.data.currentName, [requiredValidator]],
    });

    constructor(
        private dialogRef: MatDialogRef<RenameTabDialogComponent>,
        private formBuilder: FormBuilder,
        @Inject(MAT_DIALOG_DATA) private data: RenameTabDialogData,
    ) {
    }

    submit() {
        const newName = this.form.value.name?.trim();
        if (newName) {
            this.dialogRef.close(newName);
        }
    }

    close() {
        this.dialogRef.close();
    }
}
