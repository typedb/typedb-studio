/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, ElementRef, EventEmitter, HostBinding, Input, OnDestroy, Output, ViewChild, AfterViewInit, AfterViewChecked } from "@angular/core";
import { NgTemplateOutlet } from "@angular/common";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatMenuModule } from "@angular/material/menu";

import { ResizableDirective } from "@hhangular/resizable";
import { Subscription } from "rxjs";
import { GraphPngExportMode, GraphVisualiser } from "../engine";
import { GraphZoomControlsComponent } from "./zoom-controls/graph-zoom-controls.component";
import { GraphSidePanelComponent } from "../side-panel/graph-side-panel.component";
import { GraphContextMenuComponent } from "../context-menu/graph-context-menu.component";
import { GraphStyleService, buildBackgroundCSS } from "../../../service/graph-style.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SelectionMode } from "../../../service/graph-view-state.service";

export type GraphCanvasStatus = "ok" | "running" | "noQueryAnswers" | "noInstancesFound" | "error" | "graphlessQueryType" | "answerOutputDisabled" | "multiQuery" | "emptySchema";
export type GraphCanvasStatusAction = "viewLog";

@Component({
    selector: "ts-graph-canvas",
    templateUrl: "graph-canvas.component.html",
    styleUrls: ["graph-canvas.component.scss"],
    imports: [NgTemplateOutlet, MatTooltipModule, MatMenuModule, ResizableDirective, GraphZoomControlsComponent, GraphSidePanelComponent, GraphContextMenuComponent],
})
export class GraphCanvasComponent implements AfterViewInit, AfterViewChecked, OnDestroy {
    @Input() visualiser: GraphVisualiser | null = null;
    @Input() status: GraphCanvasStatus = "ok";
    @Input() graphPercent = 75;
    @Input() stylesPanePercent = 25;
    @Input() maximised = false;

    /** Side-panel size when docked bottom. Kept separate from
     *  `stylesPanePercent` (the right-dock width) because a width-tuned value
     *  is too short as a height — bottom defaults taller. */
    private bottomPanePercent = 45;
    /** The run that owns this canvas's graph. Passed through to the side panel
     *  so the Inspector knows where to push instances/attributes/links. */
    @Input() run: RunOutputState | null = null;
    /** True if the parent surface tracks a "Reset changes" capability and the
     *  graph currently has something to reset (e.g. a graph-view tab whose
     *  contents have diverged from the initial query). Drives the
     *  reset-changes button's enabled state in the zoom-controls panel. */
    @Input() hasChanges = false;
    /** When non-null, an in-canvas toggle is shown for switching between
     *  type-selection and instance-selection modes. Null hides the toggle
     *  (e.g. for canvas usages that don't have a type/instance distinction
     *  like chat output). */
    @Input() selectionMode: SelectionMode | null = null;

    @Output() maximisedChange = new EventEmitter<boolean>();
    @Output() graphPercentChange = new EventEmitter<number>();
    @Output() stylesPanePercentChange = new EventEmitter<number>();
    @Output() statusAction = new EventEmitter<GraphCanvasStatusAction>();
    @Output() resetChangesClicked = new EventEmitter<void>();
    @Output() selectionModeChange = new EventEmitter<SelectionMode>();

    get queryRunning() { return this.status === "running"; }

    @ViewChild("canvasEl", { static: false }) canvasElRef?: ElementRef<HTMLElement>;

    @HostBinding("class.graph-dark") isDark = true;
    @HostBinding("class.graph-light") isLight = false;

    private stylesSub: Subscription;

    constructor(private styleService: GraphStyleService) {
        this.stylesSub = this.styleService.styles$.subscribe(() => {
            this.updateControlTheme();
            this.applyBackground();
        });
        this.updateControlTheme();
    }

    /** The canvas element currently hosting the sigma renderer. When the dock
     *  axis flips, the `@if` rebuilds `.canvas-element`, so we detect the new
     *  node here and move the renderer onto it (preserving graph + camera). */
    private attachedCanvasEl: HTMLElement | null = null;

    ngAfterViewInit() {
        this.applyBackground();
    }

    ngAfterViewChecked() {
        const el = this.canvasElRef?.nativeElement ?? null;
        if (el && el !== this.attachedCanvasEl && this.attachedCanvasEl !== null && this.run) {
            // The host element was rebuilt (dock axis changed). Re-home the
            // renderer; GraphOutputState.attach/detach preserves the graph and
            // restores the camera, so no graph state is lost. Deferred via
            // setTimeout so detach() (which sets visualiser=null) doesn't
            // mutate parent-read state inside the same CD cycle and trip
            // ExpressionChangedAfterItHasBeenCheckedError on GraphTabComponent.
            const run = this.run;
            this.attachedCanvasEl = el;
            setTimeout(() => {
                run.graph.detach();
                run.graph.attach(el);
                this.applyBackground();
                // Use run.graph.visualiser, not this.visualiser: the @Input
                // hasn't been re-checked yet, so this.visualiser still points
                // at the destroyed pre-detach instance.
                run.graph.visualiser?.sigma.resize();
                run.graph.visualiser?.sigma.refresh();
            });
        } else if (el && this.attachedCanvasEl === null) {
            // First view-check after the parent's initial attach(); record it.
            this.attachedCanvasEl = el;
        }
    }

    ngOnDestroy() {
        this.stylesSub.unsubscribe();
    }

    private applyBackground() {
        const el = this.canvasElRef?.nativeElement;
        if (!el) return;
        const css = buildBackgroundCSS(this.styleService.background);
        el.style.backgroundColor = css.color;
        el.style.backgroundImage = css.image;
        el.style.backgroundSize = css.size;
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

    /** Which edge the side panel docks to (graph-wide, persisted). */
    get dock() {
        return this.styleService.sidePanelDock;
    }

    /** True when the outer split stacks vertically (panel docked bottom). */
    get isVertical(): boolean {
        return this.dock === "bottom";
    }

    /** Side-panel size for the current dock (height for bottom, width for right). */
    get sidePanelPercent(): number {
        return this.isVertical ? this.bottomPanePercent : this.stylesPanePercent;
    }

    /** Persist a side-panel resize against the right dock-orientation slot. */
    onSidePanelPercentChange(percent: number): void {
        if (this.isVertical) {
            this.bottomPanePercent = percent;
        } else {
            this.stylesPanePercent = percent;
            this.stylesPanePercentChange.emit(percent);
        }
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

    exporting = false;
    async exportPng(mode: GraphPngExportMode) {
        const visualiser = this.visualiser;
        if (!visualiser || this.exporting) return;
        this.exporting = true;
        try {
            const blob = await visualiser.exportPng(mode);
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            const ts = new Date().toISOString().replace(/[:.]/g, "-");
            const suffix = mode === "wholeGraph" ? "whole-graph" : "current-view";
            a.download = `graph-${suffix}-${ts}.png`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            setTimeout(() => URL.revokeObjectURL(url), 0);
        } catch (err) {
            console.error("[Graph PNG Export]", err);
        } finally {
            this.exporting = false;
        }
    }

    canExportPng(): boolean {
        return !!this.visualiser && this.visualiser.graph.order > 0 && this.status === "ok";
    }
}
