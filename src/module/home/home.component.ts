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
import { PageScaffoldComponent, ResourceAvailability } from "../scaffold/page/page-scaffold.component";

@Component({
    selector: "ts-home",
    templateUrl: "home.component.html",
    styleUrls: ["home.component.scss"],
    standalone: true,
    imports: [ButtonComponent, RouterLink, AsyncPipe, PageScaffoldComponent],
})
export class HomeComponent implements OnInit {
    data$ = new Subject<string>();
    availability$ = new BehaviorSubject<ResourceAvailability>("ready");
    hasConnections$ = new BehaviorSubject(false);

    constructor(private http: HttpClient) {
    }

    ngOnInit() {

    }

    showAGraph() {
        this.http.get(`https://raw.githubusercontent.com/alexjpwalker/typedb-studio-temp/refs/heads/master/graph.json`).pipe(map(x => JSON.stringify(x)))
            .subscribe(x => this.data$.next(x));
    }
}
