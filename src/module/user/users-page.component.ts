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
import { RouterLink } from "@angular/router";
import { isApiErrorResponse } from "../../../../typedb-driver/http-ts/src";
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
        this.driver.refreshUserList().subscribe({
            next: (res) => {
                this.errorLines = isApiErrorResponse(res) ? res.err.message.split(`\n`) : null;
            },
            error: (err) => {
                this.errorLines = err?.message?.split(`\n`) ?? err?.toString()?.split(`\n`);
            },
        });
    }

    openCreateUserDialog() {
        this.dialog.open(UserCreateDialogComponent);
    }
}
