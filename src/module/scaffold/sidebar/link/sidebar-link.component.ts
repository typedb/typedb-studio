/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, Input } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { AsyncPipe, NgClass, NgTemplateOutlet } from "@angular/common";
import { map } from "rxjs";

@Component({
    selector: "tp-sidebar-link",
    templateUrl: "./sidebar-link.component.html",
    styleUrls: ["./sidebar-link.component.scss"],
    standalone: true,
    imports: [RouterLink, NgClass, AsyncPipe, NgTemplateOutlet],
})
export class SidebarLinkComponent {
    @Input() link?: string;
    @Input({ required: true }) idSuffix!: string;
    @Input() domain?: any;
    @Input() external = false;
    selected$ = this.route.data.pipe(map(data => !!this.domain && this.domain === data["domain"]));

    constructor(private route: ActivatedRoute) {}

    get linkId(): string {
        return `sidebar_${this.idSuffix}`;
    }
}
