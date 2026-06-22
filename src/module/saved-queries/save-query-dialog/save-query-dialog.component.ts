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

export interface SaveQueryDialogData {
    suggestedName: string;
}

/**
 * Prompts the user for a name when saving a query. The query page passes
 * either the current tab name (if the user has renamed it) or an empty
 * string (if it's still the auto-generated "Query N" form); the chat page
 * passes the conversation title as a sensible default.
 */
@Component({
    selector: "ts-save-query-dialog",
    templateUrl: "./save-query-dialog.component.html",
    imports: [
        ModalComponent, FormsModule, ReactiveFormsModule, AsyncPipe,
        FormComponent, FormInputComponent, FormActionsComponent,
    ],
})
export class SaveQueryDialogComponent {

    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        name: [this.data.suggestedName, [requiredValidator]],
    });

    constructor(
        private dialogRef: MatDialogRef<SaveQueryDialogComponent>,
        private formBuilder: FormBuilder,
        @Inject(MAT_DIALOG_DATA) private data: SaveQueryDialogData,
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
