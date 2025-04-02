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
