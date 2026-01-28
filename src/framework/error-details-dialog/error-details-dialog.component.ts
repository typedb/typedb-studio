/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";

export interface ErrorDetailsDialogData {
    message: string;
}

@Component({
    selector: "tp-error-details-dialog",
    templateUrl: "./error-details-dialog.component.html",
    styleUrls: ["./error-details-dialog.component.scss"],
    imports: [MatButtonModule],
})
export class ErrorDetailsDialogComponent {
    copied = false;

    constructor(
        private dialogRef: MatDialogRef<ErrorDetailsDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: ErrorDetailsDialogData,
    ) {}

    close() {
        this.dialogRef.close();
    }

    copyToClipboard() {
        navigator.clipboard.writeText(this.data.message);
        this.copied = true;
        setTimeout(() => this.copied = false, 1500);
    }
}
