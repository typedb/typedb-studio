/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ResizableDirective } from "@hhangular/resizable";
import { GraphVisualiser } from "../graph-visualiser";
import { GraphZoomControlsComponent } from "../graph-zoom-controls/graph-zoom-controls.component";
import { GraphStylesPaneComponent } from "../graph-styles-pane/graph-styles-pane.component";

@Component({
    selector: "ts-graph-canvas",
    templateUrl: "graph-canvas.component.html",
    styleUrls: ["graph-canvas.component.scss"],
    imports: [MatTooltipModule, ResizableDirective, GraphZoomControlsComponent, GraphStylesPaneComponent],
})
export class GraphCanvasComponent {
    @Input() visualiser: GraphVisualiser | null = null;
    @Input() queryRunning = false;
    @Input() graphPercent = 75;
    @Input() stylesPanePercent = 25;
    @Input() maximised = false;

    @Output() maximisedChange = new EventEmitter<boolean>();
    @Output() graphPercentChange = new EventEmitter<number>();
    @Output() stylesPanePercentChange = new EventEmitter<number>();

    @ViewChild("canvasEl", { static: false }) canvasElRef?: ElementRef<HTMLElement>;

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
