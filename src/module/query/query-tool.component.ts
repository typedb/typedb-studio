/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { CodeEditor } from "@acrodata/code-editor";
import { AsyncPipe, DatePipe } from "@angular/common";
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from "@angular/core";
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
import { MatTree, MatTreeModule } from "@angular/material/tree";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { Prec } from "@codemirror/state";
import { ResizableDirective } from "@hhangular/resizable";
import { filter, map, startWith } from "rxjs";
import { otherExampleLinter, TypeQL, typeqlAutocompleteExtension } from "../../framework/codemirror-lang-typeql";
import { DriverAction, TransactionOperationAction, isQueryRun, isTransactionOperation } from "../../concept/action";
import { basicDark } from "../../framework/code-editor/theme";
import { DetectScrollDirective } from "../../framework/scroll-container/detect-scroll.directive";
import { SpinnerComponent } from "../../framework/spinner/spinner.component";
import { RichTooltipDirective } from "../../framework/tooltip/rich-tooltip.directive";
import { AppData } from "../../service/app-data.service";
import { DriverState } from "../../service/driver-state.service";
import { QueryToolState } from "../../service/query-tool-state.service";
import { SnackbarService } from "../../service/snackbar.service";
import { DatabaseSelectDialogComponent } from "../database/select-dialog/database-select-dialog.component";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { SchemaTreeNodeComponent } from "./schema-tree-node/schema-tree-node.component";
import { keymap } from "@codemirror/view";
import {startCompletion} from "@codemirror/autocomplete";
import { MatMenuModule } from "@angular/material/menu";
import { SchemaToolWindowState, SchemaTreeNode } from "../../service/schema-tool-window-state.service";

@Component({
    selector: "ts-query-tool",
    templateUrl: "query-tool.component.html",
    styleUrls: ["query-tool.component.scss"],
    imports: [
        RouterLink, AsyncPipe, PageScaffoldComponent, MatDividerModule, MatFormFieldModule, MatTreeModule, MatIconModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule, CodeEditor, ResizableDirective,
        DatePipe, SpinnerComponent, MatTableModule, MatSortModule, MatTooltipModule, MatButtonModule, RichTooltipDirective,
        SchemaTreeNodeComponent, DetectScrollDirective, MatMenuModule
    ]
})
export class QueryToolComponent implements OnInit, AfterViewInit, OnDestroy {

    @ViewChild(CodeEditor) codeEditor!: CodeEditor;
    @ViewChild("articleRef") articleRef!: ElementRef<HTMLElement>;
    @ViewChildren("graphViewRef") graphViewRef!: QueryList<ElementRef<HTMLElement>>;
    @ViewChildren(ResizableDirective) resizables!: QueryList<ResizableDirective>;
    @ViewChild("tree") tree!: MatTree<any>;
    readonly codeEditorTheme = basicDark;
    codeEditorHidden = true;
    refreshSchemaTooltip$ = this.state.schema.refreshDisabledReason$.pipe(map(x => x ? `` : `Refresh`));
    editorKeymap = Prec.highest(keymap.of([
        { key: "Alt-Space", run: startCompletion, preventDefault: true },
        {
            key: "Mod-Enter",
            run: () => {
                this.runQuery();
                return true;
            },
        },
    ]));

    constructor(
        public state: QueryToolState, public schemaWindow: SchemaToolWindowState, public driver: DriverState,
        private appData: AppData, private snackbar: SnackbarService, private dialog: MatDialog,
    ) {
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
        this.resizables.last.percent = (articleWidth * 0.15 + 100) / articleWidth * 100;
        this.graphViewRef.changes.pipe(
            map(x => x as QueryList<ElementRef<HTMLElement>>),
            startWith(this.graphViewRef),
            filter(queryList => queryList.length > 0),
            map(x => x.first.nativeElement),
        ).subscribe((canvasEl) => {
            this.state.graphOutput.canvasEl = canvasEl;
        });
        this.schemaWindow.dataSource$.subscribe((dataSource) => {
            dataSource.forEach( node => {
                if (this.schemaWindow.rootNodesCollapsed[node.label]) {
                    this.tree.collapse(node);
                } else {
                    this.tree.expand(node);
                }
            });
        });
    }

    ngOnDestroy() {
        this.state.graphOutput.destroy();
    }

    openSelectDatabaseDialog() {
        this.dialog.open(DatabaseSelectDialogComponent);
    }

    runQuery() {
        this.state.runQuery();
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
            case "commit": return "committed transaction";
            case "close": return "closed transaction";
        }
    }

    treeNodeClass(node: SchemaTreeNode): string {
        switch (node.nodeKind) {
            case "root":
                return `root ${node.label.toLowerCase()}`;
            case "concept":
                return "concept";
            case "link":
                return `link ${node.linkKind} ${node.linkKind === "sub" ? node.supertype.kind : ""}`;
        }
    }

    toggleNode(node: SchemaTreeNode) {
        if (this.tree.isExpanded(node)) {
            this.tree.collapse(node);
        } else {
            this.tree.expand(node);
        }

        if (node.nodeKind === "root") {
            this.schemaWindow.rootNodesCollapsed[node.label] = !this.tree.isExpanded(node);
            const schemaToolWindowState = this.appData.viewState.schemaToolWindowState();
            schemaToolWindowState.rootNodesCollapsed = this.schemaWindow.rootNodesCollapsed;
            this.appData.viewState.setSchemaToolWindowState(schemaToolWindowState);
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

    readonly isQueryRun = isQueryRun;
    readonly isTransactionOperation = isTransactionOperation;
    readonly JSON = JSON;
    readonly TypeQL = TypeQL;
    readonly linter = otherExampleLinter;
    readonly typeqlAutocompleteExtension = typeqlAutocompleteExtension;
}
