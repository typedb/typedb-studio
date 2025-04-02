/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
