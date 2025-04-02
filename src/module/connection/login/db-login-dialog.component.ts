/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { Subject } from "rxjs";
import { emailPattern, emailPatternErrorText } from "typedb-web-common/lib";
import { FormActionsComponent, FormComponent, FormInputComponent, FormSelectComponent, patternValidator, requiredValidator } from "../../../framework/form";
import { DialogResult } from "../../../framework/modal/dialog-result";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { ModalComponent } from "../../../framework/modal";

@Component({
    selector: "tp-db-login-dialog",
    templateUrl: "./db-login-dialog.component.html",
    styleUrls: ["./db-login-dialog.component.scss"],
    standalone: true,
    imports: [
        ModalComponent, AsyncPipe, FormInputComponent, FormSelectComponent, FormActionsComponent,
        FormsModule, ReactiveFormsModule, FormComponent, MatButtonModule, MatFormFieldModule,
    ],
})
export class DBLoginDialogComponent {
    readonly form = this.formBuilder.group({
        email: ["", [patternValidator(emailPattern, emailPatternErrorText), requiredValidator]],
        accessLevel: ["read", [requiredValidator]],
    }, {
    });
    readonly isSubmitting$ = new Subject<boolean>();

    constructor(
        private dialogRef: MatDialogRef<DBLoginDialogComponent, DialogResult>,
        private formBuilder: FormBuilder,
    ) {
    }

    submit() {
    }

    close(result?: DialogResult) {
        this.dialogRef.close(result);
    }
}
