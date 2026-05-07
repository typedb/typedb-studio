/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe, DatePipe } from "@angular/common";
import { Component } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDialogRef } from "@angular/material/dialog";
import { MatTooltipModule } from "@angular/material/tooltip";
import { map } from "rxjs";
import { Transaction } from "../../../../concept/transaction";
import { FormActionsComponent } from "../../../../framework/form";
import { ModalComponent } from "../../../../framework/modal";
import { DriverState } from "../../../../service/driver-state.service";

@Component({
    selector: "ts-transaction-manager-dialog",
    templateUrl: "./transaction-manager-dialog.component.html",
    styleUrls: ["./transaction-manager-dialog.component.scss"],
    imports: [ModalComponent, AsyncPipe, DatePipe, MatButtonModule, MatTooltipModule, FormActionsComponent],
})
export class TransactionManagerDialogComponent {

    lastTx$ = this.driver.lastTransaction$;
    isOpen$ = this.lastTx$.pipe(map(tx => tx != null && !tx.closedAtTimestamp));
    closedAt$ = this.lastTx$.pipe(map(tx => tx?.closedAtTimestamp ? new Date(tx.closedAtTimestamp) : null));

    constructor(
        public driver: DriverState,
        private dialogRef: MatDialogRef<TransactionManagerDialogComponent>,
    ) {}

    formatMetadata(tx: Transaction): string {
        const meta: any = {};
        if (tx.id !== "(oneshot)") meta.transactionId = tx.id;
        meta.options = { transactionTimeoutMillis: this.driver.transactionTimeoutSeconds * 1000 };
        return JSON.stringify(meta, null, 2);
    }

    closeTransaction() {
        this.driver.closeTransaction().subscribe();
    }

    close() {
        this.dialogRef.close();
    }
}
