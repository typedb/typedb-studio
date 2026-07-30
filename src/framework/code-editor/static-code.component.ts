/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { classHighlighter, highlightCode } from "@lezer/highlight";
import { parser } from "../codemirror-lang-typeql/generated/typeql.grammar.generated";

/**
 * Read-only, statically highlighted TypeQL code block. Unlike CodeEditorComponent,
 * this creates no CodeMirror instance — safe to render in large lists (e.g. query history).
 */
@Component({
    selector: "tp-static-code",
    templateUrl: "static-code.component.html",
    styleUrls: ["static-code.component.scss"],
    standalone: true,
    imports: [MatTooltipModule],
})
export class StaticCodeComponent implements OnChanges {

    @Input({ required: true }) code!: string;
    @Input() runOverlayVisible = false;
    @Output() runButtonClick = new EventEmitter<void>();
    @Input() expandOverlayVisible = false;
    @Output() expandButtonClick = new EventEmitter<void>();

    @ViewChild("codeEl", { static: true }) private codeEl!: ElementRef<HTMLElement>;

    ran = false;
    copied = false;

    ngOnChanges(changes: SimpleChanges) {
        if (changes["code"]) this.render();
    }

    private render() {
        const el = this.codeEl.nativeElement;
        el.textContent = "";
        const code = this.code ?? "";
        highlightCode(code, parser.parse(code), classHighlighter,
            (text, classes) => {
                if (classes) {
                    const span = document.createElement("span");
                    span.className = classes;
                    span.textContent = text;
                    el.appendChild(span);
                } else {
                    el.appendChild(document.createTextNode(text));
                }
            },
            () => el.appendChild(document.createTextNode("\n")),
        );
    }

    onRunButtonClick() {
        this.ran = true;
        this.runButtonClick.emit();
        setTimeout(() => { this.ran = false; }, 3000);
    }

    async onCopyButtonClick() {
        try {
            await navigator.clipboard.writeText(this.code);
            this.copied = true;
            setTimeout(() => { this.copied = false; }, 3000);
        } catch (err) {
            console.error('Failed to copy code:', err);
        }
    }
}
