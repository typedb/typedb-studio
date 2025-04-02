/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";
import { MatDialogContent, MatDialogTitle } from "@angular/material/dialog";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { ModalCloseButtonComponent } from "./close-button/modal-close-button.component";

export type DialogResult = "ok" | "cancelled" | { error: string };

@Component({
    selector: "tp-modal",
    templateUrl: "modal.component.html",
    styleUrls: ["./modal.component.scss"],
    standalone: true,
    imports: [MatDialogTitle, ModalCloseButtonComponent, MatDialogContent, MatProgressBarModule],
})
export class ModalComponent {
    @Input() isBusy?: boolean | null;
    @Input() dialogTitle!: string;
}
