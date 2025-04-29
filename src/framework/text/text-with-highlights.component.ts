/*!/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { NgStyle, NgTemplateOutlet } from "@angular/common";
import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { ParagraphWithHighlights } from "typedb-web-schema/lib";

@Component({
    selector: "tp-heading-with-highlights",
    templateUrl: "heading-with-highlights.component.html",
    styleUrls: ["heading-with-highlights.component.scss"],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [NgTemplateOutlet, NgStyle],
})
export class HeadingWithHighlightsComponent {
    // eslint-disable-next-line @angular-eslint/no-input-rename
    @Input("id") inputId?: string;
    @Input() level: "h1" | "h2" | "h3" | "h4" = "h2";
    @Input() value!: ParagraphWithHighlights;
    @Input() themeColorHex = "#02DAC9";

    get id(): string {
        return (
            this.inputId ||
            this.value.spans
                .map((v) => v.text)
                .join("")
                .toLocaleLowerCase()
                .replace(/ /g, "-")
                .replace(/[^0-9a-z-]/g, "")
        );
    }
}

@Component({
    selector: "tp-p-with-highlights",
    template:
        '<p [class]="rootClass">@for (span of value.spans; track span) {<span [ngStyle]="span.highlight ? { \'color\': themeColorHex } : undefined">{{ span.text }}</span>}</p>\n',
    styleUrls: ["p-with-highlights.component.scss"],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [NgStyle],
})
export class ParagraphWithHighlightsComponent {
    @Input() value!: ParagraphWithHighlights;
    @Input() themeColorHex = "#02DAC9";
    @Input() level: "p1" | "p2" | "aside" = "p1";

    get rootClass(): string {
        return `text-${this.level}`;
    }
}
