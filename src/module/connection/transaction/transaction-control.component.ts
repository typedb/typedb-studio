/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { FormBuilder, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { combineLatest, map } from "rxjs";
import { INTERNAL_ERROR } from "../../../framework/util/strings";
import { AppData } from "../../../service/app-data.service";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { TransactionType } from "@typedb/driver-http";
import { OperationMode } from "../../../concept/transaction";
import { TransactionManagerDialogComponent } from "./manager-dialog/transaction-manager-dialog.component";
import { TransactionOptionsDialogComponent } from "./options-dialog/transaction-options-dialog.component";

@Component({
    selector: "ts-transaction-control",
    templateUrl: "./transaction-control.component.html",
    styleUrls: ["./transaction-control.component.scss"],
    imports: [MatTooltipModule, AsyncPipe, MatSelectModule, ReactiveFormsModule, MatIconModule, MatButtonModule, MatMenuModule, MatDividerModule]
})
export class TransactionControlComponent {

    transactionTypes: TransactionType[] = ["read", "write", "schema"];
    operationModes: OperationMode[] = ["auto", "manual"];
    hasUncommittedChanges$ = this.driver.transactionHasUncommittedChanges$;
    transactionTypeVisible$ = this.driver.transactionOperationModeChanges$.pipe(map((mode) => mode === "manual"));
    commitButtonDisabled$ = this.hasUncommittedChanges$.pipe(map(x => !x));
    transactionWidgetTooltip$ = this.hasUncommittedChanges$.pipe(map(x => x ? `Has uncommitted changes` : ``));
    closeButtonDisabled$ = this.driver.transaction$.pipe(map(tx => !tx));
    transactionIconClass$ = this.driver.transaction$.pipe(map(tx => tx ? "fa-solid fa-code-commit active" : "fa-regular fa-code-commit"));
    openButtonClass$ = this.driver.transaction$.pipe(map(tx => tx ? "open-btn active" : "open-btn"));
    openButtonIconClass$ = this.driver.transaction$.pipe(map(tx => tx ? "fa-regular fa-arrow-rotate-right" : "fa-regular fa-play"));
    transactionConfigDisabled$ = this.driver.database$.pipe(map(db => !db));
    openButtonDisabled$ = combineLatest([this.driver.database$, this.driver.transaction$]).pipe(map(([db, tx]) => !db || !!tx));
    openButtonVisible$ = this.driver.transactionOperationModeChanges$.pipe(map((mode) => mode !== "auto"));
    commitButtonVisible$ = combineLatest([this.driver.transactionTypeChanges$, this.driver.transactionOperationModeChanges$]).pipe(
        map(([type, mode]) => type !== "read" && mode === "manual")
    );
    closeButtonVisible$ = this.openButtonVisible$;

    constructor(public driver: DriverState, private formBuilder: FormBuilder, private snackbar: SnackbarService, private appData: AppData, private dialog: MatDialog) {
        this.transactionConfigDisabled$.subscribe((disabled) => {
            if (disabled) this.driver.transactionControls.disable();
            else this.driver.transactionControls.enable();
        });
        this.driver.transactionTypeChanges$.subscribe((type) => {
            // TODO: confirm before closing with uncommitted changes
            this.driver.closeTransaction().subscribe();
        });
        this.driver.transactionOperationModeChanges$.subscribe((operationMode) => {
            // TODO: confirm before closing with uncommitted changes
            this.driver.closeTransaction().subscribe();
            this.driver.autoTransactionEnabled$.next(operationMode === "auto");
            this.appData.preferences.setTransactionMode(operationMode);
        });
    }

    setOperationMode(mode: OperationMode) {
        this.driver.transactionControls.controls.operationMode.setValue(mode);
    }

    openTransactionInspector() {
        this.dialog.open(TransactionManagerDialogComponent, { width: "560px" });
    }

    openTransactionOptions() {
        this.dialog.open(TransactionOptionsDialogComponent, { width: "460px" });
    }

    open() {
        if (!this.driver.transactionControls.value.type) throw new Error(INTERNAL_ERROR);
        this.driver.openTransaction(this.driver.transactionControls.value.type).subscribe({
            error: (err) => {
                let msg = ``;
                if (typeof err === "object" && "err" in err && err.err?.message) {
                    msg = err.err.message;
                } else {
                    msg = err?.message ?? err?.toString() ?? `Unknown error`;
                }
                this.snackbar.errorPersistent(`Error: ${msg}\nCaused: Failed to open transaction.`);
            }
        });
    }

    commit() {
        this.driver.commitTransaction().subscribe({
            error: (err) => {
                let msg = ``;
                if (typeof err === "object" && "err" in err && err.err?.message) {
                    msg = err.err.message;
                } else {
                    msg = err?.message ?? err?.toString() ?? `Unknown error`;
                }
                this.snackbar.errorPersistent(`Error: ${msg}\nCaused: Failed to commit transaction.`);
            }
        });
    }

    close() {
        this.driver.closeTransaction().subscribe();
    }
}
