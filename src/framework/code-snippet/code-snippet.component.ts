/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Input, NgZone, OnChanges, signal, ViewChild } from "@angular/core";

import Prism from "prismjs";
import { initCustomScrollbars } from "typedb-web-common/lib";
import { MatTooltip, MatTooltipModule } from "@angular/material/tooltip";
import { from, Observable } from "rxjs";

export interface CodeSnippetAction {
    name: string;
    icon: string;
    label?: string | null;
    action: (code: string) => void | Observable<unknown>;
}

@Component({
    selector: "tp-code-snippet",
    templateUrl: "code-snippet.component.html",
    styleUrls: ["code-snippet.component.scss"],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [MatTooltipModule],
})
export class CodeSnippetComponent implements AfterViewInit, OnChanges {

    readonly copyCodeAction: CodeSnippetAction = {
        name: "copy",
        icon: "fa-copy",
        label: null,
        action: () => from(this.copyCode())
    }

    @Input({ required: true }) snippet!: { language?: string, code: string };
    @Input() actions: CodeSnippetAction[] = [this.copyCodeAction];
    @ViewChild("scrollbarX") scrollbarX!: ElementRef<HTMLElement>;
    @ViewChild("scrollbarY") scrollbarY!: ElementRef<HTMLElement>;
    @ViewChild("rootElement") rootElement!: ElementRef<HTMLElement>;

    copied = false;

    get lineNumbers() {
        return [...Array(
            (this.snippet.code.match(/\n/g) || []).length + 1,
        ).keys()].map((n) => n + 1)
    }

    constructor(private ngZone: NgZone, private elementRef: ElementRef) {
    }

    ngAfterViewInit() {
        this.maybeInitScrollbarsAndHighlighting();
    }

    ngOnChanges() {
        this.maybeInitScrollbarsAndHighlighting();
    }

    maybeInitScrollbarsAndHighlighting() {
        if (this.snippet && this.rootElement) {
            Prism.highlightAllUnder(this.elementRef.nativeElement);
        }
    }

    async copyCode() {
        try {
            await navigator.clipboard.writeText(this.snippet.code);
            this.copied = true;
            
            // Reset copied state after 3 seconds
            setTimeout(() => {
                this.copied = false;
            }, 3000);
        } catch (err) {
            console.error('Failed to copy code:', err);
        }
    }
}
