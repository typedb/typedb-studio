/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, Input } from "@angular/core";
import { SpinnerComponent } from "../../spinner/spinner.component";

@Component({
    selector: "tp-properties-table-row",
    templateUrl: "properties-table-row.component.html",
    styleUrls: ["./properties-table-row.component.scss"],
    standalone: true,
    imports: [SpinnerComponent],
})
export class PropertiesTableRowComponent {
    @Input({ required: true }) key!: string;
    @Input() loading?: boolean | null;
}
