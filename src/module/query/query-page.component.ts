/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { CodeEditor } from "@acrodata/code-editor";
import { AsyncPipe } from "@angular/common";
import { AfterViewChecked, AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from "@angular/core";
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
import { MatTabsModule } from "@angular/material/tabs";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Router, RouterLink } from "@angular/router";
import { Prec } from "@codemirror/state";
import { ResizableDirective } from "@hhangular/resizable";
import { map, skip, startWith } from "rxjs";
import { CodeEditorComponent } from "../../framework/code-editor/code-editor.component";
import { otherExampleLinter, TypeQL, typeqlAutocompleteExtension } from "../../framework/codemirror-lang-typeql";
import { basicDark } from "../../framework/code-editor/theme";
import { SpinnerComponent } from "../../framework/spinner/spinner.component";
import { AppData } from "../../service/app-data.service";
import { ChatState } from "../../service/chat-state.service";
import { DriverState } from "../../service/driver-state.service";
import { QueryPageState } from "../../service/query-page-state.service";
import { QueryTab, QueryTabsState } from "../../service/query-tabs-state.service";
import { RunOutputState } from "../../service/query-page-state.service";
import { QueryExportService, SerializedOutput } from "../../service/query-export.service";
import { SnackbarService } from "../../service/snackbar.service";
import { DatabaseCreateDialogComponent } from "../database/create-dialog/database-create-dialog.component";
import { DatabaseSelectDialogComponent } from "../database/select-dialog/database-select-dialog.component";
import { RenameTabDialogComponent, RenameTabDialogData } from "./rename-tab-dialog/rename-tab-dialog.component";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { keymap } from "@codemirror/view";
import { startCompletion } from "@codemirror/autocomplete";
import { indentWithTab } from "@codemirror/commands";
import { MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { MatSelectModule } from "@angular/material/select";
import { SchemaToolWindowComponent } from "../schema/tool-window/schema-tool-window.component";
import { GraphCanvasComponent } from "../../framework/graph-visualiser/canvas/graph-canvas.component";
import { HistoryPaneComponent } from "../query-history/history-pane/history-pane.component";

@Component({
    selector: "ts-query-page",
    templateUrl: "query-page.component.html",
    styleUrls: ["query-page.component.scss"],
    imports: [
        RouterLink, AsyncPipe, PageScaffoldComponent, MatDividerModule, MatFormFieldModule, MatIconModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule, ResizableDirective,
        SpinnerComponent, MatTableModule, MatSortModule, MatTabsModule, MatTooltipModule, MatButtonModule,
        MatMenuModule, MatSelectModule, SchemaToolWindowComponent, CodeEditorComponent,
        GraphCanvasComponent, HistoryPaneComponent,
    ]
})
export class QueryPageComponent implements OnInit, AfterViewInit, AfterViewChecked, OnDestroy {

    @ViewChild(CodeEditor) codeEditor!: CodeEditor;
    @ViewChild("articleRef") articleRef!: ElementRef<HTMLElement>;
    @ViewChild("logTextarea") logTextarea?: ElementRef<HTMLTextAreaElement>;
    @ViewChildren(GraphCanvasComponent) graphCanvasComponents!: QueryList<GraphCanvasComponent>;
    @ViewChildren(ResizableDirective) resizables!: QueryList<ResizableDirective>;
    @ViewChild("queryTabContextMenuTrigger") queryTabContextMenuTrigger!: MatMenuTrigger;
    @ViewChild("runTabContextMenuTrigger") runTabContextMenuTrigger!: MatMenuTrigger;
    @ViewChild("tabsScrollContainer") tabsScrollContainer?: ElementRef<HTMLElement>;
    @ViewChild("runTabsScrollContainer") runTabsScrollContainer?: ElementRef<HTMLElement>;

    state = inject(QueryPageState);
    driver = inject(DriverState);
    queryTabsState = inject(QueryTabsState);
    exportService = inject(QueryExportService);
    private appData = inject(AppData);
    private chatState = inject(ChatState);
    private snackbar = inject(SnackbarService);
    private dialog = inject(MatDialog);
    private router = inject(Router);

    // Query tab context menu state
    queryTabContextMenuPosition = { x: 0, y: 0 };
    queryTabContextMenuTab: QueryTab | null = null;
    queryTabContextMenuTabIndex = 0;

    // Run tab context menu state
    runTabContextMenuPosition = { x: 0, y: 0 };
    runTabContextMenuRun: RunOutputState | null = null;
    runTabContextMenuTabIndex = 0;

    graphMaximised = false;

    get currentRun(): RunOutputState | null {
        const runs = this.state.currentTabRuns;
        const idx = this.state.selectedRunIndex;
        return idx >= 0 && idx < runs.length ? runs[idx] : null;
    }

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
    private static readonly DEFAULT_PANEL_SIZES = [20, 60, 20, 50, 50, 75, 25];
    panelSizes = [...QueryPageComponent.DEFAULT_PANEL_SIZES];

    sentLogToAi = false;
    logHasScrollbar = false;
    /** Tracks the log textarea's scrollHeight across change-detection passes, so we know when content has grown. */
    private lastLogScrollHeight = 0;
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
        const saved = this.appData.panelLayout.get("query");
        if (saved && saved.length === QueryPageComponent.DEFAULT_PANEL_SIZES.length) {
            this.panelSizes = saved;
        }
        this.renderCodeEditorWithDelay();
    }

    private renderCodeEditorWithDelay() {
        // omitting this causes the left sidebar to flicker
        setTimeout(() => {
            this.codeEditorHidden = false;
        }, 0);
    }

    ngAfterViewInit() {
        this.graphCanvasComponents.changes.pipe(
            map(x => x as QueryList<GraphCanvasComponent>),
            startWith(this.graphCanvasComponents),
        ).subscribe((queryList) => {
            if (queryList.length === 0 || !queryList.first.canvasEl) {
                this.state.setGraphCanvasEl(null);
                return;
            }
            this.canvasEl = queryList.first.canvasEl;
            this.state.setGraphCanvasEl(this.canvasEl);
        });

        this.previousTabId = this.queryTabsState.currentTab?.id ?? null;
        this.queryTabsState.selectedTabIndex$.pipe(skip(1)).subscribe(() => {
            this.state.handleTabSwitch(this.previousTabId);
            this.previousTabId = this.queryTabsState.currentTab?.id ?? null;
            this.scrollActiveTabIntoView();
        });

        this.state.outputTypeControl.valueChanges.subscribe((value) => {
            if (value === "graph") {
                requestAnimationFrame(() => this.state.graphOutput.resize());
            }
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
            this.scrollActiveTabIntoView();
        }

        if (this.runTabsScrollContainer) {
            this.runTabsScrollObserver = new ResizeObserver(() => {
                this.updateRunTabsScrollState();
            });
            this.runTabsScrollObserver.observe(this.runTabsScrollContainer.nativeElement);
        }
    }

    ngOnDestroy() {
        this.state.detachAllGraphOutputs();
        this.logResizeObserver?.disconnect();
        this.tabsScrollObserver?.disconnect();
        this.runTabsScrollObserver?.disconnect();
    }

    ngAfterViewChecked() {
        const el = this.logTextarea?.nativeElement;
        if (!el) return;
        if (el.scrollHeight === this.lastLogScrollHeight) return;
        this.lastLogScrollHeight = el.scrollHeight;
        if (this.state.logOutput.autoscrollEnabled) {
            el.scrollTop = el.scrollHeight;
        }
    }

    onLogScroll() {
        // If the user (or any scroll) ends up not at the bottom, we stop auto-following new content.
        // Programmatic scrolls in ngAfterViewChecked always land at the bottom, so they pass this check harmlessly.
        const el = this.logTextarea?.nativeElement;
        if (!el) return;
        const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 4;
        this.state.logOutput.autoscrollEnabled = atBottom;
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

    private scrollActiveTabIntoView() {
        setTimeout(() => {
            const container = this.tabsScrollContainer?.nativeElement;
            if (!container) return;
            const activeTab = container.querySelector('.query-tab.active') as HTMLElement;
            if (!activeTab) return;
            activeTab.scrollIntoView({ inline: "nearest", block: "nearest" });
            this.updateTabsScrollState();
        });
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

    openCreateDatabaseDialog() {
        this.dialog.open(DatabaseCreateDialogComponent);
    }

    onGraphStatusAction(action: string) {
        if (action === "viewLog") this.state.outputTypeControl.patchValue("log");
    }

    runQuery() {
        this.state.runCurrentTabQuery();
    }

    stopQuery() {
        this.state.stopQuery();
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
        if (this.queryTabContextMenuTrigger.menuOpen) return;
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
                this.scrollActiveTabIntoView();
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

    openRunTabContextMenu(event: MouseEvent, run: RunOutputState, index: number) {
        event.preventDefault();
        event.stopPropagation();
        if (this.runTabContextMenuTrigger.menuOpen) return;
        this.runTabContextMenuPosition = { x: event.clientX, y: event.clientY };
        this.runTabContextMenuRun = run;
        this.runTabContextMenuTabIndex = index;
        this.runTabContextMenuTrigger.menuData = { run, index };
        this.runTabContextMenuTrigger.openMenu();

        setTimeout(() => {
            const activeElement = document.activeElement as HTMLElement;
            if (activeElement?.classList.contains("mat-mdc-menu-item")) {
                activeElement.blur();
            }
        });
    }

    openRenameRunDialog(run: RunOutputState) {
        const dialogRef = this.dialog.open(RenameTabDialogComponent, {
            data: { currentName: run.label } as RenameTabDialogData,
            width: "400px",
        });
        dialogRef.afterClosed().subscribe((newName: string | undefined) => {
            if (newName) {
                this.state.renameRun(run, newName);
            }
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

    canExportResults(): boolean {
        return this.exportService.canExportResults(this.currentRun ?? undefined);
    }

    exportResultsDisabledReason(): string | null {
        const run = this.currentRun;
        if (run?.multiQuery || run?.batchSummary) {
            return "Export is not available for query batches";
        }
        return null;
    }

    isExportResultsVisible(): boolean {
        return this.canExportResults() || !!this.exportResultsDisabledReason();
    }

    exportResultsTooltip(): string {
        return this.exportResultsDisabledReason() ?? "Export";
    }

    canExportLog(): boolean {
        return !!this.state.logOutput.control.value.length;
    }

    canExportRaw(): boolean {
        return !!this.state.rawOutput.control.value.length;
    }

    private async runCopy(payload: SerializedOutput | null, label: string) {
        if (!payload) {
            this.snackbar.warn("Nothing to copy");
            return;
        }
        try {
            await this.exportService.copy(payload);
            this.snackbar.success(`Copied ${label} to clipboard`);
        } catch (err) {
            console.error("Copy failed:", err);
            this.snackbar.warn(`Failed to copy ${label}`);
        }
    }

    private runDownload(run: RunOutputState | null, suffix: string, payload: SerializedOutput | null) {
        if (!run || !payload) {
            this.snackbar.warn("Nothing to export");
            return;
        }
        this.exportService.download(run, suffix, payload);
    }

    copyLog() {
        const run = this.currentRun;
        if (!run) return;
        void this.runCopy(this.exportService.serializeLog(run), "log");
    }

    copyResultsJson() {
        const run = this.currentRun;
        if (!run) return;
        void this.runCopy(this.exportService.serializeResults(run, "json"), "results");
    }

    copyResultsCsv() {
        const run = this.currentRun;
        if (!run) return;
        void this.runCopy(this.exportService.serializeResults(run, "csv"), "results");
    }

    downloadResultsJson() {
        const run = this.currentRun;
        this.runDownload(run, "results", run ? this.exportService.serializeResults(run, "json") : null);
    }

    downloadResultsCsv() {
        const run = this.currentRun;
        this.runDownload(run, "results", run ? this.exportService.serializeResults(run, "csv") : null);
    }

    copyRaw() {
        const run = this.currentRun;
        if (!run) return;
        void this.runCopy(this.exportService.serializeRaw(run), "raw");
    }

    downloadRaw() {
        const run = this.currentRun;
        this.runDownload(run, "raw", run ? this.exportService.serializeRaw(run) : null);
    }

    onPanelResize(index: number, percent: number) {
        this.panelSizes[index] = percent;
        this.appData.panelLayout.set("query", [...this.panelSizes]);
    }

    readonly JSON = JSON;
    readonly TypeQL = TypeQL;
    readonly linter = otherExampleLinter;
    readonly typeqlAutocompleteExtension = typeqlAutocompleteExtension;
}
