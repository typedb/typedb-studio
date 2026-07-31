/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component } from "@angular/core";
import { MatDividerModule } from "@angular/material/divider";
import { QueryPageState } from "../../service/query-page-state.service";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { HistoryPaneComponent } from "./history-pane/history-pane.component";

@Component({
    selector: "ts-query-history-page",
    templateUrl: "./query-history-page.component.html",
    styleUrls: ["./query-history-page.component.scss"],
    imports: [MatDividerModule, PageScaffoldComponent, HistoryPaneComponent],
})
export class QueryHistoryPageComponent {
    constructor(public queryPageState: QueryPageState) {}
}
