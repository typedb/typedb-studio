/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, Input } from "@angular/core";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { BaseConcept } from "../../../concept/base";
import { ResourceTable } from "../../../service/resource-table.service";

@Component({
    selector: "tp-table-selection-cell",
    templateUrl: "./table-selection-cell.component.html",
    standalone: true,
    imports: [MatCheckboxModule],
})
export class TableSelectionCellComponent<ENTITY extends BaseConcept> {
    @Input({ required: true }) table!: ResourceTable<ENTITY, string>;
    @Input({ required: true }) row!: ENTITY;
}
