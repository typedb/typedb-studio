/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
