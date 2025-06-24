/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, Inject, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { ButtonComponent } from "../../../button/button.component";
import { FormActionsComponent } from "../../../form";
import { ModalComponent } from "../../modal.component";

import { Subject } from "rxjs";
import { AsyncPipe } from "@angular/common";
import { ConfirmationModalData } from "../confirmation-modal.component";
import { MatInputModule } from "@angular/material/input";
import { MatFormFieldModule } from "@angular/material/form-field";

export type StrongConfirmationModalData = ConfirmationModalData & { strongConfirmationString: string; };

@Component({
    selector: "tp-strong-confirmation-modal",
    templateUrl: "./strong-confirmation-modal.component.html",
    styleUrls: ["./strong-confirmation-modal.component.scss"],
    imports: [FormsModule, ModalComponent, FormActionsComponent, AsyncPipe, FormActionsComponent, MatFormFieldModule, MatInputModule]
})
export class StrongConfirmationModalComponent {
    @Output() confirmed = new EventEmitter<void>();
    strongConfirmationEntry = "";
    isSubmitting$ = new Subject<boolean>();

    constructor(
        private dialogRef: MatDialogRef<StrongConfirmationModalComponent>,
        @Inject(MAT_DIALOG_DATA) public data: StrongConfirmationModalData,
    ) {}

    confirm(): void {
        this.isSubmitting$.next(true);
        this.confirmed.emit();
    }

    close(): void {
        this.dialogRef.close();
    }

    get confirmButtonDisabled(): boolean {
        return this.strongConfirmationEntry !== this.data.strongConfirmationString;
    }
}
