/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
