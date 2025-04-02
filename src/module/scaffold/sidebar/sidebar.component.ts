/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, HostBinding, Input, Output, ViewChild } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { SidebarState } from "../../../concept/view-state";
import { AppDataService } from "../../../service/app-data.service";
import { AsyncPipe, NgClass } from "@angular/common";
import { SidebarLinkComponent } from "./link/sidebar-link.component";
import { MatSelect, MatSelectModule } from "@angular/material/select";
import { MatDividerModule } from "@angular/material/divider";
import { MatMenuModule } from "@angular/material/menu";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ModalComponent } from "../../../framework/modal";

@Component({
    selector: "ts-sidebar",
    templateUrl: "./sidebar.component.html",
    styleUrls: ["./sidebar.component.scss"],
    standalone: true,
    imports: [
        SidebarLinkComponent, ModalComponent, AsyncPipe, MatSelectModule, MatDividerModule,
        MatMenuModule, RouterLink, NgClass, MatTooltipModule
    ],
})
export class SidebarComponent {
    @ViewChild(MatSelect) orgSelector!: MatSelect;
    expandedState = this.app.viewState.sidebarState();
    @Output() expandedStateChange = new EventEmitter<SidebarState>();

    constructor(
        public app: AppDataService, private route: ActivatedRoute,
        // private dialog: MatDialog,
    ) {}

    get collapsed(): boolean {
        return this.expandedState === "collapsed";
    }

    toggleCollapsed() {
        this.expandedState = this.expandedState === "expanded" ? "collapsed" : "expanded";
        this.app.viewState.setSidebarState(this.expandedState);
        this.expandedStateChange.emit(this.expandedState);
    }
}
