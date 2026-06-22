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

/**
 * Lazy opt-in gate shown the first time a user invokes any AI Agent feature.
 * Closes with `true` only when the user ticks the consent checkbox and confirms;
 * any other dismissal closes with `undefined` (treated as "declined").
 */
@Component({
    selector: "ts-ai-consent-grant-dialog",
    templateUrl: "./ai-consent-grant-dialog.component.html",
    styleUrls: ["./ai-consent-dialog.component.scss"],
    imports: [ModalComponent, FormActionsComponent, FormsModule, MatCheckboxModule, AsyncPipe],
})
export class AiConsentGrantDialogComponent {
    isSubmitting$ = new Subject<boolean>();
    accepted = false;

    constructor(private dialogRef: MatDialogRef<AiConsentGrantDialogComponent>) {}

    confirm(): void {
        if (!this.accepted) return;
        this.dialogRef.close(true);
    }

    close(): void {
        this.dialogRef.close();
    }
}
