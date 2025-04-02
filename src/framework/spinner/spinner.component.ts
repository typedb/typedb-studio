/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, Input } from "@angular/core";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";

@Component({
    selector: "tp-spinner",
    template: `<mat-spinner [diameter]="size"/>`,
    styleUrls: ["./spinner.component.scss"],
    standalone: true,
    imports: [MatProgressSpinnerModule],
})
export class SpinnerComponent {
    @Input() size = 32;
}
