/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
