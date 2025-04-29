/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { BaseConcept } from "../../../concept/base";
import { ResourceTable } from "../../../service/resource-table.service";

@Component({
    selector: "tp-table-selection-header-cell",
    templateUrl: "./table-selection-header-cell.component.html",
    standalone: true,
    imports: [MatCheckboxModule],
})
export class TableSelectionHeaderCellComponent<ENTITY extends BaseConcept> {
    @Input({ required: true }) table!: ResourceTable<ENTITY, string>;
}
