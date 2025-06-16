/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild } from "@angular/core";
import { AbstractControl, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";

@Component({
    selector: "tp-form-textarea",
    templateUrl: "./form-textarea.component.html",
    styleUrls: ["./form-textarea.component.scss"],
    imports: [MatFormFieldModule, MatInputModule, FormsModule, ReactiveFormsModule]
})
export class FormTextareaComponent<FORM extends { [K in keyof FORM & string]: AbstractControl; } & { [key: string]: AbstractControl }> implements OnInit, AfterViewInit {
    @Input() label = "";
    @Input({ required: true }) form!: FormGroup<FORM>;
    @Input({ required: true }) field!: string;
    @Input() hint = "";
    @Input() autocomplete?: string;
    @ViewChild("textarea") textareaEl!: ElementRef<HTMLTextAreaElement>;

    formControl!: FormControl;

    constructor(private hostEl: ElementRef<HTMLElement>) {
    }

    ngOnInit() {
        this.formControl = this.form.controls[this.field] as FormControl;
    }

    ngAfterViewInit() {
        [...this.hostEl.nativeElement.attributes]
            .filter(attr => !attr.nodeName.startsWith("_ng") && !attr.nodeName.startsWith("ng"))
            .forEach(attr => {
                if (attr.nodeValue != null) this.textareaEl.nativeElement.setAttribute(attr.nodeName, attr.nodeValue);
            });
    }
}
