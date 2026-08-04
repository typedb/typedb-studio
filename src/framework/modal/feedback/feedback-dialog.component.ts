/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, inject } from "@angular/core";
import { MatDialogRef } from "@angular/material/dialog";
import { ModalComponent } from "../modal.component";
import { ButtonComponent } from "../../button/button.component";

@Component({
    selector: "tp-feedback-dialog",
    templateUrl: "./feedback-dialog.component.html",
    styleUrls: ["./feedback-dialog.component.scss"],
    imports: [ModalComponent, ButtonComponent],
})
export class FeedbackDialogComponent {
    readonly dialogRef = inject(MatDialogRef<FeedbackDialogComponent>);
}
