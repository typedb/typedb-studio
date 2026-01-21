/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Inject } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { ModalComponent } from "../../../../framework/modal";

export interface AdvancedFilterDialogData {
    typeLabel: string;
    attributeColumns: string[];
    currentFilter: string;
    tabFilter: string;
}

export interface AdvancedFilterDialogResult {
    filter: string;
}

@Component({
    selector: "ts-advanced-filter-dialog",
    templateUrl: "./advanced-filter-dialog.component.html",
    styleUrls: ["./advanced-filter-dialog.component.scss"],
    imports: [
        ModalComponent,
        FormsModule,
        MatButtonModule,
    ]
})
export class AdvancedFilterDialogComponent {
    filterClause: string;

    constructor(
        private dialogRef: MatDialogRef<AdvancedFilterDialogComponent, AdvancedFilterDialogResult>,
        @Inject(MAT_DIALOG_DATA) public data: AdvancedFilterDialogData,
    ) {
        this.filterClause = data.currentFilter;
    }

    get previewQuery(): string {
        // Use try blocks to make attribute fetching optional
        const attributeClauses = this.data.attributeColumns.map(attr =>
            `try { $instance has ${attr} $${attr}; };`
        ).join("\n    ");

        const selectVars = ["$instance", ...this.data.attributeColumns.map(attr => `$${attr}`)].join(", ");

        const tabFilter = this.data.tabFilter ? `${this.data.tabFilter}\n    ` : "";
        const filterLine = this.filterClause ? `${this.filterClause}\n    ` : "";

        return `match
    $instance isa ${this.data.typeLabel};
    ${tabFilter}${filterLine}${attributeClauses}
select ${selectVars};
distinct;
offset 0; limit 100;`;
    }

    apply() {
        this.dialogRef.close({ filter: this.filterClause });
    }

    clear() {
        this.filterClause = "";
    }

    close() {
        this.dialogRef.close();
    }
}
