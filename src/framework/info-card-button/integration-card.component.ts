/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { NgClass } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";

@Component({
    selector: "tp-integration-card",
    standalone: true,
    imports: [NgClass],
    templateUrl: "./integration-card.component.html",
    styleUrl: "./integration-card.component.scss"
})
export class IntegrationCardComponent {
    @Input() chip?: "active" | "coming-soon";
    @Input() buttonLinkText?: string;
    @Input({ required: true }) title!: string;
    @Input() imageIcon?: { src: string, alt: string };
    @Input() faIcon?: string;
    @Input() enabled: boolean = true;
    @Input() link?: string;
    @Output() cardClick = new EventEmitter();
    
    onLinkClick() {
        if (this.enabled) this.cardClick.emit();
    }
}
