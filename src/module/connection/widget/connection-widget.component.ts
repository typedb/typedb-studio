/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, HostBinding, HostListener } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Router } from "@angular/router";
import { combineLatest, first, map } from "rxjs";
import { DriverState, DriverStatus } from "../../../service/driver-state.service";
import { TransactionWidgetComponent } from "../../transaction/widget/transaction-widget.component";

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
    imports: [MatTooltipModule, AsyncPipe, TransactionWidgetComponent],
})
export class ConnectionWidgetComponent {

    connectionText$ = this.driver.connection$.pipe(map(x => x?.name ?? `No connection selected`));
    connectionBeaconClass$ = this.driver.status$.pipe(map(status => `ts-beacon fa-solid fa-circle ${statusStyleMap[status]}`));

    databaseWidgetVisible$ = this.driver.status$.pipe(map((status) => ["connected", "reconnecting"].includes(status)));
    databaseText$ = this.driver.database$.pipe(map(db => db?.name ?? `No database selected`));

    transactionWidgetVisible$ = this.driver.database$.pipe(map(db => !!db));
    transactionText$ = this.driver.transaction$.pipe(map(tx => tx?.type ?? `No active transaction`));
    transactionHasUncommittedChanges$ = this.driver.transaction$.pipe(map(tx => tx?.hasUncommittedChanges ?? false));
    transactionWidgetTooltip$ = this.transactionHasUncommittedChanges$.pipe(map(x => x ? `Has uncommitted changes` : ``));

    constructor(private driver: DriverState, private router: Router) {}

    onClick() {
        this.driver.connection$.pipe(first()).subscribe((connection) => {
            if (!connection) {
                this.router.navigate(["/connections/new"]);
            }
        });
    }
}
