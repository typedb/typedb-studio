/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { AsyncPipe } from "@angular/common";
import { Component, Input } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatTableModule } from "@angular/material/table";
import { MatSortModule } from "@angular/material/sort";
import { User } from "@samuel-butcher-typedb/typedb-http-driver";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatMenuModule } from "@angular/material/menu";
import { MatButtonModule } from "@angular/material/button";
import { UserChangePasswordDialogComponent } from "../change-password-dialog/user-change-password-dialog.component";

@Component({
    selector: "ts-users-table",
    templateUrl: "./users-table.component.html",
    styleUrls: ["./users-table.component.scss"],
    imports: [
        MatTableModule, MatSortModule, MatTooltipModule, MatMenuModule, MatButtonModule, AsyncPipe,
    ]
})
export class UsersTableComponent {
    @Input({ required: true}) table!: User[];
    columns = ["username", "actions-loud"];

    constructor(private dialog: MatDialog) {}

    trackByFn(_index: number, user: User): string {
        return user.username;
    }

    openChangePasswordDialog(user: User) {
        this.dialog.open(UserChangePasswordDialogComponent, {
            data: { username: user.username },
        });
    }

    openDeleteUserDialog(user: User) {
        throw new Error("Deleting users is bad and you should not do it");
    }
}
