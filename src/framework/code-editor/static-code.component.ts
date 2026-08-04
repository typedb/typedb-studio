/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { classHighlighter, highlightCode } from "@lezer/highlight";
import { TypeQLLanguage } from "../codemirror-lang-typeql";

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
export class StaticCodeComponent implements OnChanges, AfterViewInit {

    @Input({ required: true }) code!: string;
    @Input() copyOverlayVisible = false;
    @Input() expandOverlayVisible = false;
    @Output() expandButtonClick = new EventEmitter<void>();

    @ViewChild("codeEl", { static: true }) private codeEl?: ElementRef<HTMLElement>;

    copied = false;
    private renderedCode: string | null = null;

    ngOnChanges(changes: SimpleChanges) {
        if (changes["code"]) this.renderIfReady();
    }

    ngAfterViewInit() {
        this.renderIfReady();
    }

    private renderIfReady() {
        // The first ngOnChanges can fire before the view (and #codeEl) exists;
        // ngAfterViewInit covers that case.
        if (!this.codeEl) return;
        const code = this.code ?? "";
        if (code === this.renderedCode) return;
        this.renderedCode = code;

        const el = this.codeEl.nativeElement;
        el.textContent = "";
        // TypeQLLanguage.parser carries the styleTags metadata — the raw generated
        // parser does not, and produces zero highlight classes.
        highlightCode(code, TypeQLLanguage.parser.parse(code), classHighlighter,
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
