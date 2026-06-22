/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component } from "@angular/core";
import { Router } from "@angular/router";
import { MatDividerModule } from "@angular/material/divider";
import { PersistedSavedQuery } from "../../service/app-data.service";
import { QueryPageState } from "../../service/query-page-state.service";
import { QueryTabsState } from "../../service/query-tabs-state.service";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { SavedQueriesPaneComponent } from "./saved-queries-pane/saved-queries-pane.component";

@Component({
    selector: "ts-saved-queries-page",
    templateUrl: "./saved-queries-page.component.html",
    styleUrls: ["./saved-queries-page.component.scss"],
    imports: [MatDividerModule, PageScaffoldComponent, SavedQueriesPaneComponent],
})
export class SavedQueriesPageComponent {
    constructor(
        public queryPageState: QueryPageState,
        private queryTabsState: QueryTabsState,
        private router: Router,
    ) {}

    onRunSavedQuery(entry: PersistedSavedQuery) {
        this.queryPageState.runQuery(entry.query);
        this.router.navigate(["/query"]);
    }

    /**
     * Open the saved query in the query page without auto-running: spin up
     * a new tab named after the saved entry and pre-populate its editor.
     * Navigate first so the new tab lands on the visible page.
     */
    onOpenSavedQuery(entry: PersistedSavedQuery) {
        this.router.navigate(["/query"]).then(() => {
            const tab = this.queryTabsState.newTab();
            this.queryTabsState.renameTab(tab, entry.name);
            this.queryTabsState.getTabControl(tab).setValue(entry.query);
        });
    }
}
