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
import { combineLatest, distinctUntilChanged, filter, map, startWith, switchMap } from "rxjs";
import { OperationMode } from "../../../concept/transaction";
import { TransactionType } from "../../../framework/typedb-driver/transaction";
import { INTERNAL_ERROR } from "../../../framework/util/strings";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";

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
        operationMode: ["auto" as OperationMode, []],
    });
    transactionTypes: TransactionType[] = ["read", "write", "schema"];
    operationModes: OperationMode[] = ["auto", "manual"];
    private typeControlValueChanges$ = this.form.valueChanges.pipe(
        filter((changes) => !!changes.type),
        map((changes) => changes.type!),
        startWith(this.form.value.type!),
        distinctUntilChanged(),
    );
    private operationModeControlValueChanges = this.form.valueChanges.pipe(
        filter(changes => !!changes.operationMode),
        map(changes => changes.operationMode!),
        startWith(this.form.value.operationMode!),
        distinctUntilChanged(),
    );

    hasUncommittedChanges$ = this.driver.transactionHasUncommittedChanges$;
    commitButtonDisabled$ = this.hasUncommittedChanges$.pipe(map(x => !x));
    transactionWidgetTooltip$ = this.hasUncommittedChanges$.pipe(map(x => x ? `Has uncommitted changes` : ``));
    closeButtonDisabled$ = this.driver.transaction$.pipe(map(tx => !tx));
    transactionIconClass$ = this.driver.transaction$.pipe(map(tx => tx ? "fa-solid fa-code-commit active" : "fa-regular fa-code-commit"));
    openButtonClass$ = this.driver.transaction$.pipe(map(tx => tx ? "open-btn active" : "open-btn"));
    openButtonIconClass$ = this.driver.transaction$.pipe(map(tx => tx ? "fa-regular fa-arrow-rotate-right" : "fa-regular fa-play"));
    transactionConfigDisabled$ = this.driver.database$.pipe(map(db => !db));
    openButtonDisabled$ = combineLatest([this.driver.database$, this.driver.transaction$]).pipe(map(([db, tx]) => !db || !!tx));
    openButtonVisible$ = this.operationModeControlValueChanges.pipe(map((mode) => mode !== "auto"));
    commitButtonVisible$ = combineLatest([this.typeControlValueChanges$, this.operationModeControlValueChanges]).pipe(
        map(([type, mode]) => type !== "read" && mode === "manual")
    );
    closeButtonVisible$ = this.openButtonVisible$;

    constructor(private driver: DriverState, private formBuilder: FormBuilder, private snackbar: SnackbarService) {
        this.transactionConfigDisabled$.subscribe((disabled) => {
            if (disabled) this.form.disable();
            else this.form.enable();
        });
        this.typeControlValueChanges$.subscribe((type) => {
            // TODO: confirm before closing with uncommitted changes
            this.driver.closeTransaction().subscribe(() => {
                this.driver.selectTransactionType(type);
            });
        });
        this.operationModeControlValueChanges.subscribe((operationMode) => {
            // TODO: confirm before closing with uncommitted changes
            this.driver.closeTransaction().subscribe();
            this.driver.autoTransactionEnabled$.next(operationMode === "auto");
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
