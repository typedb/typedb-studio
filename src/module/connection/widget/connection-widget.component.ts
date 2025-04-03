/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ConnectionStatus } from "../../../concept/connection";
import { ConnectionStateService } from "../../../service/connection-state.service";

const statusStyleMap: { [K in ConnectionStatus]: string } = {
    connected: "ok",
    connecting: "warn",
    disconnected: "error",
};

@Component({
    selector: "ts-connection-widget",
    templateUrl: "./connection-widget.component.html",
    styleUrls: ["./connection-widget.component.scss"],
    standalone: true,
    imports: [MatTooltipModule],
})
export class ConnectionWidgetComponent {

    constructor(private connection: ConnectionStateService) {}

    get connectionName(): string {
        return this.connection.config?.name ?? `No connection selected`;
    }

    get beaconClass(): string {
        return `ts-beacon fa-solid fa-circle ${statusStyleMap[this.connection.status]}`;
    }
}
