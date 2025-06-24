/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component } from "@angular/core";
import { AsyncPipe } from "@angular/common";
import { MatSelectModule } from "@angular/material/select";
import { MatDividerModule } from "@angular/material/divider";
import { ButtonComponent } from "../../../../framework/button/button.component";
import { ModalComponent } from "../../../../framework/modal";
import { MatButtonModule } from "@angular/material/button";

@Component({
    selector: "tp-right-sidebar",
    templateUrl: "./right-sidebar.component.html",
    styleUrls: ["./right-sidebar.component.scss"],
    imports: [MatSelectModule, MatDividerModule, MatButtonModule, ButtonComponent]
})
export class RightSidebarComponent {
    constructor() {}
}
