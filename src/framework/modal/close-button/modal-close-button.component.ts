/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
