/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { version } from "../../../package.json";
import { AsyncPipe } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { ModalComponent, ModalCloseButtonComponent } from "../../framework/modal";
import { FormActionsComponent, FormComponent, FormInputComponent } from "../../framework/form";
import { ButtonComponent } from "../../framework/button/button.component";
import { AnalyticsService } from "../../service/analytics.service";
import { SnackbarService } from "../../service/snackbar.service";

@Component({
    selector: "ts-app-info-dialog",
    templateUrl: "./app-info-dialog.component.html",
    styleUrls: ["./app-info-dialog.component.scss"],
    imports: [FormsModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, FormActionsComponent]
})
export class AppInfoDialogComponent {
    clickCount = 0;
    easterEggState: "none" | "wobble" | "wobble-more" | "pre-explode" | "explode" = "none";

    constructor(
        private dialogRef: MatDialogRef<AppInfoDialogComponent>,
        private analytics: AnalyticsService,
    ) {
    }

    onLogoClick() {
        this.clickCount++;
        if (this.easterEggState === "pre-explode" || this.easterEggState === "explode") {
            return;
        } else if (this.clickCount >= 30) {
            this.easterEggState = "pre-explode";
            setTimeout(() => {
                this.easterEggState = "explode";
                this.analytics.posthog.capture("typedb bot exploded");
            }, 800);
        } else if (this.clickCount >= 20) {
            this.easterEggState = "wobble-more";
        } else if (this.clickCount >= 10) {
            this.easterEggState = "wobble";
        }
    }

    close() {
        this.dialogRef.close();
    }

    get version() {
        return version;
    }
}
