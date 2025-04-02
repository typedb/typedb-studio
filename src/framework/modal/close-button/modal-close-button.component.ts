/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDialogClose } from "@angular/material/dialog";

@Component({
    selector: "tp-modal-close-button",
    templateUrl: "modal-close-button.component.html",
    styleUrls: ["./modal-close-button.component.scss"],
    standalone: true,
    imports: [MatButtonModule, MatDialogClose],
})
export class ModalCloseButtonComponent {}
