/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
