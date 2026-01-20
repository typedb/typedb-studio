/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input, OnInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltip, MatTooltipModule } from "@angular/material/tooltip";
import { Clipboard } from "@angular/cdk/clipboard";
import { SchemaConcept } from "../../../service/schema-state.service";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { ApiResponse, ConceptRowAnswer, isApiErrorResponse, QueryResponse } from "@typedb/driver-http";

interface AttributeData {
    type: string;
    values: string[];
}

@Component({
    selector: "ts-instance-detail",
    templateUrl: "./instance-detail.component.html",
    styleUrls: ["./instance-detail.component.scss"],
    imports: [
        CommonModule,
        MatProgressSpinnerModule,
        MatButtonModule,
        MatTooltipModule,
    ],
})
export class InstanceDetailComponent implements OnInit, OnDestroy {
    @Input({ required: true }) type!: SchemaConcept;
    @Input({ required: true }) instanceIID!: string;

    attributes: AttributeData[] = [];
    loading = false;

    constructor(
        private driver: DriverState,
        private snackbar: SnackbarService,
        private clipboard: Clipboard,
    ) {}

    copyValue(value: string, tooltip: MatTooltip) {
        this.clipboard.copy(value);
        tooltip.show(0);
        setTimeout(() => tooltip.hide(0), 1000);
    }

    ngOnInit() {
        this.fetchInstanceData();
    }

    ngOnDestroy() {
        // Cleanup if needed
    }

    private fetchInstanceData() {
        this.loading = true;

        // Fetch all attributes for this instance
        const query = `
match
    $instance isa ${this.type.label};
    $instance iid ${this.instanceIID};
    $instance has $attr;
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                this.loading = false;

                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching instance data: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processAttributes(answers);
                }
            },
            error: (err) => {
                this.loading = false;
                this.snackbar.errorPersistent(`Error fetching instance data: ${err.message || err}`);
            }
        });
    }

    private processAttributes(conceptRowAnswers: ConceptRowAnswer[]) {
        // Group attributes by type
        const attrMap = new Map<string, string[]>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;
            const attrConcept = row["attr"];
            if (attrConcept && attrConcept.kind === "attribute") {
                const attrType = attrConcept.type?.label || "unknown";
                const attrValue = String(attrConcept.value);

                if (!attrMap.has(attrType)) {
                    attrMap.set(attrType, []);
                }
                attrMap.get(attrType)!.push(attrValue);
            }
        }

        // Convert to AttributeData array
        this.attributes = Array.from(attrMap.entries()).map(([type, values]) => ({
            type,
            values,
        }));
    }
}
