/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe, NgClass } from "@angular/common";
import { Component, Input, OnInit, ViewChild } from "@angular/core";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatDialog } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { Database } from "@typedb/driver-http";
import { distinctUntilChanged, map } from "rxjs";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { DatabaseCreateDialogComponent } from "../../database/create-dialog/database-create-dialog.component";
import { DatabaseDeleteDialogComponent } from "../../database/delete-dialog/database-delete-dialog.component";
import { SampleDatasetDialogComponent } from "../../database/sample-dataset-dialog/sample-dataset-dialog.component";
import { TransactionControlComponent } from "../transaction/transaction-control.component";

@Component({
    selector: "ts-connection-widget",
    templateUrl: "./connection-widget.component.html",
    styleUrls: ["./connection-widget.component.scss"],
    imports: [
        MatTooltipModule, AsyncPipe, MatMenuModule, MatDividerModule,
        RouterLink, NgClass, MatCheckboxModule, TransactionControlComponent
    ]
})
export class ConnectionWidgetComponent implements OnInit {

    @Input({ required: true }) condensed!: boolean;
    @ViewChild("connectionMenuTrigger") connectionMenuTrigger!: MatMenuTrigger;
    @ViewChild("databaseMenuTrigger") databaseMenuTrigger!: MatMenuTrigger;

    connectionText$ = this.driver.connection$.pipe(map(x => x == null ? `No server connected` : `${x.params.username}@${x.name}`));
    connectionBeaconStatusClass$ = this.driver.status$.pipe(map((status) => {
        if (status === "disconnected") return "error";
        else if (status === "connected") return "ok";
        return "warn";
    }));
    connectionBeaconTooltip$ = this.driver.status$.pipe(map((status) => `${status[0].toUpperCase()}${status.substring(1)}`));
    private transactionControlVisible = false;
    connectionText = ``;
    connectionAreaHovered = false;
    connectionMenuHovered = false;

    databaseVisible$ = this.driver.status$.pipe(map((status) => ["connected", "reconnecting"].includes(status)));
    databaseText$ = this.driver.database$.pipe(map(db => db?.name ?? `No database selected`));

    transactionControlVisible$ = this.driver.database$.pipe(map(db => !!db), distinctUntilChanged());
    transactionText$ = this.driver.transaction$.pipe(map(tx => tx?.type ?? `No active transaction`));
    transactionWidgetTooltip$ = this.driver.transactionHasUncommittedChanges$.pipe(map(x => x ? `Has uncommitted changes` : ``));

    rootNgClass$ = this.driver.status$.pipe(map((status) => ({
        "root": true,
        "hoverable": status !== "disconnected"
    })));

    constructor(
        public driver: DriverState, private snackbar: SnackbarService,
        private dialog: MatDialog
    ) {}

    ngOnInit() {
        this.connectionText$.subscribe((text) =>{
            this.connectionText = text;
        });
        this.transactionControlVisible$.subscribe((visible) => {
            this.transactionControlVisible = visible;
        });
    }

    get connectionTooltip() {
        return this.condensed && this.transactionControlVisible ? this.connectionText : ``;
    }

    signOut() {
        this.driver.tryDisconnect().subscribe(() => {
            window.location.href = "/connect";
        });
    }

    selectDatabase(database: Database) {
        if (this.driver.database$.value?.name === database.name) return;
        this.driver.selectDatabase(database);
        this.snackbar.info(`Now using database '${database.name}'`);
    }

    onDeleteDatabaseClick(database: Database, e: Event) {
        e.stopPropagation();
        this.databaseMenuTrigger.closeMenu();
        this.dialog.open(DatabaseDeleteDialogComponent, { data: { db: database } });
    }

    openCreateDatabaseDialog() {
        this.dialog.open(DatabaseCreateDialogComponent);
    }

    openLoadSampleDatasetDialog() {
        this.dialog.open(SampleDatasetDialogComponent);
    }

    refreshingDatabaseList = false;

    private truncationState = new WeakMap<HTMLElement, boolean>();

    checkTruncation(el: HTMLElement) {
        this.truncationState.set(el, el.scrollWidth > el.clientWidth);
    }

    isTruncated(el: HTMLElement): boolean {
        return this.truncationState.get(el) ?? false;
    }

    refreshDatabaseList() {
        if (this.refreshingDatabaseList) return;
        this.refreshingDatabaseList = true;
        this.driver.refreshDatabaseList().subscribe({
            next: () => this.snackbar.success(`Database list refreshed`),
            complete: () => { this.refreshingDatabaseList = false; },
            error: () => { this.refreshingDatabaseList = false; },
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
