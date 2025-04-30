/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { CodeEditor } from "@acrodata/code-editor";
import { AsyncPipe, DatePipe } from "@angular/common";
import { AfterViewInit, Component, ElementRef, OnInit, QueryList, ViewChild, ViewChildren } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { ResizableDirective } from "@hhangular/resizable";
import { first, map } from "rxjs";
import { ButtonComponent } from "../../framework/button/button.component";
import { basicDark } from "../../framework/code-editor/theme";
import { SpinnerComponent } from "../../framework/spinner/spinner.component";
import { AppData } from "../../service/app-data.service";
import { DriverAction, DriverState, isQueryRun, isTransactionOperation, TransactionOperationAction } from "../../service/driver-state.service";
import { QueryToolState } from "../../service/query-tool-state.service";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";

@Component({
    selector: "ts-query-tool",
    templateUrl: "query-tool.component.html",
    styleUrls: ["query-tool.component.scss"],
    standalone: true,
    imports: [
        ButtonComponent, RouterLink, AsyncPipe, PageScaffoldComponent, MatDividerModule, MatFormFieldModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule, CodeEditor, ResizableDirective,
        DatePipe, SpinnerComponent, MatTableModule, MatSortModule, MatTooltipModule, MatButtonModule,
    ],
})
export class QueryToolComponent implements OnInit, AfterViewInit {

    @ViewChild(CodeEditor) codeEditor!: CodeEditor;
    @ViewChild("articleRef") articleRef!: ElementRef<HTMLElement>;
    @ViewChildren("structureViewRef") structureViewRef!: QueryList<ElementRef<HTMLElement>>;
    @ViewChildren(ResizableDirective) resizables!: QueryList<ResizableDirective>;
    readonly codeEditorTheme = basicDark;
    codeEditorHidden = true;

    constructor(protected state: QueryToolState, public driver: DriverState, private appData: AppData) {
    }

    ngOnInit() {
        this.appData.viewState.setLastUsedTool("query");
        this.renderCodeEditorWithDelay();
    }

    private renderCodeEditorWithDelay() {
        // omitting this causes the left sidebar to flicker
        setTimeout(() => {
            this.codeEditorHidden = false;
        }, 0);
    }

    ngAfterViewInit() {
        const articleWidth = this.articleRef.nativeElement.clientWidth;
        this.resizables.first.percent = (articleWidth * 0.15 + 100) / articleWidth * 100;
        this.structureViewRef.changes.pipe(
            map(x => x as QueryList<ElementRef<HTMLElement>>),
            first(queryList => queryList.length > 0)
        ).subscribe((queryList) => {
            this.state.structureOutput.canvasEl = queryList.first.nativeElement;
        });
    }

    runQuery() {
        this.state.runQuery();
    }

    queryHistoryPreview(query: string) {
        return query.split(`\n`).slice(0, 2).join(`\n`);
    }

    // TODO: any angular dev with a shred of self-respect would make this be a pipe
    actionDurationString(action: DriverAction) {
        const startedAtTimestamp = isQueryRun(action) ? action.queryRun.startedAtTimestamp : action.transactionOperation.startedAtTimestamp;
        const completedAtTimestamp = isQueryRun(action) ? action.queryRun.completedAtTimestamp : action.transactionOperation.completedAtTimestamp;
        if (completedAtTimestamp == undefined) return ``;
        return `${completedAtTimestamp - startedAtTimestamp}ms`;
    }

    transactionOperationString(action: TransactionOperationAction) {
        switch (action.transactionOperation.operationType) {
            case "open": return "opened transaction";
            case "commit": return "committed transaction";
            case "close": return "closed transaction";
        }
    }

    isPending(historyEntry: DriverAction) {
        return isQueryRun(historyEntry) ? historyEntry.queryRun.status === "pending" : historyEntry.transactionOperation.status === "pending";
    }

    readonly isQueryRun = isQueryRun;
    readonly isTransactionOperation = isTransactionOperation;
    readonly JSON = JSON;
}
