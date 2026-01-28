/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, HostBinding, Inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { ColorStyle } from "../util";
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from "@angular/material/snack-bar";

export interface SnackbarAction {
    label: string;
    callback: () => void;
}

export interface SnackbarData {
    message: string;
    status: ColorStyle;
    action?: SnackbarAction;
    /** Maximum lines to show before truncating (default: no limit) */
    maxLines?: number;
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
    imports: [MatButtonModule],
})
export class SnackbarComponent {
    readonly message: string;
    readonly displayLines: string[];
    readonly isTruncated: boolean;
    readonly action?: SnackbarAction;
    @HostBinding("class") readonly status: ColorStyle;

    constructor(private matSnackBarRef: MatSnackBarRef<SnackbarComponent>, @Inject(MAT_SNACK_BAR_DATA) data: SnackbarData) {
        this.message = data.message;
        this.status = data.status;
        this.action = data.action;

        // Filter out empty lines and limit to maxLines if specified
        const allLines = data.message.split(`\n`).filter(line => line.trim().length > 0);
        if (data.maxLines && allLines.length > data.maxLines) {
            this.displayLines = allLines.slice(0, data.maxLines);
            this.isTruncated = true;
        } else {
            this.displayLines = allLines;
            this.isTruncated = false;
        }
    }

    close(): void {
        this.matSnackBarRef.dismiss();
    }

    onAction(): void {
        this.action?.callback();
        this.matSnackBarRef.dismiss();
    }

    get icon(): string {
        return statusIcons[this.status];
    }
}
