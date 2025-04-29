/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { SpinnerComponent } from "../../spinner/spinner.component";
import { ColorStyle } from "../../util";

export type StatusIcon = { loading: true, paused: boolean } | { color: ColorStyle };

@Component({
    selector: "tp-table-status-cell",
    templateUrl: "./table-status-cell.component.html",
    styleUrls: ["./table-status-cell.component.scss"],
    standalone: true,
    imports: [MatCheckboxModule, SpinnerComponent],
})
export class TableStatusCellComponent {
    @Input({ required: true }) status!: StatusIcon;
}
