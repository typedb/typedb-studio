/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, OnInit } from "@angular/core";
import { AsyncPipe, NgClass } from "@angular/common";
import { MatTabsModule } from "@angular/material/tabs";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatDialog } from "@angular/material/dialog";
import { RouterLink } from "@angular/router";
import { ResizableDirective } from "@hhangular/resizable";
import { SchemaToolWindowComponent } from "../schema/tool-window/schema-tool-window.component";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { DriverState } from "../../service/driver-state.service";
import { DatabaseCreateDialogComponent } from "../database/create-dialog/database-create-dialog.component";
import { DatabaseSelectDialogComponent } from "../database/select-dialog/database-select-dialog.component";
import { AppData } from "../../service/app-data.service";
import { GraphViewState, GraphViewTab } from "../../service/graph-view-state.service";
import { GraphTabComponent } from "./graph-tab/graph-tab.component";

@Component({
    selector: "ts-graph-page",
    templateUrl: "./graph-page.component.html",
    styleUrls: ["./graph-page.component.scss"],
    imports: [
        AsyncPipe,
        NgClass,
        RouterLink,
        MatTabsModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        ResizableDirective,
        PageScaffoldComponent,
        SchemaToolWindowComponent,
        GraphTabComponent,
    ],
})
export class GraphPageComponent implements OnInit {

    private static readonly DEFAULT_PANEL_SIZES = [20, 80];
    panelSizes = [...GraphPageComponent.DEFAULT_PANEL_SIZES];

    constructor(
        public state: GraphViewState,
        public driver: DriverState,
        private dialog: MatDialog,
        private appData: AppData,
    ) {}

    ngOnInit() {
        this.appData.viewState.setLastUsedTool("graph");
        const saved = this.appData.panelLayout.get("graph");
        if (saved && saved.length === GraphPageComponent.DEFAULT_PANEL_SIZES.length) {
            this.panelSizes = saved;
        }
    }

    onPanelResize(index: number, percent: number) {
        this.panelSizes[index] = percent;
        this.appData.panelLayout.set("graph", [...this.panelSizes]);
    }

    openSelectDatabaseDialog() {
        this.dialog.open(DatabaseSelectDialogComponent);
    }

    openCreateDatabaseDialog() {
        this.dialog.open(DatabaseCreateDialogComponent);
    }

    onTabChange(index: number) {
        this.state.selectedTabIndex$.next(index);
    }

    closeTab(event: Event, tabIndex: number) {
        event.stopPropagation();
        const tab = this.state.openTabs$.value[tabIndex];
        if (tab) this.state.closeTab(tab);
    }

    getTabTrackId(tab: GraphViewTab): string {
        return `type:${tab.type.label}:${tab.attrsOnly ? "attrs" : "all"}`;
    }

    getTypeIconClass(typeKind: string): string {
        switch (typeKind) {
            case "entityType": return "fa-solid fa-square entity";
            case "relationType": return "fa-solid fa-diamond relation";
            case "attributeType": return "fa-solid fa-circle attribute";
            default: return "fa-solid fa-question";
        }
    }
}
