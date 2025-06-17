/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, HostBinding, Input } from "@angular/core";
import { NgStyle, NgClass } from "@angular/common";
import { SpinnerComponent } from "../spinner/spinner.component";

export type ButtonStyle =
    | "primary-outline white stroke"
    | "primary-outline white grey-stroke"
    | "primary-outline green stroke"
    | "primary-outline red stroke"
    | "secondary stroke"
    | "secondary";

@Component({
    selector: "tp-button",
    templateUrl: "./button.component.html",
    styleUrls: ["./button.component.scss"],
    imports: [NgStyle]
})
export class ButtonComponent {
    @Input() buttonStyle: ButtonStyle = "secondary";
    @Input("height") _height?: number;
    @Input() enabled = true;
    @Input() type: HTMLButtonElement["type"] = "button";
    @Input() buttonClass?: string;
    @Input({ required: true }) buttonId!: string;

    constructor() {}

    get height() {
        return this._height || (this.buttonStyle === "secondary" ? 32 : 40);
    }

    get buttonClazz() {
        return `${this.buttonStyle} ${this.buttonClass ?? ''}`;
    }
}
