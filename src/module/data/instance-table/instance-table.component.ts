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

export interface InstanceRow {
    iid: string;
    [key: string]: any;
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
        this.displayedColumns = ["iid", ...this.attributeColumns, "relations"];
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
            this.snackbar.errorPersistent(`Error fetching instances: ${error}`);
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

            // Get existing row or create new one
            let tableRow = rowsByIid.get(iid);
            if (!tableRow) {
                tableRow = {
                    iid,
                    ["relations"]: [] as any[]
                };
                rowsByIid.set(iid, tableRow);
            }

            // Extract attribute values (merge with existing)
            for (const attrLabel of this.attributeColumns) {
                const attrConcept = row[attrLabel];
                if (attrConcept) {
                    const value = this.extractAttributeValue(attrConcept);
                    if (tableRow[attrLabel] === undefined || tableRow[attrLabel] === null) {
                        tableRow[attrLabel] = value;
                    }
                }
            }
        }

        return Array.from(rowsByIid.values());
    }

    private extractAttributeValue(concept: Concept): any {
        if (!concept) return null;

        switch (concept.kind) {
            case "attribute":
                return concept.value;
            case "value":
                return concept.value;
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

        const sortClause = this.sortColumn && this.sortColumn !== "relations" && this.sortColumn !== "iid"
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

    formatAttributeValue(value: any): string {
        if (value == null) return "-";
        if (Array.isArray(value)) {
            if (value.length === 0) return "-";
            if (value.length === 1) return String(value[0]);
            return `${value[0]} (+${value.length - 1} more)`;
        }
        return String(value);
    }

    formatRelationCount(relationCounts: any): string {
        if (!relationCounts || relationCounts.length === 0) return "-";
        return `${relationCounts.length} relations`;
    }

    onRowClick(row: InstanceRow) {
        // Create breadcrumb back to this table
        const breadcrumbs: BreadcrumbItem[] = [
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
