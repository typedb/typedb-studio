/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { AsyncPipe, NgClass, NgTemplateOutlet } from "@angular/common";
import { map } from "rxjs";

@Component({
    selector: "ts-sidebar-link",
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
