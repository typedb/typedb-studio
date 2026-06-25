/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, inject, Input, OnDestroy, ViewChild } from "@angular/core";
import { Router } from "@angular/router";
import { GraphCanvasComponent } from "../../../framework/graph-visualiser/canvas/graph-canvas.component";
import { GraphViewState, GraphViewTab, SelectionMode } from "../../../service/graph-view-state.service";

@Component({
    selector: "ts-graph-tab",
    template: `
        <ts-graph-canvas
            [visualiser]="tab.run.graph.visualiser"
            [status]="tab.run.graph.status"
            [run]="tab.run"
            [hasChanges]="hasChanges"
            [selectionMode]="tab.selectionMode"
            (statusAction)="onGraphStatusAction($event)"
            (resetChangesClicked)="onResetChanges()"
            (selectionModeChange)="onSelectionModeChange($event)"
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

    private graphViewState = inject(GraphViewState);

    constructor(private router: Router) {}

    get hasChanges(): boolean {
        return this.graphViewState.tabHasChanges(this.tab);
    }

    onResetChanges(): void {
        this.graphViewState.resetTab(this.tab);
    }

    onSelectionModeChange(mode: SelectionMode): void {
        if (this.tab.selectionMode === mode) return;
        this.tab.selectionMode = mode;
        // setSelectionMode also calls clearSelection() internally — type and
        // instance selection mean different things, so the previous
        // selection is no longer meaningful.
        this.tab.run.graph.visualiser?.interactionHandler.setSelectionMode(mode);
    }

    ngAfterViewInit() {
        const canvasEl = this.graphCanvas?.canvasEl;
        if (canvasEl) {
            this.tab.run.graph.attach(canvasEl);
        }
        // Sync the tab's stored selectionMode onto the freshly-created
        // interaction handler. The handler defaults to "types" too so this is
        // typically a no-op for new tabs, but covers the case where the tab
        // had been toggled to "instances" and is being re-attached.
        this.tab.run.graph.visualiser?.interactionHandler.setSelectionMode(this.tab.selectionMode);
    }

    ngOnDestroy() {
        this.tab.run.graph.detach();
    }

    onGraphStatusAction(action: string) {
        if (action === "viewLog") this.router.navigate(["/history"]);
        else if (action === "openTransaction") this.graphViewState.openReadTransaction();
        else if (action === "switchToAuto") this.graphViewState.switchToAutoMode();
    }
}
