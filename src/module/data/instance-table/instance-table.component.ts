/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input, OnInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { MatTableModule } from "@angular/material/table";
import { MatSortModule } from "@angular/material/sort";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatDialog } from "@angular/material/dialog";
import { Subject, Subscription } from "rxjs";
import { debounceTime, distinctUntilChanged } from "rxjs/operators";
import { TypeTableTab, DataEditorState, BreadcrumbItem } from "../../../service/data-editor-state.service";
import { DriverState } from "../../../service/driver-state.service";
import { ApiResponse, Concept, ConceptRow, ConceptRowAnswer, isApiErrorResponse, QueryResponse } from "@typedb/driver-http";
import { SnackbarService } from "../../../service/snackbar.service";
import { AdvancedFilterDialogComponent, AdvancedFilterDialogData, AdvancedFilterDialogResult } from "./advanced-filter-dialog/advanced-filter-dialog.component";
import { extractErrorMessage } from "../../../framework/util/observable";

/** Primitive value types that TypeDB attributes can hold */
type AttributeValue = string | number | boolean | Date;

/** Maps relation type label to count of relations of that type */
type RelationCountsByType = Record<string, number>;

export interface InstanceRow {
    iid: string;
    type: string;
    kind: "entity" | "relation";
    relationCounts: RelationCountsByType | null;
    /** Dynamic attribute columns store arrays of values */
    [key: string]: string | RelationCountsByType | null | AttributeValue[] | "entity" | "relation" | undefined;
}

@Component({
    selector: "ts-instance-table",
    templateUrl: "./instance-table.component.html",
    styleUrls: ["./instance-table.component.scss"],
    imports: [
        CommonModule,
        FormsModule,
        MatTableModule,
        MatSortModule,
        MatPaginatorModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatButtonToggleModule,
        MatTooltipModule,
    ],
})
export class InstanceTableComponent implements OnInit, OnDestroy {
    @Input({ required: true }) tab!: TypeTableTab;

    displayedColumns: string[] = [];
    attributeColumns: string[] = [];
    dataSource: InstanceRow[] = [];
    totalCount = 0;
    loading = false;
    loadingStartTime = 0;
    showSpinner = false;

    // Pagination
    currentPage = 0;
    pageSize = 100;
    pageSizeOptions = [50, 100, 200];

    // Sorting
    sortColumn = "";
    sortDirection: "asc" | "desc" = "asc";

    // Filter
    filterText = "";
    filterMode: "simple" | "advanced" = "simple";
    private filterSubject = new Subject<string>();

    private subscriptions: Subscription[] = [];

    constructor(
        private driver: DriverState,
        private dataEditorState: DataEditorState,
        private snackbar: SnackbarService,
        private dialog: MatDialog,
    ) {}

    ngOnInit() {
        this.buildColumns();
        this.fetchInstances();
        this.fetchTotalCount();

        // Debounce filter input to avoid excessive server requests
        this.subscriptions.push(
            this.filterSubject.pipe(
                debounceTime(300),
                distinctUntilChanged()
            ).subscribe(filterText => {
                this.filterText = filterText;
                this.currentPage = 0;
                this.fetchInstances();
                this.fetchTotalCount();
            })
        );
    }

    ngOnDestroy() {
        this.subscriptions.forEach(sub => sub.unsubscribe());
    }

    private buildColumns() {
        // Use schema to determine which attributes this type owns
        const type = this.tab.type;
        if (type.kind === "entityType" || type.kind === "relationType") {
            this.attributeColumns = type.ownedAttributes.map(a => a.label);
        }
        this.displayedColumns = ["type", "iid", ...this.attributeColumns, "relationCounts"];
    }

    private async fetchInstances() {
        this.loading = true;
        this.loadingStartTime = Date.now();
        this.showSpinner = false;

        // Show spinner only after 1 second
        const spinnerTimeout = setTimeout(() => {
            if (this.loading) {
                this.showSpinner = true;
            }
        }, 1000);

        try {
            const offset = this.currentPage * this.pageSize;
            const query = this.buildInstanceQuery(offset, this.pageSize);

            // Execute query via driver
            this.driver.query(query).subscribe({
                next: (res: ApiResponse<QueryResponse>) => {
                    clearTimeout(spinnerTimeout);
                    this.loading = false;
                    this.showSpinner = false;

                    if (isApiErrorResponse(res)) {
                        this.snackbar.errorPersistent(`Error fetching instances: ${res.err.message}`);
                        this.dataSource = [];
                        return;
                    }

                    // Transform response to table rows
                    if (res.ok.answerType === "conceptRows") {
                        const answers = (res.ok as any).answers as ConceptRowAnswer[];
                        this.dataSource = this.transformToTableRows(answers);
                        this.fetchRelationCounts();
                    } else {
                        this.dataSource = [];
                    }
                },
                error: (err) => {
                    clearTimeout(spinnerTimeout);
                    this.loading = false;
                    this.showSpinner = false;
                    let msg = ``;
                    if (isApiErrorResponse(err)) {
                        msg = err.err.message;
                    } else {
                        msg = err?.message ?? err?.toString() ?? `Unknown error`;
                    }
                    this.snackbar.errorPersistent(`Error fetching instances:\n${msg}`);
                    this.dataSource = [];
                }
            });
        } catch (error) {
            clearTimeout(spinnerTimeout);
            this.loading = false;
            this.showSpinner = false;
            console.error("Error fetching instances:", error);
            this.snackbar.errorPersistent(`Error fetching instances: ${extractErrorMessage(error)}`);
        }
    }

    private async fetchTotalCount() {
        try {
            const tabFilter = this.tab.typeqlFilter || "";
            const query = `match $instance isa ${this.tab.type.label}; ${tabFilter} reduce $count = count;`;

            this.driver.query(query).subscribe({
                next: (res: ApiResponse<QueryResponse>) => {
                    if (isApiErrorResponse(res)) {
                        console.error("Error fetching count:", res.err.message);
                        return;
                    }

                    // Parse the count from the reduce result
                    if (res.ok.answerType === "conceptRows") {
                        const answers = (res.ok as any).answers as ConceptRowAnswer[];
                        if (answers.length > 0) {
                            const countValue = answers[0].data["count"];
                            if (countValue && countValue.kind === "value") {
                                this.totalCount = countValue.value as number;
                                this.dataEditorState.updateTabCount(this.tab, this.totalCount);
                                return;
                            }
                        }
                    }
                    // Fallback to row count
                    this.totalCount = this.dataSource.length;
                    this.dataEditorState.updateTabCount(this.tab, this.totalCount);
                },
                error: (err) => {
                    console.error("Error fetching total count:", err);
                }
            });
        } catch (error) {
            console.error("Error fetching total count:", error);
        }
    }

    private fetchRelationCounts() {
        if (this.dataSource.length === 0) return;

        // Build query to count relations for all instances in current page
        // Use disjunction to match any of the instance IIDs
        const iidBranches = this.dataSource.map(row => `{ $instance iid ${row.iid}; }`).join(" or ");
        const query = `match ${iidBranches}; $rel links ($instance);`;

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching relation counts: ${res.err.message}`);
                    for (const row of this.dataSource) {
                        row.relationCounts = {};
                    }
                    return;
                }

                // Count unique relations per instance, grouped by relation type
                // Map: instance IID -> relation type -> Set of relation IIDs
                const countsByIid = new Map<string, Map<string, Set<string>>>();
                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    for (const answer of answers) {
                        const instance = answer.data["instance"];
                        const rel = answer.data["rel"];
                        if (instance?.kind === "entity" || instance?.kind === "relation") {
                            if (rel?.kind === "relation") {
                                if (!countsByIid.has(instance.iid)) {
                                    countsByIid.set(instance.iid, new Map());
                                }
                                const typeMap = countsByIid.get(instance.iid)!;
                                const relType = rel.type.label;
                                if (!typeMap.has(relType)) {
                                    typeMap.set(relType, new Set());
                                }
                                typeMap.get(relType)!.add(rel.iid);
                            }
                        }
                    }
                }

                // Update rows with counts by type (empty object if no relations found)
                for (const row of this.dataSource) {
                    const typeMap = countsByIid.get(row.iid);
                    if (typeMap) {
                        const counts: RelationCountsByType = {};
                        for (const [type, iids] of typeMap) {
                            counts[type] = iids.size;
                        }
                        row.relationCounts = counts;
                    } else {
                        row.relationCounts = {};
                    }
                }
            },
            error: (err) => {
                this.snackbar.errorPersistent(`Error fetching relation counts: ${extractErrorMessage(err)}`);
                for (const row of this.dataSource) {
                    row.relationCounts = {};
                }
            }
        });
    }

    private transformToTableRows(conceptRowAnswers: ConceptRowAnswer[]): InstanceRow[] {
        const rowsByIid = new Map<string, InstanceRow>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;

            // Extract instance IID
            const instanceConcept = row["instance"];
            if (!instanceConcept || (instanceConcept.kind !== "entity" && instanceConcept.kind !== "relation")) {
                continue;
            }

            const iid = instanceConcept.iid;
            const type = instanceConcept.type.label;
            const kind = instanceConcept.kind as "entity" | "relation";

            // Get existing row or create new one
            let tableRow = rowsByIid.get(iid);
            if (!tableRow) {
                tableRow = {
                    iid,
                    type,
                    kind,
                    relationCounts: null,
                };
                rowsByIid.set(iid, tableRow);
            }

            // Extract attribute values (aggregate into arrays for multi-valued attributes)
            for (const attrLabel of this.attributeColumns) {
                const attrConcept = row[attrLabel];
                if (attrConcept) {
                    const value = this.extractAttributeValue(attrConcept);
                    if (value != null) {
                        const existing = tableRow[attrLabel] as AttributeValue[] | undefined;
                        if (!existing) {
                            tableRow[attrLabel] = [value];
                        } else if (!existing.some(v => String(v) === String(value))) {
                            existing.push(value);
                        }
                    }
                }
            }
        }

        return Array.from(rowsByIid.values());
    }

    private extractAttributeValue(concept: Concept): AttributeValue | null {
        if (!concept) return null;

        switch (concept.kind) {
            case "attribute":
                return concept.value as AttributeValue;
            case "value":
                return concept.value as AttributeValue;
            default:
                return null;
        }
    }

    private buildInstanceQuery(offset: number, limit: number): string {
        const type = this.tab.type;

        // Use try blocks to make attribute fetching optional
        const attributeClauses = this.attributeColumns.map(attr =>
            `try { $instance has ${attr} $${attr}; };`
        ).join("\n    ");

        let filterClause = "";
        if (this.filterText) {
            if (this.filterMode === "simple") {
                // Simple mode: search in all text attributes
                filterClause = `$instance has $attr; $attr contains "${this.filterText}";`;
            } else {
                // Advanced mode: use raw TypeQL
                filterClause = this.filterText;
            }
        }

        // Tab-level TypeQL filter (e.g., for filtered relation views)
        const tabFilter = this.tab.typeqlFilter || "";

        const sortClause = this.sortColumn && this.sortColumn !== "relationCounts" && this.sortColumn !== "iid"
            ? `sort $${this.sortColumn} ${this.sortDirection};`
            : "";

        // Build select clause with all variables we want to return
        const selectVars = ["$instance", ...this.attributeColumns.map(attr => `$${attr}`)].join(", ");

        const tabFilterLine = tabFilter ? `${tabFilter}\n    ` : "";
        const filterLine = filterClause ? `${filterClause}\n    ` : "";

        return `match
    $instance isa ${type.label};
    ${tabFilterLine}${filterLine}${attributeClauses}
select ${selectVars};
distinct;
${sortClause}offset ${offset}; limit ${limit};`.trim();
    }

    private readonly MAX_DISPLAY_LENGTH = 50;

    formatAttributeValue(value: AttributeValue[] | null | undefined): string {
        if (value == null) return "-";
        if (value.length === 0) return "-";
        if (value.length === 1) return String(value[0]);
        return value.join(", ");
    }

    truncateValue(value: AttributeValue[] | null | undefined): string {
        const formatted = this.formatAttributeValue(value);
        if (formatted.length <= this.MAX_DISPLAY_LENGTH) return formatted;
        return formatted.substring(0, this.MAX_DISPLAY_LENGTH) + "…";
    }

    shouldShowTooltip(value: AttributeValue[] | null | undefined): boolean {
        const formatted = this.formatAttributeValue(value);
        return formatted.length > this.MAX_DISPLAY_LENGTH;
    }

    copyToClipboard(event: Event, value: AttributeValue[] | null | undefined) {
        event.stopPropagation(); // Prevent row click
        const text = this.formatAttributeValue(value);
        navigator.clipboard.writeText(text).then(() => {
            this.snackbar.success("Copied to clipboard");
        });
    }

    formatRelationCounts(counts: RelationCountsByType | null | undefined): string {
        if (counts == null) return "…";
        const entries = Object.entries(counts);
        if (entries.length === 0) return "-";
        // Calculate total
        const total = entries.reduce((sum, [, count]) => sum + count, 0);
        // Sort by type name for consistent display
        entries.sort((a, b) => a[0].localeCompare(b[0]));
        const breakdown = entries.map(([type, count]) => `${count} ${type}`).join(", ");
        return `${total} (${breakdown})`;
    }

    shouldShowRelationTooltip(counts: RelationCountsByType | null | undefined): boolean {
        const formatted = this.formatRelationCounts(counts);
        return formatted.length > this.MAX_DISPLAY_LENGTH;
    }

    truncateRelationCounts(counts: RelationCountsByType | null | undefined): string {
        const formatted = this.formatRelationCounts(counts);
        if (formatted.length <= this.MAX_DISPLAY_LENGTH) return formatted;
        return formatted.substring(0, this.MAX_DISPLAY_LENGTH) + "…";
    }

    get breadcrumbs(): BreadcrumbItem[] {
        return this.tab.breadcrumbs || [];
    }

    navigateToBreadcrumb(breadcrumb: BreadcrumbItem, index: number) {
        this.dataEditorState.navigateToBreadcrumb(breadcrumb, index, this.breadcrumbs);
    }

    onRowClick(row: InstanceRow) {
        // Create breadcrumb back to this table, preserving any existing breadcrumbs
        const breadcrumbs: BreadcrumbItem[] = [
            ...this.breadcrumbs,
            { kind: "type-table", typeLabel: this.tab.type.label }
        ];
        this.dataEditorState.openInstanceDetail(this.tab.type, row.iid, breadcrumbs);
    }

    onPageChange(event: any) {
        this.currentPage = event.pageIndex;
        this.pageSize = event.pageSize;
        this.fetchInstances();
    }

    onSortChange(event: any) {
        this.sortColumn = event.active;
        this.sortDirection = event.direction;
        this.fetchInstances();
    }

    onFilterChange(filterText: string) {
        // Only auto-apply filter in simple mode
        if (this.filterMode === "simple") {
            this.filterSubject.next(filterText);
        }
    }

    openAdvancedFilterDialog() {
        const dialogData: AdvancedFilterDialogData = {
            typeLabel: this.tab.type.label,
            attributeColumns: this.attributeColumns,
            currentFilter: this.filterMode === "advanced" ? this.filterText : "",
            tabFilter: this.tab.typeqlFilter || "",
        };

        const dialogRef = this.dialog.open<AdvancedFilterDialogComponent, AdvancedFilterDialogData, AdvancedFilterDialogResult>(
            AdvancedFilterDialogComponent,
            {
                data: dialogData,
                width: "600px",
            }
        );

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.filterMode = "advanced";
                this.filterText = result.filter;
                this.currentPage = 0;
                this.fetchInstances();
                this.fetchTotalCount();
            }
        });
    }

    clearAdvancedFilter() {
        this.filterMode = "simple";
        this.filterText = "";
        this.currentPage = 0;
        this.fetchInstances();
        this.fetchTotalCount();
    }

    refresh() {
        this.fetchInstances();
        this.fetchTotalCount();
    }
}
