/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { FormBuilder, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { combineLatest, map } from "rxjs";
import { INTERNAL_ERROR } from "../../../framework/util/strings";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { TransactionType } from "@typedb/driver-http";
import { OperationMode } from "../../../concept/transaction";

@Component({
    selector: "ts-transaction-control",
    templateUrl: "./transaction-control.component.html",
    styleUrls: ["./transaction-control.component.scss"],
    imports: [MatTooltipModule, AsyncPipe, MatSelectModule, ReactiveFormsModule, MatIconModule, MatButtonModule]
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

    constructor(public driver: DriverState, private formBuilder: FormBuilder, private snackbar: SnackbarService) {
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
        });
    }

    open() {
        if (!this.driver.transactionControls.value.type) throw new Error(INTERNAL_ERROR);
        this.driver.openTransaction(this.driver.transactionControls.value.type).subscribe();
    }

    commit() {
        this.driver.commitTransaction().subscribe();
    }

    close() {
        this.driver.closeTransaction().subscribe();
    }
}
