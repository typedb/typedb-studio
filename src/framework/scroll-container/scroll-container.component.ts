/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
