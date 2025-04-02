/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
    standalone: true,
    imports: [
        ModalComponent, AsyncPipe, MatSelectModule,
        MatDividerModule, MatButtonModule, ButtonComponent
    ],
})
export class RightSidebarComponent {
    constructor() {}
}
