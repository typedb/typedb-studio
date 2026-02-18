/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, HostBinding, Input, Output, ViewChild } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { SidebarState } from "../../../concept/view-state";
import { AppData } from "../../../service/app-data.service";
import { AsyncPipe, NgClass } from "@angular/common";
import { ConnectionWidgetComponent } from "../../connection/widget/connection-widget.component";
import { SidebarLinkComponent } from "./link/sidebar-link.component";
import { MatSelect, MatSelectModule } from "@angular/material/select";
import { MatDividerModule } from "@angular/material/divider";
import { MatMenuModule } from "@angular/material/menu";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ModalComponent } from "../../../framework/modal";
import { MatDialog } from "@angular/material/dialog";
import { AppInfoDialogComponent } from "../../about/app-info-dialog.component";

@Component({
    selector: "ts-sidebar",
    templateUrl: "./sidebar.component.html",
    styleUrls: ["./sidebar.component.scss"],
    imports: [SidebarLinkComponent, MatSelectModule, MatDividerModule, MatMenuModule, MatTooltipModule]
})
export class SidebarComponent {
    @ViewChild(MatSelect) orgSelector!: MatSelect;
    expandedState: SidebarState = this.app.viewState.sidebarState();
    @Output() expandedStateChange = new EventEmitter<SidebarState>();

    constructor(
        public app: AppData, private route: ActivatedRoute,
        private dialog: MatDialog,
    ) {}

    get collapsed(): boolean {
        return this.expandedState === "collapsed";
    }

    toggleCollapsed() {
        this.expandedState = this.expandedState === "expanded" ? "collapsed" : "expanded";
        this.app.viewState.setSidebarState(this.expandedState);
        this.expandedStateChange.emit(this.expandedState);
    }

    showAppInfoDialog() {
        this.dialog.open(AppInfoDialogComponent, { width: "360px", panelClass: "app-info-dialog-panel" });
    }
}
