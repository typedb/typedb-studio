/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { CodeEditor } from "@acrodata/code-editor";
import { AsyncPipe, DatePipe } from "@angular/common";
import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from "@angular/core";
import { FormControl, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDialog } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Router, RouterLink } from "@angular/router";
import { Prec } from "@codemirror/state";
import { ResizableDirective } from "@hhangular/resizable";
import { map, startWith } from "rxjs";
import { CodeEditorComponent } from "../../framework/code-editor/code-editor.component";
import { otherExampleLinter, TypeQL, typeqlAutocompleteExtension } from "../../framework/codemirror-lang-typeql";
import { DriverAction, QueryRunAction, TransactionOperationAction, isQueryRun, isTransactionOperation } from "../../concept/action";
import { basicDark } from "../../framework/code-editor/theme";
import { SpinnerComponent } from "../../framework/spinner/spinner.component";
import { ActionDurationPipe } from "../../framework/util/action-duration.pipe";
import { RichTooltipDirective } from "../../framework/tooltip/rich-tooltip.directive";
import { AppData } from "../../service/app-data.service";
import { ChatState } from "../../service/chat-state.service";
import { DriverState } from "../../service/driver-state.service";
import { QueryPageState } from "../../service/query-page-state.service";
import { QueryTab, QueryTabsState } from "../../service/query-tabs-state.service";
import { SnackbarService } from "../../service/snackbar.service";
import { ErrorDetailsDialogComponent } from "../../framework/error-details-dialog/error-details-dialog.component";
import { DatabaseSelectDialogComponent } from "../database/select-dialog/database-select-dialog.component";
import { RenameTabDialogComponent, RenameTabDialogData } from "./rename-tab-dialog/rename-tab-dialog.component";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { keymap } from "@codemirror/view";
import { startCompletion } from "@codemirror/autocomplete";
import { indentWithTab } from "@codemirror/commands";
import { MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { MatSelectModule } from "@angular/material/select";
import { SchemaToolWindowComponent } from "../schema/tool-window/schema-tool-window.component";

@Component({
    selector: "ts-query-page",
    templateUrl: "query-page.component.html",
    styleUrls: ["query-page.component.scss"],
    imports: [
        RouterLink, AsyncPipe, PageScaffoldComponent, MatDividerModule, MatFormFieldModule, MatIconModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule, ResizableDirective,
        DatePipe, SpinnerComponent, MatTableModule, MatSortModule, MatTabsModule, MatTooltipModule, MatButtonModule, RichTooltipDirective,
        MatMenuModule, MatSelectModule, SchemaToolWindowComponent, CodeEditorComponent, ActionDurationPipe,
    ]
})
export class QueryPageComponent implements OnInit, AfterViewInit, OnDestroy {

    @ViewChild(CodeEditor) codeEditor!: CodeEditor;
    @ViewChild("articleRef") articleRef!: ElementRef<HTMLElement>;
    @ViewChild("logTextarea") logTextarea?: ElementRef<HTMLTextAreaElement>;
    @ViewChildren("graphViewRef") graphViewRef!: QueryList<ElementRef<HTMLElement>>;
    @ViewChildren(ResizableDirective) resizables!: QueryList<ResizableDirective>;
    @ViewChild("queryTabContextMenuTrigger") queryTabContextMenuTrigger!: MatMenuTrigger;
    @ViewChild("tabsScrollContainer") tabsScrollContainer?: ElementRef<HTMLElement>;
    @ViewChild("runTabsScrollContainer") runTabsScrollContainer?: ElementRef<HTMLElement>;

    state = inject(QueryPageState);
    driver = inject(DriverState);
    queryTabsState = inject(QueryTabsState);
    private appData = inject(AppData);
    private chatState = inject(ChatState);
    private snackbar = inject(SnackbarService);
    private dialog = inject(MatDialog);
    private router = inject(Router);

    // Query tab context menu state
    queryTabContextMenuPosition = { x: 0, y: 0 };
    queryTabContextMenuTab: QueryTab | null = null;
    queryTabContextMenuTabIndex = 0;

    readonly codeEditorTheme = basicDark;
    codeEditorHidden = true;
    private historyEntryControls = new Map<QueryRunAction, FormControl<string>>();
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
    sentLogToAi = false;
    logHasScrollbar = false;
    canScrollLeft = false;
    canScrollRight = false;
    canScrollRunsLeft = false;
    canScrollRunsRight = false;
    private canvasEl?: HTMLElement;
    private previousTabId: string | null = null;
    private logResizeObserver?: ResizeObserver;
    private tabsScrollObserver?: ResizeObserver;
    private runTabsScrollObserver?: ResizeObserver;

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
        ).subscribe((queryList) => {
            if (queryList.length === 0) {
                console.warn("[QueryPage] Graph canvas element not found in DOM. QueryList is empty.");
                return;
            }
            this.canvasEl = queryList.first.nativeElement;
            this.state.setGraphCanvasEl(this.canvasEl);
        });

        this.previousTabId = this.queryTabsState.currentTab?.id ?? null;
        this.queryTabsState.selectedTabIndex$.subscribe(() => {
            this.state.handleTabSwitch(this.previousTabId);
            this.previousTabId = this.queryTabsState.currentTab?.id ?? null;
        });

        if (this.logTextarea) {
            this.logResizeObserver = new ResizeObserver(() => {
                const el = this.logTextarea?.nativeElement;
                if (el) {
                    this.logHasScrollbar = el.scrollHeight > el.clientHeight;
                }
            });
            this.logResizeObserver.observe(this.logTextarea.nativeElement);
        }

        if (this.tabsScrollContainer) {
            this.tabsScrollObserver = new ResizeObserver(() => {
                this.updateTabsScrollState();
            });
            this.tabsScrollObserver.observe(this.tabsScrollContainer.nativeElement);
        }

        if (this.runTabsScrollContainer) {
            this.runTabsScrollObserver = new ResizeObserver(() => {
                this.updateRunTabsScrollState();
            });
            this.runTabsScrollObserver.observe(this.runTabsScrollContainer.nativeElement);
        }
    }

    ngOnDestroy() {
        this.state.destroyAllGraphOutputs();
        this.logResizeObserver?.disconnect();
        this.tabsScrollObserver?.disconnect();
        this.runTabsScrollObserver?.disconnect();
    }

    // Tab scroll methods
    updateTabsScrollState() {
        const el = this.tabsScrollContainer?.nativeElement;
        if (!el) return;
        this.canScrollLeft = el.scrollLeft > 0;
        this.canScrollRight = el.scrollLeft < el.scrollWidth - el.clientWidth - 1;
    }

    onTabsScroll() {
        this.updateTabsScrollState();
    }

    onTabsWheel(event: WheelEvent) {
        const el = this.tabsScrollContainer?.nativeElement;
        if (!el) return;
        if (event.deltaY !== 0) {
            event.preventDefault();
            el.scrollLeft += event.deltaY;
        }
    }

    scrollTabsLeft() {
        const el = this.tabsScrollContainer?.nativeElement;
        if (!el) return;
        el.scrollBy({ left: -200, behavior: "smooth" });
    }

    scrollTabsRight() {
        const el = this.tabsScrollContainer?.nativeElement;
        if (!el) return;
        el.scrollBy({ left: 200, behavior: "smooth" });
    }

    // Run tab scroll methods
    updateRunTabsScrollState() {
        const el = this.runTabsScrollContainer?.nativeElement;
        if (!el) return;
        this.canScrollRunsLeft = el.scrollLeft > 0;
        this.canScrollRunsRight = el.scrollLeft < el.scrollWidth - el.clientWidth - 1;
    }

    onRunTabsScroll() {
        this.updateRunTabsScrollState();
    }

    onRunTabsWheel(event: WheelEvent) {
        const el = this.runTabsScrollContainer?.nativeElement;
        if (!el) return;
        if (event.deltaY !== 0) {
            event.preventDefault();
            el.scrollLeft += event.deltaY;
        }
    }

    scrollRunTabsLeft() {
        const el = this.runTabsScrollContainer?.nativeElement;
        if (!el) return;
        el.scrollBy({ left: -200, behavior: "smooth" });
    }

    scrollRunTabsRight() {
        const el = this.runTabsScrollContainer?.nativeElement;
        if (!el) return;
        el.scrollBy({ left: 200, behavior: "smooth" });
    }

    openSelectDatabaseDialog() {
        this.dialog.open(DatabaseSelectDialogComponent);
    }

    runQuery() {
        this.state.runCurrentTabQuery();
    }

    // Query tab methods
    onQueryTabChange(index: number) {
        this.queryTabsState.selectTab(index);
    }

    newQueryTab() {
        this.queryTabsState.newTab();
        this.scrollTabsToEnd();
    }

    private scrollTabsToEnd() {
        setTimeout(() => {
            const el = this.tabsScrollContainer?.nativeElement;
            if (!el) return;
            // Scroll to maximum possible position
            el.scrollLeft = el.scrollWidth;
        }, 50);
    }

    closeQueryTab(event: Event, tabIndex: number) {
        event.stopPropagation();
        const tab = this.queryTabsState.openTabs$.value[tabIndex];
        if (tab) {
            this.queryTabsState.closeTab(tab);
        }
    }

    openQueryTabContextMenu(event: MouseEvent, tab: QueryTab, index: number) {
        event.preventDefault();
        event.stopPropagation();
        this.queryTabContextMenuPosition = { x: event.clientX, y: event.clientY };
        this.queryTabContextMenuTab = tab;
        this.queryTabContextMenuTabIndex = index;
        this.queryTabContextMenuTrigger.menuData = { tab, index };
        this.queryTabContextMenuTrigger.openMenu();

        setTimeout(() => {
            const activeElement = document.activeElement as HTMLElement;
            if (activeElement?.classList.contains("mat-mdc-menu-item")) {
                activeElement.blur();
            }
        });
    }

    onQueryTabMouseDown(event: MouseEvent) {
        if (event.button === 1) {
            event.preventDefault();
        }
    }

    onQueryTabMouseUp(event: MouseEvent, index: number) {
        if (event.button === 1) {
            event.preventDefault();
            this.closeQueryTab(event, index);
        }
    }

    closeOtherQueryTabs(tab: QueryTab) {
        this.queryTabsState.closeOtherTabs(tab);
    }

    closeQueryTabsToRight(tab: QueryTab) {
        this.queryTabsState.closeTabsToRight(tab);
    }

    closeAllQueryTabs() {
        this.queryTabsState.closeAllTabs();
    }

    togglePinQueryTab(tab: QueryTab) {
        this.queryTabsState.togglePinTab(tab);
    }

    openRenameTabDialog(tab: QueryTab) {
        const dialogRef = this.dialog.open(RenameTabDialogComponent, {
            data: { currentName: tab.name } as RenameTabDialogData,
            width: "400px",
        });
        dialogRef.afterClosed().subscribe((newName: string | undefined) => {
            if (newName) {
                this.queryTabsState.renameTab(tab, newName);
            }
        });
    }

    duplicateQueryTab(tab: QueryTab) {
        this.queryTabsState.duplicateTab(tab);
    }

    // Run tab methods
    onRunTabChange(index: number) {
        this.state.selectRun(index);
    }

    closeRunTab(event: Event, index: number) {
        event.stopPropagation();
        this.state.closeRun(index);
    }

    getHistoryEntryControl(entry: QueryRunAction): FormControl<string> {
        let control = this.historyEntryControls.get(entry);
        if (!control) {
            control = new FormControl(entry.query, { nonNullable: true });
            this.historyEntryControls.set(entry, control);
        }
        return control;
    }

    runHistoryQuery(entry: QueryRunAction) {
        this.state.runQuery(entry.query);
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

    openErrorDetails(entry: DriverAction) {
        const message = this.historyEntryErrorTooltip(entry);
        if (!message) return;
        this.dialog.open(ErrorDetailsDialogComponent, {
            data: { message },
            width: "600px",
        });
    }

    sendLogToAi(): void {
        const logText = this.state.logOutput.control.value;
        if (!logText) return;

        this.chatState.newConversation();
        if (!this.chatState.selectedConversationId$.value) return;

        const run = this.state.currentTabRuns[this.state.selectedRunIndex];
        if (run?.query) {
            this.chatState.pendingTitle = run.query;
        }
        this.chatState.pendingMessage = logText;
        this.sentLogToAi = true;
        setTimeout(() => this.sentLogToAi = false, 3000);
        this.router.navigate(['/agent-mode']);
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

    readonly isQueryRun = isQueryRun;
    readonly isTransactionOperation = isTransactionOperation;
    readonly JSON = JSON;
    readonly TypeQL = TypeQL;
    readonly linter = otherExampleLinter;
    readonly typeqlAutocompleteExtension = typeqlAutocompleteExtension;
}
