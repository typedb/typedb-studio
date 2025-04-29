/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { RouterLink } from "@angular/router";
import { BehaviorSubject, map } from "rxjs";
import { ButtonComponent } from "../../framework/button/button.component";
import { DriverState } from "../../service/driver-state.service";
import { PageScaffoldComponent, ResourceAvailability } from "../scaffold/page/page-scaffold.component";

@Component({
    selector: "ts-home",
    templateUrl: "home.component.html",
    styleUrls: ["home.component.scss"],
    standalone: true,
    imports: [ButtonComponent, RouterLink, AsyncPipe, PageScaffoldComponent],
})
export class HomeComponent implements OnInit {
    availability$ = new BehaviorSubject<ResourceAvailability>("ready");
    isConnected$ = this.driver.status$.pipe(map(x => x === "connected"));

    constructor(private driver: DriverState) {
    }

    ngOnInit() {

    }
}
