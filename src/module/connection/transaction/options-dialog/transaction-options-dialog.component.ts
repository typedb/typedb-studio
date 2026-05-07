/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { FormActionsComponent } from "../../../../framework/form";
import { ModalComponent } from "../../../../framework/modal";
import { AppData } from "../../../../service/app-data.service";
import { DriverState } from "../../../../service/driver-state.service";

@Component({
    selector: "ts-transaction-options-dialog",
    templateUrl: "./transaction-options-dialog.component.html",
    styleUrls: ["./transaction-options-dialog.component.scss"],
    imports: [ModalComponent, FormsModule, MatButtonModule, FormActionsComponent],
})
export class TransactionOptionsDialogComponent {

    timeoutSeconds = this.driver.transactionTimeoutSeconds;

    constructor(
        public driver: DriverState,
        private appData: AppData,
        private dialogRef: MatDialogRef<TransactionOptionsDialogComponent>,
    ) {}

    onTimeoutChange(value: number) {
        this.timeoutSeconds = value;
        this.driver.transactionTimeoutSeconds = value;
        this.appData.preferences.setTransactionTimeoutSeconds(value);
    }

    close() {
        this.dialogRef.close();
    }
}
