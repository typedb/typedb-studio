/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component } from "@angular/core";
import { AsyncPipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatDialogRef } from "@angular/material/dialog";
import { Subject } from "rxjs";
import { ModalComponent } from "../../../framework/modal";
import { FormActionsComponent } from "../../../framework/form";

export interface AiConsentWithdrawResult {
    confirmed: boolean;
    clearHistory: boolean;
}

/**
 * Confirmation shown when withdrawing AI consent. Closes with
 * `{ confirmed, clearHistory }`; `clearHistory` reflects an opt-in checkbox that
 * is unchecked by default (so history is retained unless explicitly requested).
 */
@Component({
    selector: "ts-ai-consent-withdraw-dialog",
    templateUrl: "./ai-consent-withdraw-dialog.component.html",
    styleUrls: ["./ai-consent-dialog.component.scss"],
    imports: [ModalComponent, FormActionsComponent, FormsModule, MatCheckboxModule, AsyncPipe],
})
export class AiConsentWithdrawDialogComponent {
    isSubmitting$ = new Subject<boolean>();
    clearHistory = false;

    constructor(private dialogRef: MatDialogRef<AiConsentWithdrawDialogComponent, AiConsentWithdrawResult>) {}

    confirm(): void {
        this.dialogRef.close({ confirmed: true, clearHistory: this.clearHistory });
    }

    close(): void {
        this.dialogRef.close({ confirmed: false, clearHistory: false });
    }
}
