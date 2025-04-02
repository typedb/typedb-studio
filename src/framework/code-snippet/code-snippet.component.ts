/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { AfterViewChecked, AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Input, NgZone, ViewChild } from "@angular/core";

import Prism from "prismjs";
import { initCustomScrollbars } from "typedb-web-common/lib";

const DEFAULT_MIN_LINES = { desktop: 33, mobile: 13 };

@Component({
    selector: "tp-code-snippet",
    templateUrl: "code-snippet.component.html",
    styleUrls: ["code-snippet.component.scss"],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [],
})
export class CodeSnippetComponent implements AfterViewInit, AfterViewChecked {
    @Input({ required: true }) snippet!: { language?: string, code: string };
    @ViewChild("scrollbarX") scrollbarX!: ElementRef<HTMLElement>;
    @ViewChild("scrollbarY") scrollbarY!: ElementRef<HTMLElement>;

    get lineNumbers() {
        return [...Array(Math.max(
            (this.snippet.code.match(/\n/g) || []).length + 2,
            DEFAULT_MIN_LINES.desktop,
        )).keys()].map((n) => n + 1)
    }

    constructor(private ngZone: NgZone, private elementRef: ElementRef) { }

    ngAfterViewInit() {
        this.ngZone.runOutsideAngular(() => initCustomScrollbars(this.elementRef.nativeElement));
    }

    ngAfterViewChecked() {
        Prism.highlightAll();
    }
}
