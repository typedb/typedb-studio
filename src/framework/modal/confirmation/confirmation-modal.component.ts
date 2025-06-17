/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
    imports: [FormsModule, ModalComponent, FormActionsComponent, AsyncPipe]
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
