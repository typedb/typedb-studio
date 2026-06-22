/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, HostBinding, HostListener, Input, ViewChild } from "@angular/core";
import { ScrollingModule } from "@angular/cdk/scrolling";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Router, RouterLink } from "@angular/router";
import { combineLatest, map } from "rxjs";
import { MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { FlatSchemaTreeNode, SchemaToolWindowState, SchemaTreeNode } from "../../../service/schema-tool-window-state.service";
import { DriverState } from "../../../service/driver-state.service";
import { MatDialog } from "@angular/material/dialog";
import { SchemaTextDialogComponent } from "../text-dialog/schema-text-dialog.component";
import { SampleDatasetDialogComponent } from "../../database/sample-dataset-dialog/sample-dataset-dialog.component";
import { SchemaConcept } from "../../../service/schema-state.service";
import { DataEditorState } from "../../../service/data-editor-state.service";
import { QueryTabsState } from "../../../service/query-tabs-state.service";
import { QueryPageState } from "../../../service/query-page-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { GraphViewState, kindInstancesQuery, RootKind } from "../../../service/graph-view-state.service";

@Component({
    selector: "ts-schema-tool-window",
    templateUrl: "schema-tool-window.component.html",
    styleUrls: ["schema-tool-window.component.scss"],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        AsyncPipe, RouterLink, ScrollingModule, MatDividerModule, MatFormFieldModule, MatIconModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule,
        MatTableModule, MatSortModule, MatTooltipModule, MatButtonModule, MatMenuModule,
    ]
})
export class SchemaToolWindowComponent {

    @Input() title = "Schema";
    @Input() mode: "schema" | "data" | "graph-view" = "schema";
    @HostBinding("class") readonly clazz = "schema-pane";
    @ViewChild("conceptContextMenuTrigger") conceptContextMenuTrigger!: MatMenuTrigger;
    @ViewChild("rootContextMenuTrigger") rootContextMenuTrigger!: MatMenuTrigger;
    @ViewChild("searchInput") searchInputRef?: ElementRef<HTMLInputElement>;
    refreshSchemaTooltip$ = this.state.schema.interactionDisabledReason$.pipe(map(x => x ? `` : `Refresh`));
    actionsButtonTooltip$ = this.state.schema.interactionDisabledReason$.pipe(map(x => x ? x : `More actions`));

    // Concept context menu state
    conceptContextMenuPosition = { x: 0, y: 0 };
    conceptContextMenuConcept: SchemaConcept | null = null;

    // Root context menu state
    rootContextMenuPosition = { x: 0, y: 0 };

    /** Whether the search overlay is mounted (and the input is focused). The
     *  query itself lives on the state service so the tree filter reacts
     *  reactively. */
    searchOpen = false;

    constructor(
        public state: SchemaToolWindowState,
        public driver: DriverState,
        public router: Router,
        private cdr: ChangeDetectorRef,
        private dialog: MatDialog,
        private dataEditorState: DataEditorState,
        private queryTabsState: QueryTabsState,
        private queryPageState: QueryPageState,
        private snackbar: SnackbarService,
        private graphViewState: GraphViewState,
        private elementRef: ElementRef<HTMLElement>,
    ) {
        // Subscribe to active tab changes to highlight the corresponding type in the schema tree.
        combineLatest([
            this.dataEditorState.openTabs$,
            this.dataEditorState.selectedTabIndex$
        ]).subscribe(([tabs, selectedIndex]) => {
            const activeTab = tabs[selectedIndex];
            if (activeTab) {
                this.state.highlightedConceptLabel$.next(activeTab.type.label);
            } else {
                this.state.highlightedConceptLabel$.next(null);
            }
        });
        // Highlight changes need to mark this OnPush component for check.
        this.state.highlightedConceptLabel$.subscribe(() => this.cdr.markForCheck());
    }

    trackByFlatNode(_index: number, flat: FlatSchemaTreeNode): string {
        return flat.node.key;
    }

    onExpandClick(flat: FlatSchemaTreeNode, event: Event) {
        event.stopPropagation();
        if (!flat.expandable) return;
        this.state.toggleExpand(flat.node);
    }

    onRowClick(flat: FlatSchemaTreeNode, event: Event) {
        const node = flat.node;
        if (this.mode === "data") {
            if (node.nodeKind === "concept") {
                this.dataEditorState.openTypeTab(node.concept);
                event.stopPropagation();
            } else if (node.nodeKind === "root") {
                const rootKind: RootKind = node.label === "entities" ? "entity"
                    : node.label === "relations" ? "relation" : "attribute";
                this.dataEditorState.openKindTab(rootKind);
                event.stopPropagation();
            }
        } else if (this.mode === "graph-view") {
            if (node.nodeKind === "concept") {
                this.graphViewState.openTypeTab(node.concept);
                event.stopPropagation();
            } else if (node.nodeKind === "root") {
                const rootKind: RootKind = node.label === "entities" ? "entity"
                    : node.label === "relations" ? "relation" : "attribute";
                this.graphViewState.openKindTab(rootKind);
                event.stopPropagation();
            }
        } else {
            // In other modes, clicking toggles expand/collapse
            if (flat.expandable) {
                this.state.toggleExpand(node);
                event.stopPropagation();
            }
        }
    }

    openLoadSampleDatasetDialog() {
        this.dialog.open(SampleDatasetDialogComponent);
    }

    openSchemaTextDialog() {
        this.dialog.open(SchemaTextDialogComponent, { width: "80vw", height: "80vh" });
    }

    /** Cmd/Ctrl+F opens the search overlay anywhere the schema tool window
     *  is alive. Hijacks the browser's "find in page" shortcut — matches the
     *  spec ("hit Ctrl/Cmd+F"). Esc closes the overlay regardless of focus
     *  (the input's own keydown only fires when the input itself is focused,
     *  so the user-reported "Esc only works when focused" came from missing
     *  the global path). */
    @HostListener("window:keydown", ["$event"])
    onWindowKeydown(event: KeyboardEvent) {
        if ((event.ctrlKey || event.metaKey) && (event.key === "f" || event.key === "F")) {
            event.preventDefault();
            this.openSearch();
            return;
        }
        if (this.searchOpen && event.key === "Escape") {
            event.preventDefault();
            this.closeSearch();
        }
    }

    /** Click anywhere outside the schema tool window dismisses the search
     *  bar. Inside-the-window clicks (tree rows, headers, etc.) keep it
     *  open so the user can browse the filtered list while the filter is
     *  still applied. Replaces an earlier blur-on-input handler — blur
     *  couldn't distinguish "focus moved to a tree row" (rows aren't
     *  focusable) from "focus moved entirely elsewhere", which made simply
     *  clicking a result dismiss the bar. */
    @HostListener("document:mousedown", ["$event"])
    onDocumentMouseDown(event: MouseEvent) {
        if (!this.searchOpen) return;
        const host = this.elementRef.nativeElement;
        if (!host.contains(event.target as Node)) {
            this.closeSearch();
        }
    }

    openSearch() {
        this.searchOpen = true;
        // detectChanges() forces the @if branch to render synchronously,
        // which also resolves the #searchInput ViewChild for the new
        // element. setTimeout (a macrotask) defers the actual focus call
        // past mat-menu's focus restoration — when the user opens the bar
        // via the "Find" actions-menu item, Material restores focus to the
        // menu trigger button after the click handler returns, which
        // otherwise steals focus right after we set it.
        this.cdr.detectChanges();
        setTimeout(() => this.searchInputRef?.nativeElement.focus());
    }

    closeSearch() {
        this.searchOpen = false;
        // Clearing also restores the original tree (filter goes inert when
        // the query is empty). Expansion state is untouched throughout.
        this.state.searchQuery$.next("");
        this.cdr.markForCheck();
    }

    onSearchInput(value: string) {
        this.state.searchQuery$.next(value);
    }

    onSearchKeydown(event: KeyboardEvent) {
        if (event.key === "Escape") {
            event.preventDefault();
            this.closeSearch();
        }
    }


    /** Display form for the root-row labels. The tree-node `label` field is
     *  the plural form ("entities" / "relations" / "attributes") because it
     *  doubles as a stable key in CSS classes and persisted view state; this
     *  helper just maps it to the singular wording shown in the UI. */
    rootDisplayLabel(label: string): string {
        return label === "entities" ? "entity"
            : label === "relations" ? "relation"
            : "attribute";
    }

    get searchHasNoResults(): boolean {
        return this.state.searchQuery$.value.trim().length > 0
            && this.state.flatNodes$.value.length === 0;
    }

    clearSearch() {
        // X-button: clear the filter but keep the bar open + focused so the
        // user can immediately type a new query.
        this.state.searchQuery$.next("");
        queueMicrotask(() => this.searchInputRef?.nativeElement.focus());
    }

    isNodeHighlighted(node: SchemaTreeNode): boolean {
        if (node.nodeKind !== "concept") return false;
        return node.concept.label === this.state.highlightedConceptLabel$.value;
    }

    openConceptContextMenu(event: MouseEvent, concept: SchemaConcept) {
        event.preventDefault();
        event.stopPropagation();
        this.conceptContextMenuPosition = { x: event.clientX, y: event.clientY };
        this.conceptContextMenuConcept = concept;
        this.conceptContextMenuTrigger.menuData = { concept };
        this.conceptContextMenuTrigger.openMenu();

        setTimeout(() => {
            const activeElement = document.activeElement as HTMLElement;
            if (activeElement?.classList.contains("mat-mdc-menu-item")) {
                activeElement.blur();
            }
        });
    }

    onRowContextMenu(flat: FlatSchemaTreeNode, event: MouseEvent) {
        if (flat.node.nodeKind === "concept") {
            this.openConceptContextMenu(event, flat.node.concept);
        } else if (flat.node.nodeKind === "root") {
            this.openRootContextMenu(event, flat.node.label);
        }
    }

    openRootContextMenu(event: MouseEvent, rootLabel: "entities" | "relations" | "attributes") {
        event.preventDefault();
        event.stopPropagation();
        this.rootContextMenuPosition = { x: event.clientX, y: event.clientY };
        // Schema tree labels them in plural ("entities") but the TypeQL
        // matcher kind is singular ("entity").
        const rootKind: RootKind = rootLabel === "entities" ? "entity"
            : rootLabel === "relations" ? "relation" : "attribute";
        this.rootContextMenuTrigger.menuData = { rootKind };
        this.rootContextMenuTrigger.openMenu();

        setTimeout(() => {
            const activeElement = document.activeElement as HTMLElement;
            if (activeElement?.classList.contains("mat-mdc-menu-item")) {
                activeElement.blur();
            }
        });
    }

    async copyTypeLabel(concept: SchemaConcept) {
        try {
            await navigator.clipboard.writeText(concept.label);
            this.snackbar.success("Type label copied", { duration: 2500 });
        } catch (e) {
            console.warn(e);
        }
    }

    loadInstances(concept: SchemaConcept) {
        const typeLabel = concept.label;
        const query = `match $x isa ${typeLabel}; fetch { $x.* };`;
        this.router.navigate(["/query"]).then(() => {
            const tab = this.queryTabsState.newTab();
            this.queryTabsState.renameTab(tab, `${typeLabel} instances`);
            this.queryTabsState.getTabControl(tab).setValue(query);
            this.queryPageState.runQuery(query);
        });
    }

    openDataTab(concept: SchemaConcept) {
        this.dataEditorState.openTypeTab(concept);
    }

    openGraphTabInstancesOnly(concept: SchemaConcept) {
        this.graphViewState.openTypeTab(concept);
    }

    openGraphTabWithLinks(concept: SchemaConcept) {
        this.graphViewState.openTypeTab(concept, { includeLinks: true });
    }

    openGraphTabWithAttributes(concept: SchemaConcept) {
        this.graphViewState.openTypeTab(concept, { includeAttributes: true });
    }

    loadKindInstances(rootKind: RootKind) {
        const query = kindInstancesQuery(rootKind);
        const title = `${rootKind} instances`;
        this.router.navigate(["/query"]).then(() => {
            const tab = this.queryTabsState.newTab();
            this.queryTabsState.renameTab(tab, title);
            this.queryTabsState.getTabControl(tab).setValue(query);
            this.queryPageState.runQuery(query);
        });
    }

    openGraphKindTab(rootKind: RootKind) {
        this.graphViewState.openKindTab(rootKind);
    }

    openDataKindTab(rootKind: RootKind) {
        this.dataEditorState.openKindTab(rootKind);
    }
}

