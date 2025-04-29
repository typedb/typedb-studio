/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";

@Component({
    selector: "tp-link-panel",
    templateUrl: "link-panel.component.html",
    styleUrls: ["link-panel.component.scss"],
    standalone: true,
    imports: [],
})
export class LinkPanelComponent {
    @Input({ required: true }) title!: string;
    @Input({ required: true }) iconURL!: string;
    @Input({ required: true }) href!: string;
}

// Copied from typedb-web
@Component({
    selector: "tp-link-panels",
    template: "<ng-content/>",
    styleUrls: ["./link-panels.component.scss"],
    standalone: true,
    imports: [],
})
export class LinkPanelsComponent {}
