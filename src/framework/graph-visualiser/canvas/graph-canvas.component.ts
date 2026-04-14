/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, ElementRef, EventEmitter, HostBinding, Input, OnDestroy, Output, ViewChild } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ResizableDirective } from "@hhangular/resizable";
import { Subscription } from "rxjs";
import { GraphVisualiser } from "../engine";
import { GraphZoomControlsComponent } from "./zoom-controls/graph-zoom-controls.component";
import { GraphStylesPaneComponent } from "../style-editor/graph-styles-pane.component";
import { GraphStyleService } from "../../../service/graph-style.service";

@Component({
    selector: "ts-graph-canvas",
    templateUrl: "graph-canvas.component.html",
    styleUrls: ["graph-canvas.component.scss"],
    imports: [MatTooltipModule, ResizableDirective, GraphZoomControlsComponent, GraphStylesPaneComponent],
})
export class GraphCanvasComponent implements OnDestroy {
    @Input() visualiser: GraphVisualiser | null = null;
    @Input() queryRunning = false;
    @Input() graphPercent = 75;
    @Input() stylesPanePercent = 25;
    @Input() maximised = false;

    @Output() maximisedChange = new EventEmitter<boolean>();
    @Output() graphPercentChange = new EventEmitter<number>();
    @Output() stylesPanePercentChange = new EventEmitter<number>();

    @ViewChild("canvasEl", { static: false }) canvasElRef?: ElementRef<HTMLElement>;

    @HostBinding("class.graph-dark") isDark = true;
    @HostBinding("class.graph-light") isLight = false;

    private stylesSub: Subscription;

    constructor(private styleService: GraphStyleService) {
        this.stylesSub = this.styleService.styles$.subscribe(() => this.updateControlTheme());
        this.updateControlTheme();
    }

    ngOnDestroy() {
        this.stylesSub.unsubscribe();
    }

    private updateControlTheme() {
        const hex = this.styleService.effectiveBackgroundHex;
        const h = hex.startsWith("#") ? hex.slice(1) : hex;
        const r = parseInt(h.substring(0, 2), 16);
        const g = parseInt(h.substring(2, 4), 16);
        const b = parseInt(h.substring(4, 6), 16);
        const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        this.isDark = luminance < 0.5;
        this.isLight = luminance >= 0.5;
    }

    get canvasEl(): HTMLElement | undefined {
        return this.canvasElRef?.nativeElement;
    }

    toggleMaximised() {
        this.maximised = !this.maximised;
        this.maximisedChange.emit(this.maximised);
        document.body.classList.toggle("graph-fullscreen", this.maximised);
        setTimeout(() => {
            this.visualiser?.sigma.resize();
            this.visualiser?.sigma.refresh();
        });
    }
}
