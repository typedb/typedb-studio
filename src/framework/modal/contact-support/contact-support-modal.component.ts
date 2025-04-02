/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { ButtonComponent } from "../../button/button.component";
import { ModalComponent } from "../modal.component";

export interface ContactSupportDialogData {
    title: string;
}

@Component({
    selector: "tp-contact-support-modal",
    templateUrl: "./contact-support-modal.component.html",
    styleUrls: ["./contact-support-modal.component.scss"],
    standalone: true,
    imports: [ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, ButtonComponent],
})
export class ContactSupportModalComponent {
    readonly emailAddress = "support@typedb.com";

    constructor(
        public dialogRef: MatDialogRef<ContactSupportModalComponent, ContactSupportDialogData>,
        @Inject(MAT_DIALOG_DATA) public data: ContactSupportDialogData
    ) {}
}
