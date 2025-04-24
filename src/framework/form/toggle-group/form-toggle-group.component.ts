/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatFormFieldModule } from "@angular/material/form-field";
import { FormOption } from "../select/form-select.component";

@Component({
    selector: "tp-form-toggle-group",
    templateUrl: "./form-toggle-group.component.html",
    styleUrls: ["./form-toggle-group.component.scss"],
    standalone: true,
    imports: [MatFormFieldModule, MatButtonToggleModule, FormsModule, ReactiveFormsModule],
})
export class FormToggleGroupComponent<VALUE, FORM extends { [K in keyof FORM & string]: AbstractControl; } & { [key: string]: AbstractControl }> implements OnInit {
    @Input() form?: FormGroup<FORM>;
    @Input() field?: string;
    @Input() formControl!: FormControl;
    @Input({ required: true }) options: FormOption<VALUE>[] = [];
    @Input() disabled?: boolean;

    constructor() {
    }

    ngOnInit() {
        if (!this.formControl) this.formControl = this.form!.controls[this.field!] as FormControl;
    }
}
