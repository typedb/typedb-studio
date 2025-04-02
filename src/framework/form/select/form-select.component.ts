/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";

export type FormOption<VALUE> = { value: VALUE, viewValue?: string, tooltip?: string };
export type FormOptionGroup<VALUE> = { name: string, options: FormOption<VALUE>[] };

@Component({
    selector: "tp-form-select",
    templateUrl: "./form-select.component.html",
    styleUrls: ["./form-select.component.scss"],
    standalone: true,
    imports: [MatFormFieldModule, MatSelectModule, FormsModule, ReactiveFormsModule, MatTooltipModule],
})
export class FormSelectComponent<VALUE, FORM extends { [K in keyof FORM & string]: AbstractControl; } & { [key: string]: AbstractControl }> implements OnInit {
    @Input() label = "";
    @Input({ required: true }) form!: FormGroup<FORM>;
    @Input({ required: true }) field!: string;
    @Input({ required: true }) options: FormOption<VALUE>[] = [];

    formControl!: FormControl;

    constructor() {
    }

    ngOnInit() {
        this.formControl = this.form.controls[this.field] as FormControl;
    }

    get title(): string {
        return this.options.find(x => x.value === this.formControl.value)?.viewValue ?? "Select an option";
    }
}
