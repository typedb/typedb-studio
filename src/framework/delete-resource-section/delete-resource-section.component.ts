/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { NgClass } from "@angular/common";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ButtonComponent } from "../button/button.component";

interface AlwaysEnabled {
    text: string;
}

interface Toggleable {
    enabled: boolean;
    enabledText: string;
    disabledText: string;
}

type DisplayText = AlwaysEnabled | Toggleable;

function isAlwaysEnabled(display: DisplayText): display is AlwaysEnabled {
    return "text" in display;
}

@Component({
    selector: "tp-delete-resource-section",
    templateUrl: "./delete-resource-section.component.html",
    styleUrls: ["./delete-resource-section.component.scss"],
    standalone: true,
    imports: [NgClass, ButtonComponent, MatTooltipModule],
})
export class DeleteResourceSectionComponent {
    @Input() title!: string;
    @Input() display!: DisplayText;
    @Input() buttonText!: string;
    @Input() buttonIconClass?: string;
    @Output() buttonPressed = new EventEmitter<boolean>();

    get bodyText() {
        if (isAlwaysEnabled(this.display)) return this.display.text;
        return this.display.enabled ? this.display.enabledText : this.display.disabledText;
    }

    isEnabled() {
        if (isAlwaysEnabled(this.display)) return true;
        return this.display.enabled;
    }

    onButtonClick() {
        if (this.isEnabled()) {
            this.buttonPressed.emit(true);
        }
    }
}
