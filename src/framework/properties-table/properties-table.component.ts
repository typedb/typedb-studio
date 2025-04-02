/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";

@Component({
    selector: "tp-properties-table",
    templateUrl: "properties-table.component.html",
    styleUrls: ["properties-table.component.scss"],
    standalone: true,
})
export class PropertiesTableComponent {
    @Input() title?: string;
}
