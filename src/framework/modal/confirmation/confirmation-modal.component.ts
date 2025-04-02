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
import { ButtonComponent, ButtonStyle } from "../../button/button.component";
import { ModalComponent } from "../modal.component";
import { FormActionsComponent } from "../../form";

import { Subject } from "rxjs";
import { AsyncPipe } from "@angular/common";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

export type ConfirmationModalData =
    { title: string, body?: string, confirmText?: string, confirmButtonStyle?: ButtonStyle };

@Component({
    selector: "tp-confirmation-modal",
    templateUrl: "./confirmation-modal.component.html",
    styleUrls: ["./confirmation-modal.component.scss"],
    standalone: true,
    imports: [FormsModule, ButtonComponent, ModalComponent, FormActionsComponent, AsyncPipe],
})
export class ConfirmationModalComponent {
    @Output() confirmed = new EventEmitter<void>();
    isSubmitting$ = new Subject<boolean>();

    constructor(
        private dialogRef: MatDialogRef<ConfirmationModalComponent>,
        @Inject(MAT_DIALOG_DATA) public data: ConfirmationModalData,
    ) {}

    confirm(): void {
        this.isSubmitting$.next(true);
        this.confirmed.emit();
    }

    close(): void {
        this.dialogRef.close();
    }
}
