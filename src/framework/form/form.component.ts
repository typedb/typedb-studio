/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from "@angular/core";
import { AbstractControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { filter, Subject, Subscription } from "rxjs";

@Component({
    selector: "tp-form",
    templateUrl: "form.component.html",
    imports: [ReactiveFormsModule]
})
export class FormComponent<CONTROLS extends { [K in keyof CONTROLS]: AbstractControl<any, any>; }> implements OnInit, OnDestroy {
    @Input({ required: true }) formGroup!: FormGroup<CONTROLS>;
    @Input({ required: true }) isSubmitting$!: Subject<boolean>;
    @Output() doSubmit = new EventEmitter<void>();
    private submittingSub!: Subscription;
    private previouslyDisabledControls: string[] = [];

    ngOnInit() {
        this.submittingSub = this.isSubmitting$.subscribe((isSubmitting) => {
            if (!isSubmitting && this.formGroup.disabled) {
                this.formGroup.enable({emitEvent: false, onlySelf: true});
                this.previouslyDisabledControls.forEach((control) => {
                    this.formGroup.get(control)?.disable();
                });
                this.previouslyDisabledControls = [];
            }
            else if (isSubmitting && this.formGroup.enabled) {
                this.previouslyDisabledControls = Object.keys(this.formGroup.controls).filter((control) => this.formGroup.get(control)?.disabled);
                this.formGroup.disable({emitEvent: false})
            }
        });
    }

    ngOnDestroy() {
        this.submittingSub.unsubscribe();
    }

    get value() {
        return this.formGroup.value;
    }

    submit() {
        if (!this.formGroup.valid) {
            this.formGroup.markAllAsTouched();
            return;
        }
        this.isSubmitting$.next(true);
        this.doSubmit.emit();
    }
}
