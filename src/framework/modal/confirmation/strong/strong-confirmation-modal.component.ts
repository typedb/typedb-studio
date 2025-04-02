/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
    standalone: true,
    imports: [FormsModule, ButtonComponent, ModalComponent, FormActionsComponent, AsyncPipe, FormActionsComponent, MatFormFieldModule, MatInputModule],
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
