/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidatorFn } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { Subject } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent, requiredValidator } from "../../../../framework/form";
import { ModalComponent } from "../../../../framework/modal";
import { connectionString } from "../../../../concept/connection";

const addressValidator: ValidatorFn = (control: AbstractControl<string>) => {
    const value = control.value;
    if (!value) return null; // Let requiredValidator handle empty
    if (!value.startsWith(`http://`) && !value.startsWith(`https://`)) {
        return { errorText: `Please specify http:// or https://` };
    }
    return null;
};

@Component({
    selector: "ts-connection-string-editor-dialog",
    templateUrl: "./connection-string-editor-dialog.component.html",
    styleUrls: ["./connection-string-editor-dialog.component.scss"],
    imports: [
        AsyncPipe, ModalComponent, ReactiveFormsModule, MatFormFieldModule,
        MatInputModule, MatButtonModule, FormComponent, FormInputComponent,
        FormActionsComponent,
    ]
})
export class ConnectionStringEditorDialogComponent {

    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        address: ["", [requiredValidator, addressValidator]],
        username: ["", [requiredValidator]],
        password: ["", [requiredValidator]],
        database: [""],
        name: [""],
    });

    passwordRevealed = false;
    previewRevealed = false;

    constructor(
        private dialogRef: MatDialogRef<ConnectionStringEditorDialogComponent>,
        private formBuilder: FormBuilder,
    ) {}

    get connectionStringPreview(): string {
        const { address, username, password, database, name } = this.form.value;
        if (!address || !username) return "";
        const displayPassword = this.previewRevealed ? (password || "") : "••••••••";
        return connectionString({
            addresses: [address],
            username: username,
            password: displayPassword,
            database: database || undefined,
            name: name || undefined,
        });
    }

    get connectionStringActual(): string {
        const { address, username, password, database, name } = this.form.value;
        if (!address || !username) return "";
        return connectionString({
            addresses: [address],
            username: username,
            password: password || "",
            database: database || undefined,
            name: name || undefined,
        });
    }

    submit() {
        if (this.form.invalid) return;
        this.dialogRef.close(this.connectionStringActual);
    }

    close() {
        this.dialogRef.close();
    }
}
