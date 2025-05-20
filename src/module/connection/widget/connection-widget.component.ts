/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { CdkMenuItemCheckbox } from "@angular/cdk/menu";
import { AsyncPipe, NgClass } from "@angular/common";
import { Component, Input, OnInit, ViewChild } from "@angular/core";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatDialog } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Router, RouterLink } from "@angular/router";
import { combineLatest, distinctUntilChanged, first, map } from "rxjs";
import { HoverMenuComponent } from "../../../framework/menu/hover-menu.component";
import { Database } from "../../../framework/typedb-driver/database";
import { DriverState, DriverStatus } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { TransactionWidgetComponent } from "../../transaction/widget/transaction-widget.component";
import { DatabaseCreateDialogComponent } from "../database-create-dialog/database-create-dialog.component";

const statusStyleMap: { [K in DriverStatus]: string } = {
    disconnected: "error",
    connecting: "warn",
    connected: "ok",
    reconnecting: "warn",
};

@Component({
    selector: "ts-connection-widget",
    templateUrl: "./connection-widget.component.html",
    styleUrls: ["./connection-widget.component.scss"],
    standalone: true,
    imports: [
        MatTooltipModule, AsyncPipe, TransactionWidgetComponent, MatMenuModule, MatDividerModule,
        RouterLink, NgClass, MatCheckboxModule
    ],
})
export class ConnectionWidgetComponent implements OnInit {

    @Input({ required: true }) condensed!: boolean;
    @ViewChild(MatMenuTrigger) connectionMenuTrigger!: MatMenuTrigger;

    connectionText$ = this.driver.connection$.pipe(map(x => x?.name ?? `No server connected`));
    connectionBeaconStatusClass$ = combineLatest([this.driver.status$, this.driver.database$]).pipe(map(([status, database]) => {
        if (status === "disconnected") return "error";
        if (status === "connected") return database == null ? "error" : "ok";
        return "warn";
    }));
    connectionBeaconTooltip$ = combineLatest([this.driver.status$, this.driver.database$]).pipe(map(([status, database]) => {
        if (status === "connected") return database == null ? "" : "Connected";
        return `${status[0].toUpperCase()}${status.substring(1)}`;
    }));
    private transactionWidgetVisible = false;
    connectionText = ``;
    connectionAreaHovered = false;
    connectionMenuHovered = false;

    databaseVisible$ = this.driver.status$.pipe(map((status) => ["connected", "reconnecting"].includes(status)));
    databaseText$ = this.driver.database$.pipe(map(db => db?.name ?? `No database selected`));

    transactionWidgetVisible$ = this.driver.database$.pipe(map(db => !!db), distinctUntilChanged());
    transactionText$ = this.driver.transaction$.pipe(map(tx => tx?.type ?? `No active transaction`));
    transactionWidgetTooltip$ = this.driver.transactionHasUncommittedChanges$.pipe(map(x => x ? `Has uncommitted changes` : ``));

    rootNgClass$ = combineLatest([this.driver.status$, this.transactionWidgetVisible$]).pipe(map(([status, txWidgetVisible]) => ({
        "root": true,
        "has-transaction-widget": txWidgetVisible,
        "hoverable": status !== "disconnected"
    })));

    constructor(
        public driver: DriverState, private router: Router, private snackbar: SnackbarService,
        private dialog: MatDialog
    ) {}

    ngOnInit() {
        this.connectionText$.subscribe((text) =>{
            this.connectionText = text;
        });
        this.transactionWidgetVisible$.subscribe((visible) => {
            this.transactionWidgetVisible = visible;
        });
    }

    get connectionTooltip() {
        return this.condensed && this.transactionWidgetVisible ? this.connectionText : ``;
    }

    onClick() {
        this.driver.connection$.pipe(first()).subscribe((connection) => {
            if (!connection) {
                this.router.navigate(["/connect"]);
            }
        });
    }

    disconnect() {
        this.driver.tryDisconnect().subscribe(() => {
            this.snackbar.info(`Disconnected`);
        });
    }

    selectDatabase(database: Database) {
        this.driver.selectDatabase(database);
        this.snackbar.info(`Now using database '${database.name}'`);
    }

    openCreateDatabaseDialog() {
        this.dialog.open(DatabaseCreateDialogComponent);
    }

    refreshDatabaseList() {
        this.driver.refreshDatabaseList().subscribe(() => {
            this.snackbar.success(`Database list refreshed`);
        });
    }

    onConnectionAreaMouseEnter() {
        this.connectionAreaHovered = true;
    }

    onConnectionAreaMouseLeave() {
        this.connectionAreaHovered = false;
    }

    onConnectionMenuMouseEnter() {
        this.connectionMenuHovered = true;
    }

    onConnectionMenuMouseLeave() {}
}
