/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";
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
    standalone: true,
    imports: [NgStyle, NgClass, SpinnerComponent],
})
export class ButtonComponent {
    @Input() height = 40;
    @Input() buttonStyle: ButtonStyle = "secondary";
    @Input() enabled = true;
    @Input() type: HTMLButtonElement["type"] = "button";
    @Input({ required: true }) buttonId!: string;

    constructor() {}
}
