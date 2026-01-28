/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { CodeEditor } from "@acrodata/code-editor";
import { AsyncPipe, DatePipe } from "@angular/common";
import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDialog } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { Prec } from "@codemirror/state";
import { ResizableDirective } from "@hhangular/resizable";
import { filter, map, startWith } from "rxjs";
import { CodeEditorComponent } from "../../framework/code-editor/code-editor.component";
import { otherExampleLinter, TypeQL, typeqlAutocompleteExtension } from "../../framework/codemirror-lang-typeql";
import { DriverAction, TransactionOperationAction, isQueryRun, isTransactionOperation } from "../../concept/action";
import { basicDark } from "../../framework/code-editor/theme";
import { SpinnerComponent } from "../../framework/spinner/spinner.component";
import { RichTooltipDirective } from "../../framework/tooltip/rich-tooltip.directive";
import { AppData } from "../../service/app-data.service";
import { DriverState } from "../../service/driver-state.service";
import { QueryPageState, QueryType } from "../../service/query-page-state.service";
import { SnackbarService } from "../../service/snackbar.service";
import { VibeQueryComponent } from "../ai/vibe-query.component";
import { DatabaseSelectDialogComponent } from "../database/select-dialog/database-select-dialog.component";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { keymap } from "@codemirror/view";
import { startCompletion } from "@codemirror/autocomplete";
import { indentWithTab } from "@codemirror/commands";
import { MatMenuModule } from "@angular/material/menu";
import { SchemaToolWindowComponent } from "../schema/tool-window/schema-tool-window.component";

@Component({
    selector: "ts-query-page",
    templateUrl: "query-page.component.html",
    styleUrls: ["query-page.component.scss"],
    imports: [
        RouterLink, AsyncPipe, PageScaffoldComponent, MatDividerModule, MatFormFieldModule, MatIconModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule, ResizableDirective,
        DatePipe, SpinnerComponent, MatTableModule, MatSortModule, MatTooltipModule, MatButtonModule, RichTooltipDirective,
        MatMenuModule, SchemaToolWindowComponent, VibeQueryComponent, CodeEditorComponent,
    ]
})
export class QueryPageComponent implements OnInit, AfterViewInit, OnDestroy {

    @ViewChild(CodeEditor) codeEditor!: CodeEditor;
    @ViewChild("articleRef") articleRef!: ElementRef<HTMLElement>;
    @ViewChildren("graphViewRef") graphViewRef!: QueryList<ElementRef<HTMLElement>>;
    @ViewChildren(ResizableDirective) resizables!: QueryList<ResizableDirective>;

    state = inject(QueryPageState);
    driver = inject(DriverState);
    private appData = inject(AppData);
    private snackbar = inject(SnackbarService);
    private dialog = inject(MatDialog);

    readonly codeEditorTheme = basicDark;
    codeEditorHidden = true;
    editorKeymap = Prec.highest(keymap.of([
        { key: "Alt-Space", run: startCompletion, preventDefault: true },
        {
            key: "Mod-Enter",
            run: () => {
                this.runQuery();
                return true;
            },
        },
        indentWithTab,
    ]));
    copiedLog = false;
    sentLogToAI = false;

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
        this.graphViewRef.changes.pipe(
            map(x => x as QueryList<ElementRef<HTMLElement>>),
            startWith(this.graphViewRef),
            filter(queryList => queryList.length > 0),
            map(x => x.first.nativeElement),
        ).subscribe((canvasEl) => {
            this.state.graphOutput.canvasEl = canvasEl;
        });
    }

    ngOnDestroy() {
        this.state.graphOutput.destroy();
    }

    openSelectDatabaseDialog() {
        this.dialog.open(DatabaseSelectDialogComponent);
    }

    runQuery() {
        this.state.runQuery(this.state.queryEditorControl.value);
    }

    queryHistoryPreview(query: string) {
        return query.split(`\n`).slice(0, 2).join(`\n`);
    }

    // TODO: any angular dev with a shred of self-respect would make this be a pipe
    actionDurationString(action: DriverAction) {
        if (action.completedAtTimestamp == undefined) return ``;
        return `${action.completedAtTimestamp - action.startedAtTimestamp}ms`;
    }

    transactionOperationString(action: TransactionOperationAction) {
        switch (action.operation) {
            case "open": return "opened transaction";
            case "commit": return action.status === "error" ? "commit failed" : "committed";
            case "close": return "closed transaction";
        }
    }

    historyEntryErrorTooltip(entry: DriverAction) {
        if (!entry.result) return ``;
        else if ("err" in entry.result && !!entry.result.err?.message) return entry.result.err.message;
        else if ("message" in entry.result) return entry.result.message as string;
        else return entry.result.toString();
    }

    async copyHistoryEntryErrorTooltip(entry: DriverAction) {
        const tooltip = this.historyEntryErrorTooltip(entry);
        if (!tooltip) return;
        try {
            await navigator.clipboard.writeText(tooltip);
            this.snackbar.success("Error text copied", { duration: 2500 });
        } catch (e) {
            console.warn(e);
        }
    }

    queryTypeIconClass(queryType: QueryType): string {
        switch (queryType) {
            case "code": return "fa-light fa-code";
            case "chat": return "fa-light fa-wand-magic-sparkles";
            default: return "";
        }
    }

    clearChat() {
        this.state.clearChat();
    }

    async copyLog() {
        try {
            await navigator.clipboard.writeText(this.state.logOutput.control.value);
            this.copiedLog = true;

            // Reset copied state after 3 seconds
            setTimeout(() => {
                this.copiedLog = false;
            }, 3000);
        } catch (err) {
            console.error('Failed to copy results log:', err);
        }
    }

    sendLogToAI() {
        this.sentLogToAI = true;
        this.state.vibeQuery.promptControl.patchValue(this.state.logOutput.control.value);
        setTimeout(() => {
            this.state.vibeQuery.submitPrompt();
        });
        setTimeout(() => {
            this.sentLogToAI = false;
        }, 3000);
    }

    readonly isQueryRun = isQueryRun;
    readonly isTransactionOperation = isTransactionOperation;
    readonly JSON = JSON;
    readonly TypeQL = TypeQL;
    readonly linter = otherExampleLinter;
    readonly typeqlAutocompleteExtension = typeqlAutocompleteExtension;
}
