/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { StaticCodeComponent } from "../code-editor/static-code.component";

export interface QueryTextDialogData {
    query: string;
}

@Component({
    selector: "tp-query-text-dialog",
    templateUrl: "./query-text-dialog.component.html",
    styleUrls: ["./query-text-dialog.component.scss"],
    imports: [MatButtonModule, StaticCodeComponent],
})
export class QueryTextDialogComponent {
    copied = false;

    constructor(
        private dialogRef: MatDialogRef<QueryTextDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: QueryTextDialogData,
    ) {}

    close() {
        this.dialogRef.close();
    }

    copyToClipboard() {
        navigator.clipboard.writeText(this.data.query);
        this.copied = true;
        setTimeout(() => this.copied = false, 1500);
    }
}
