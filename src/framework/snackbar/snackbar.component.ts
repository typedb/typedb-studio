/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, HostBinding, Inject } from "@angular/core";
import { ColorStyle } from "../util";
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from "@angular/material/snack-bar";

export interface SnackbarData {
    message: string;
    status: ColorStyle;
}

const statusIcons: Record<ColorStyle, string> = {
    error: "fa-circle-xmark",
    inactive: "fa-circle-info",
    info: "fa-circle-info",
    ok: "fa-circle-check",
    warn: "fa-circle-exclamation",
};

@Component({
    selector: "tp-snackbar",
    templateUrl: "snackbar.component.html",
    styleUrls: ["snackbar.component.scss"],
    standalone: true,
})
export class SnackbarComponent {
    readonly message: string;
    @HostBinding("class") readonly status: ColorStyle;

    constructor(private matSnackBarRef: MatSnackBarRef<SnackbarComponent>, @Inject(MAT_SNACK_BAR_DATA) data: SnackbarData) {
        this.message = data.message;
        this.status = data.status;
    }

    close(): void {
        this.matSnackBarRef.dismiss();
    }

    get icon(): string {
        return statusIcons[this.status];
    }
}
