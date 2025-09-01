/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDialog } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSortModule } from "@angular/material/sort";
import { MatTooltipModule } from "@angular/material/tooltip";
import { of, switchMap } from "rxjs";
import { isApiErrorResponse } from "typedb-driver-http";
import { SpinnerComponent } from "../../framework/spinner/spinner.component";
import { DriverState } from "../../service/driver-state.service";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { UserCreateDialogComponent } from "./create-dialog/user-create-dialog.component";
import { UsersTableComponent } from "./table/users-table.component";

@Component({
    selector: "ts-users-page",
    templateUrl: "users-page.component.html",
    styleUrls: ["users-page.component.scss"],
    imports: [
        AsyncPipe, PageScaffoldComponent, MatDividerModule, MatFormFieldModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule,
        MatSortModule, MatTooltipModule, MatButtonModule, UsersTableComponent, SpinnerComponent,
    ]
})
export class UsersPageComponent {

    users$ = this.driver.userList$;
    errorLines: string[] | null = null;

    constructor(private driver: DriverState, private dialog: MatDialog) {
        this.driver.connection$.pipe(
            switchMap((connection) => {
                if (connection) return this.driver.refreshUserList();
                else return of(null);
            })
        ).subscribe({
            next: (res) => {
                if (res == null) {
                    this.errorLines = [`This page requires a connection to a TypeDB server.`, `Please connect to TypeDB and try again.`];
                } else if (isApiErrorResponse(res)) {
                    this.errorLines = res.err.message.split(`\n`);
                } else {
                    this.errorLines = null;
                }
            },
            error: (err) => {
                let msg = ``;
                if (isApiErrorResponse(err)) {
                    msg = err.err.message;
                } else {
                    msg = err?.message ?? err?.toString() ?? `Unknown error`;
                }
                this.errorLines = msg.split(`\n`);
            },
        });
    }

    openCreateUserDialog() {
        this.dialog.open(UserCreateDialogComponent);
    }
}
