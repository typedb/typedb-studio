/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ButtonComponent, ButtonStyle } from "../../button/button.component";
import { SpinnerComponent } from "../../spinner/spinner.component";

@Component({
    selector: "tp-form-actions",
    templateUrl: "form-actions.component.html",
    standalone: true,
    imports: [ButtonComponent, SpinnerComponent]
})
export class FormActionsComponent {
    @Input() submitText: string = "Submit";
    @Input() submitDisabled?: boolean | null;
    @Output() submitClick = new EventEmitter<void>();
    @Input() cancellable: boolean = false;
    @Input() cancelText: string = "Cancel";
    @Output() cancel = new EventEmitter<void>();
    @Input({ required: true }) isSubmitting?: boolean | null;
    @Input() cancelButtonStyle: ButtonStyle = "primary-outline white stroke";
    @Input() submitButtonStyle: ButtonStyle = "primary-outline green stroke";
    @Input({ required: true }) buttonIdPrefix!: string;

    get cancelEnabled(): boolean {
        return !this.isSubmitting;
    }

    get submitEnabled(): boolean {
        return !this.isSubmitting && !this.submitDisabled;
    }

    onCancel() {
        if (this.cancelEnabled) this.cancel.emit();
    }

    onSubmit() {
        if (this.submitEnabled) this.submitClick.emit();
    }
}
