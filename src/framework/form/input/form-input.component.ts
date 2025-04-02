/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild } from "@angular/core";
import { AbstractControl, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";

@Component({
    selector: "tp-form-input",
    templateUrl: "./form-input.component.html",
    styleUrls: ["./form-input.component.scss"],
    standalone: true,
    imports: [MatFormFieldModule, MatInputModule, FormsModule, ReactiveFormsModule],
})
export class FormInputComponent<FORM extends { [K in keyof FORM & string]: AbstractControl; } & { [key: string]: AbstractControl }> implements OnInit, AfterViewInit {
    @Input() label = "";
    @Input({ required: true }) form!: FormGroup<FORM>;
    @Input({ required: true }) field!: string;
    @Input() hint = "";
    @Input() autocomplete?: string;
    @Input() placeholder?: string;
    @ViewChild("input") inputEl!: ElementRef<HTMLInputElement>;

    formControl!: FormControl;

    constructor(private hostEl: ElementRef<HTMLElement>) {
    }

    ngOnInit() {
        this.formControl = this.form.controls[this.field] as FormControl;
    }

    ngAfterViewInit() {
        [...this.hostEl.nativeElement.attributes]
            .filter(attr => !attr.nodeName.startsWith("_ng") && !attr.nodeName.startsWith("ng") && attr.nodeName != "class")
            .forEach(attr => {
                if (attr.nodeValue != null) this.inputEl.nativeElement.setAttribute(attr.nodeName, attr.nodeValue);
            });
    }
}
