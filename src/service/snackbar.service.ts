/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { MatSnackBar, MatSnackBarConfig } from "@angular/material/snack-bar";
import { SnackbarComponent, SnackbarData } from "../framework/snackbar/snackbar.component";
import { ColorStyle } from "../framework/util";

@Injectable({
    providedIn: "root",
})
export class SnackbarService {
    constructor(private snackbar: MatSnackBar) {}

    open(message: string, status: ColorStyle, config?: MatSnackBarConfig<SnackbarData>) {
        const defaultConfig: MatSnackBarConfig<SnackbarData> = {
            data: { message, status },
            duration: undefined,
            horizontalPosition: "center",
            verticalPosition: "bottom",
        };
        return this.snackbar.openFromComponent<SnackbarComponent, SnackbarData>(SnackbarComponent, Object.assign(defaultConfig, config));
    }

    success(message: string, config?: MatSnackBarConfig<SnackbarData>) {
        return this.open(message, "ok", Object.assign({ duration: 4000 }, config));
    }

    info(message: string, config?: MatSnackBarConfig<SnackbarData>) {
        return this.open(message, "info", Object.assign({ duration: 10000 }, config));
    }

    infoPersistent(message: string, config?: MatSnackBarConfig<SnackbarData>) {
        return this.open(message, "info", config);
    }

    warn(message: string, config?: MatSnackBarConfig<SnackbarData>) {
        return this.open(message, "warn", Object.assign({ duration: 10000 }, config));
    }

    warnPersistent(message: string, config?: MatSnackBarConfig<SnackbarData>) {
        return this.open(message, "warn", config);
    }

    errorPersistent(message: string, config?: MatSnackBarConfig<SnackbarData>) {
        return this.open(message, "error", config);
    }
}
