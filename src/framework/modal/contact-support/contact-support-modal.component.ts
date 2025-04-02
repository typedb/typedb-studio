/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
