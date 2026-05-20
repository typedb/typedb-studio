/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { DatePipe } from "@angular/common";
import { FormControl } from "@angular/forms";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatDialog } from "@angular/material/dialog";
import { DriverAction, QueryRunAction, TransactionOperationAction, isQueryRun, isTransactionOperation } from "../../../concept/action";
import { CodeEditorComponent } from "../../../framework/code-editor/code-editor.component";
import { SpinnerComponent } from "../../../framework/spinner/spinner.component";
import { ActionDurationPipe } from "../../../framework/util/action-duration.pipe";
import { ErrorDetailsDialogComponent } from "../../../framework/error-details-dialog/error-details-dialog.component";
import { HistoryWindowState } from "../../../service/query-page-state.service";

@Component({
    selector: "ts-history-pane",
    templateUrl: "./history-pane.component.html",
    styleUrls: ["./history-pane.component.scss"],
    imports: [DatePipe, MatTooltipModule, CodeEditorComponent, SpinnerComponent, ActionDurationPipe],
})
export class HistoryPaneComponent {
    @Input({ required: true }) history!: HistoryWindowState;
    @Output() runHistoryQuery = new EventEmitter<QueryRunAction>();

    private historyEntryControls = new Map<QueryRunAction, FormControl<string>>();
    private truncationState = new WeakMap<HTMLElement, boolean>();

    readonly isQueryRun = isQueryRun;
    readonly isTransactionOperation = isTransactionOperation;

    constructor(private dialog: MatDialog) {}

    getHistoryEntryControl(entry: QueryRunAction): FormControl<string> {
        let control = this.historyEntryControls.get(entry);
        if (!control) {
            control = new FormControl(entry.query, { nonNullable: true });
            this.historyEntryControls.set(entry, control);
        }
        return control;
    }

    onRunHistoryQuery(entry: QueryRunAction) {
        this.runHistoryQuery.emit(entry);
    }

    transactionOperationString(action: TransactionOperationAction) {
        switch (action.operation) {
            case "open": return "opened transaction";
            case "commit": return action.status === "error" ? "commit failed" : "committed";
            case "close": return "closed transaction";
        }
    }

    queryRunLabel(entry: QueryRunAction): string {
        const type = entry.batch ? "query batch" : "query";
        if (entry.status === "error") {
            if (entry.batch && (entry.result as any)?.message === "Query batch interrupted") return "query batch interrupted";
            return `${type} failed`;
        }
        return entry.autoCommitted ? `ran + committed ${type}` : `ran ${type}`;
    }

    historyEntryErrorTooltip(entry: DriverAction) {
        if (!entry.result) return ``;
        else if ("err" in entry.result && !!entry.result.err?.message) return entry.result.err.message;
        else if ("message" in entry.result) return entry.result.message as string;
        else return entry.result.toString();
    }

    openErrorDetails(entry: DriverAction) {
        const message = this.historyEntryErrorTooltip(entry);
        if (!message) return;
        this.dialog.open(ErrorDetailsDialogComponent, {
            data: { message },
            width: "600px",
        });
    }

    checkTruncation(el: HTMLElement) {
        this.truncationState.set(el, el.scrollWidth > el.clientWidth);
    }

    isTruncated(el: HTMLElement): boolean {
        return this.truncationState.get(el) ?? false;
    }
}
