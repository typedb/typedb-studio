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
