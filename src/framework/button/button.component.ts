/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
    @Input() buttonStyle?: ButtonStyle;
    @Input() enabled = true;
    @Input() type: HTMLButtonElement["type"] = "button";
    @Input({ required: true }) buttonId!: string;

    constructor() {}
}
