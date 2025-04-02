/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
