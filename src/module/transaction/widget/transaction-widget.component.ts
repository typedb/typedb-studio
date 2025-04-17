/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, HostBinding } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { combineLatest, map, startWith } from "rxjs";
import { ReadMode, Transaction, TransactionType } from "../../../concept/transaction";
import { requireValue } from "../../../framework/util/observable";
import { INTERNAL_ERROR } from "../../../framework/util/strings";
import { DriverState, DriverStatus } from "../../../service/driver-state.service";

@Component({
    selector: "ts-transaction-widget",
    templateUrl: "./transaction-widget.component.html",
    styleUrls: ["./transaction-widget.component.scss"],
    standalone: true,
    imports: [MatTooltipModule, AsyncPipe, MatSelectModule, ReactiveFormsModule, MatIconModule, MatButtonModule],
})
export class TransactionWidgetComponent {

    form = this.formBuilder.nonNullable.group({
        type: ["read" as TransactionType, []],
        readMode: ["auto" as ReadMode, []],
    });
    transactionTypes: TransactionType[] = ["read", "write", "schema"];
    readModes: ReadMode[] = ["auto", "manual"];
    private typeControlValueChanges$ = this.form.controls.type.valueChanges.pipe(startWith(this.form.value.type));
    private readModeControlValueChanges$ = this.form.controls.readMode.valueChanges.pipe(startWith(this.form.value.readMode));

    transactionWidgetVisible$ = this.driver.database$.pipe(map(db => !!db));
    transactionText$ = this.driver.transaction$.pipe(map(tx => tx?.type ?? `No active transaction`));
    transactionHasUncommittedChanges$ = this.driver.transaction$.pipe(map(tx => tx?.hasUncommittedChanges ?? false));
    commitButtonDisabled$ = this.transactionHasUncommittedChanges$.pipe(map(x => !x));
    transactionWidgetTooltip$ = this.transactionHasUncommittedChanges$.pipe(map(x => x ? `Has uncommitted changes` : ``));
    closeButtonDisabled$ = this.driver.transaction$.pipe(map(tx => !tx));
    transactionIconClass$ = this.driver.transaction$.pipe(map(tx => tx ? "fa-regular fa-code-commit active" : "fa-regular fa-code-commit"));
    openButtonClass$ = this.driver.transaction$.pipe(map(tx => tx ? "open-btn active" : "open-btn"));
    openButtonIconClass$ = this.driver.transaction$.pipe(map(tx => tx ? "fa-regular fa-arrow-rotate-right" : "fa-regular fa-play"));
    openButtonTooltip$ = this.driver.transaction$.pipe(map(tx => tx ? `Reopen ${this.form.value.type} transaction` : `Open ${this.form.value.type} transaction`));
    transactionConfigDisabled$ = this.driver.database$.pipe(map(db => !db));
    openButtonDisabled$ = combineLatest([this.driver.database$, this.driver.transaction$]).pipe(map(([db, tx]) => !db || !!tx));
    openButtonVisible$ = combineLatest([this.typeControlValueChanges$, this.readModeControlValueChanges$]).pipe(
        map(([type, readMode]) => type !== "read" || readMode !== "auto")
    );
    commitButtonVisible$ = this.typeControlValueChanges$.pipe(map(x => x !== "read"));
    closeButtonVisible$ = this.openButtonVisible$;
    readModeControlVisible$ = this.typeControlValueChanges$.pipe(map(x => x === "read"));
    readModeTooltip$ = this.readModeControlValueChanges$.pipe(map(x => {
        if (x === "auto") return `Auto: Each read query will automatically run in its own transaction`;
        else return `Snapshot: Open and close read transactions manually`;
    }));

    constructor(private driver: DriverState, private formBuilder: FormBuilder) {
        this.transactionConfigDisabled$.subscribe((disabled) => {
            if (disabled) this.form.disable();
            else this.form.enable();
        });
        combineLatest([this.typeControlValueChanges$, this.readModeControlValueChanges$]).subscribe(([type, readMode]) => {
            // TODO: confirm before closing with uncommitted changes
            this.driver.closeTransaction().subscribe();
            this.driver.autoTransactionEnabled = type === "read" && readMode === "auto";
        });
    }

    open() {
        if (!this.form.value.type) throw INTERNAL_ERROR;
        this.driver.openTransaction(this.form.value.type).subscribe();
    }

    commit() {
        this.driver.commitTransaction().subscribe();
    }

    close() {
        this.driver.closeTransaction().subscribe();
    }
}
