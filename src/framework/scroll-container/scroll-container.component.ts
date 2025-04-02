/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, ElementRef, NgZone, ViewChild } from "@angular/core";
import { initCustomScrollbars } from "typedb-web-common/lib";

@Component({
    selector: "tp-scroll-container",
    template: "<ng-content/>",
    standalone: true,
    imports: [],
})
export class ScrollContainerComponent implements AfterViewInit {
    @ViewChild("scrollbarX") scrollbarX!: ElementRef<HTMLElement>;
    @ViewChild("scrollbarY") scrollbarY!: ElementRef<HTMLElement>;

    constructor(private ngZone: NgZone, private elementRef: ElementRef) { }

    ngAfterViewInit() {
        this.ngZone.runOutsideAngular(() => initCustomScrollbars(this.elementRef.nativeElement));
    }
}
