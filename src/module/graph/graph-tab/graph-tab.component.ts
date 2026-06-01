/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, Input, OnDestroy, ViewChild } from "@angular/core";
import { Router } from "@angular/router";
import { GraphCanvasComponent } from "../../../framework/graph-visualiser/canvas/graph-canvas.component";
import { GraphViewTab } from "../../../service/graph-view-state.service";

@Component({
    selector: "ts-graph-tab",
    template: `
        <ts-graph-canvas
            [visualiser]="tab.run.graph.visualiser"
            [status]="tab.run.graph.status"
            [run]="tab.run"
            (statusAction)="onGraphStatusAction($event)"
        />
    `,
    styles: [`
        :host {
            display: block;
            position: relative;
            height: 100%;
            width: 100%;
        }
        ts-graph-canvas {
            display: block;
            height: 100%;
            width: 100%;
        }
    `],
    imports: [GraphCanvasComponent],
})
export class GraphTabComponent implements AfterViewInit, OnDestroy {
    @Input({ required: true }) tab!: GraphViewTab;
    @ViewChild(GraphCanvasComponent) graphCanvas?: GraphCanvasComponent;

    constructor(private router: Router) {}

    ngAfterViewInit() {
        const canvasEl = this.graphCanvas?.canvasEl;
        if (canvasEl) {
            this.tab.run.graph.attach(canvasEl);
        }
    }

    ngOnDestroy() {
        this.tab.run.graph.detach();
    }

    onGraphStatusAction(action: string) {
        if (action === "viewLog") this.router.navigate(["/history"]);
    }
}
