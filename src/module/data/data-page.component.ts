/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component } from "@angular/core";
import { AsyncPipe } from "@angular/common";
import { MatTabsModule } from "@angular/material/tabs";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ResizableDirective } from "@hhangular/resizable";
import { SchemaToolWindowComponent } from "../schema/tool-window/schema-tool-window.component";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { DataEditorState, DataTab } from "../../service/data-editor-state.service";
import { SchemaToolWindowState } from "../../service/schema-tool-window-state.service";
import { InstanceTableComponent } from "./instance-table/instance-table.component";
import { InstanceDetailComponent } from "./instance-detail/instance-detail.component";

@Component({
    selector: "ts-data-page",
    templateUrl: "./data-page.component.html",
    styleUrls: ["./data-page.component.scss"],
    imports: [
        AsyncPipe,
        MatTabsModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        ResizableDirective,
        PageScaffoldComponent,
        SchemaToolWindowComponent,
        InstanceTableComponent,
        InstanceDetailComponent,
    ],
})
export class DataPageComponent {
    constructor(
        public state: DataEditorState,
        public schemaToolWindowState: SchemaToolWindowState,
    ) {}

    closeTab(event: Event, tabIndex: number) {
        event.stopPropagation();
        const tab = this.state.openTabs$.value[tabIndex];
        if (tab) {
            this.state.closeTab(tab);
        }
    }

    getTabTrackId(tab: DataTab): string {
        if (tab.kind === "type-table") {
            return `type:${tab.type.label}`;
        } else {
            return `instance:${tab.instanceIID}`;
        }
    }
}
