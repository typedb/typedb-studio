/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { FormActionsComponent } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { QueryPageState } from "../../../service/query-page-state.service";

@Component({
    selector: "ts-query-options-dialog",
    templateUrl: "./query-options-dialog.component.html",
    styleUrls: ["./query-options-dialog.component.scss"],
    imports: [ModalComponent, ReactiveFormsModule, MatFormFieldModule, MatSelectModule, FormActionsComponent],
})
export class QueryOptionsDialogComponent {

    constructor(
        public state: QueryPageState,
        private dialogRef: MatDialogRef<QueryOptionsDialogComponent>,
    ) {}

    close() {
        this.dialogRef.close();
    }
}
