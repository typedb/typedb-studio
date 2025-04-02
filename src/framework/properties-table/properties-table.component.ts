/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
