/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { AfterViewInit, Component, HostBinding, Input, Output, EventEmitter, ViewChild } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { MatTree, MatTreeModule } from "@angular/material/tree";
import { MatTooltipModule } from "@angular/material/tooltip";
import { combineLatest, map } from "rxjs";
import { MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { SchemaToolWindowState, SchemaTreeNode, SchemaTreeConceptNode } from "../../../service/schema-tool-window-state.service";
import { DriverState } from "../../../service/driver-state.service";
import { AppData } from "../../../service/app-data.service";
import { SchemaTreeNodeComponent } from "../tree-node/schema-tree-node.component";
import { MatDialog } from "@angular/material/dialog";
import { SchemaTextDialogComponent } from "../text-dialog/schema-text-dialog.component";
import { SchemaConcept } from "../../../service/schema-state.service";
import { DataEditorState } from "../../../service/data-editor-state.service";
import { QueryTabsState } from "../../../service/query-tabs-state.service";
import { QueryPageState } from "../../../service/query-page-state.service";
import { SnackbarService } from "../../../service/snackbar.service";

@Component({
    selector: "ts-schema-tool-window",
    templateUrl: "schema-tool-window.component.html",
    styleUrls: ["schema-tool-window.component.scss"],
    imports: [
        AsyncPipe, MatDividerModule, MatFormFieldModule, MatTreeModule, MatIconModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule,
        MatTableModule, MatSortModule, MatTooltipModule, MatButtonModule, MatMenuModule, SchemaTreeNodeComponent,
    ]
})
export class SchemaToolWindowComponent implements AfterViewInit {

    @Input() title = "Schema";
    @Input() mode: "schema" | "data" = "schema";
    @HostBinding("class") readonly clazz = "schema-pane";
    @ViewChild("tree") tree!: MatTree<any>;
    @ViewChild("conceptContextMenuTrigger") conceptContextMenuTrigger!: MatMenuTrigger;
    refreshSchemaTooltip$ = this.state.schema.interactionDisabledReason$.pipe(map(x => x ? `` : `Refresh`));
    actionsButtonTooltip$ = this.state.schema.interactionDisabledReason$.pipe(map(x => x ? x : `More actions`));

    // Concept context menu state
    conceptContextMenuPosition = { x: 0, y: 0 };
    conceptContextMenuConcept: SchemaConcept | null = null;

    constructor(
        public state: SchemaToolWindowState,
        public driver: DriverState,
        private appData: AppData,
        private dialog: MatDialog,
        private dataEditorState: DataEditorState,
        private queryTabsState: QueryTabsState,
        private queryPageState: QueryPageState,
        private snackbar: SnackbarService,
    ) {
        // Subscribe to active tab changes to highlight the corresponding type in the schema tree
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
    }

    ngAfterViewInit() {
        this.state.dataSource$.subscribe((dataSource) => {
            // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
            setTimeout(() => {
                dataSource.forEach( node => {
                    // In data mode, expand all recursively
                    if (this.mode === "data") {
                        this.expandNodeRecursively(node);
                    } else {
                        if (this.state.rootNodesCollapsed[node.label]) {
                            this.tree.collapse(node);
                        } else {
                            this.tree.expand(node);
                        }
                    }
                });
            });
        });

        this.state.expandAll$.subscribe(() => {
            setTimeout(() => {
                // Use expandDescendants for each root node to expand all levels
                this.state.dataSource$.value.forEach(node => this.tree.expandDescendants(node));
            });
        });

        this.state.collapseAll$.subscribe(() => {
            setTimeout(() => {
                // Expand root nodes, collapse only concept nodes' descendants (subtypes)
                this.state.dataSource$.value.forEach(node => {
                    this.tree.expand(node);
                    node.children?.forEach(conceptNode => this.tree.collapseDescendants(conceptNode));
                });
            });
        });
    }

    private expandNodeRecursively(node: SchemaTreeNode) {
        this.tree.expandDescendants(node);
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
        if (node.nodeKind === "root") {
            // For root nodes, toggle between showing all expanded vs collapsed to second level
            const allCollapsed = node.children?.every(child => !this.tree.isExpanded(child)) ?? true;
            if (allCollapsed) {
                this.tree.expandDescendants(node);
            } else {
                this.tree.expand(node);
                node.children?.forEach(conceptNode => this.tree.collapseDescendants(conceptNode));
            }
            this.state.rootNodesCollapsed[node.label] = false;
            const schemaToolWindowState = this.appData.viewState.schemaToolWindowState();
            schemaToolWindowState.rootNodesCollapsed = this.state.rootNodesCollapsed;
            this.appData.viewState.setSchemaToolWindowState(schemaToolWindowState);
        } else {
            if (this.tree.isExpanded(node)) {
                this.tree.collapse(node);
            } else {
                this.tree.expand(node);
            }
        }
    }

    onNodeClick(node: SchemaTreeNode, event: Event) {
        if (this.mode === "data") {
            // In data mode, clicking on the concept body opens a tab
            if (node.nodeKind === "concept") {
                this.dataEditorState.openTypeTab(node.concept);
                event.stopPropagation();
            }
        } else {
            // In other modes, clicking toggles expand/collapse
            if (node.nodeKind === "concept" || node.nodeKind === "root") {
                this.toggleNode(node);
                event.stopPropagation();
            }
        }
    }

    openSchemaTextDialog() {
        this.dialog.open(SchemaTextDialogComponent, { width: "80vw", height: "80vh" });
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
        const tab = this.queryTabsState.newTab();
        this.queryTabsState.renameTab(tab, `${typeLabel} instances`);
        this.queryTabsState.getTabControl(tab).setValue(query);
        this.queryPageState.runQuery(query);
    }
}
