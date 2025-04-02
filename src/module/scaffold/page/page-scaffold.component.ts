/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { animate, state, style, transition, trigger } from "@angular/animations";
import { Component, Input } from "@angular/core";
import { MatSidenavModule } from "@angular/material/sidenav";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ButtonComponent } from "../../../framework/button/button.component";
import { SpinnerComponent } from "../../../framework/spinner/spinner.component";
import { AppDataService } from "../../../service/app-data.service";
import { SidebarComponent } from "../sidebar/sidebar.component";
import { RightSidebarComponent } from "../sidebar/right/right-sidebar.component";
import { NgClass } from "@angular/common";

export type ResourceAvailability = "ready" | "loading" | "failed";

@Component({
    selector: "ts-page-scaffold",
    templateUrl: "./page-scaffold.component.html",
    styleUrls: ["./page-scaffold.component.scss"],
    standalone: true,
    imports: [
        SidebarComponent, RightSidebarComponent, SpinnerComponent, NgClass, MatSidenavModule, ButtonComponent,
        MatTooltipModule,
    ],
    animations: [
        trigger("sidebarLeftMargin", [
            state("open", style({ "margin-left": "289px" })),
            state("collapsed", style({ "margin-left": "101px" })),
            transition("open <=> collapsed", animate("250ms ease"))
        ])
    ],
})
export class PageScaffoldComponent {
    @Input() pageAvailability: ResourceAvailability | null = "ready";

    leftSidebarState = this.appData.viewState.sidebarState();

    constructor(private appData: AppDataService) {}
}
