/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, HostBinding } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { combineLatest, map } from "rxjs";
import { DriverStateService, DriverStatus } from "../../../service/driver-state.service";

const statusStyleMap: { [K in DriverStatus]: string } = {
    initial: "error",
    connecting: "warn",
    connected: "ok",
    reconnecting: "warn",
};

@Component({
    selector: "ts-connection-widget",
    templateUrl: "./connection-widget.component.html",
    styleUrls: ["./connection-widget.component.scss"],
    standalone: true,
    imports: [MatTooltipModule, AsyncPipe],
})
export class ConnectionWidgetComponent {

    beaconClass$ = this.driver.status$.pipe(map(status => `ts-beacon fa-solid fa-circle ${statusStyleMap[status]}`));
    connectionName$ = this.driver.config$.pipe(map(x => x?.name ?? `No connection selected`));
    databaseWidgetVisible$ = this.driver.status$.pipe(map((status) => ["connected", "reconnecting"].includes(status)));
    databaseName$ = this.driver.database$.pipe(map(db => db?.name ?? `No database selected`));

    constructor(private driver: DriverStateService) {}
}
