/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { HttpClient } from "@angular/common/http";
import { Component, OnInit } from "@angular/core";
import { RouterLink } from "@angular/router";
import { BehaviorSubject, map, Subject } from "rxjs";
import { ButtonComponent } from "../../framework/button/button.component";
import { AppDataService } from "../../service/app-data.service";
import { DriverStateService } from "../../service/driver-state.service";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";

@Component({
    selector: "ts-query-page",
    templateUrl: "query-page.component.html",
    styleUrls: ["query-page.component.scss"],
    standalone: true,
    imports: [ButtonComponent, RouterLink, AsyncPipe, PageScaffoldComponent],
})
export class QueryPageComponent implements OnInit {
    transaction$ = new BehaviorSubject<string | null>(null);
    canOpenTransaction$ = this.transaction$.pipe(map(x => x == null));
    canCloseTransaction$ = this.transaction$.pipe(map(x => x != null));
    canCommitTransaction$ = new BehaviorSubject(false);

    constructor(private driver: DriverStateService, private appData: AppDataService) {
    }

    ngOnInit() {
        this.appData.viewState.setLastUsedTool("query");
    }

    openTransaction() {

    }

    closeTransaction() {

    }

    commitTransaction() {

    }
}
