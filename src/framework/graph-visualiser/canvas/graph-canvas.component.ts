/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, ElementRef, EventEmitter, HostBinding, Input, OnDestroy, Output, ViewChild, AfterViewInit } from "@angular/core";
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

export type GraphCanvasStatus = "ok" | "running" | "noAnswers" | "error" | "graphlessQueryType" | "answerOutputDisabled" | "multiQuery" | "emptySchema";
export type GraphCanvasStatusAction = "viewLog";

@Component({
    selector: "ts-graph-canvas",
    templateUrl: "graph-canvas.component.html",
    styleUrls: ["graph-canvas.component.scss"],
    imports: [MatTooltipModule, MatMenuModule, ResizableDirective, GraphZoomControlsComponent, GraphSidePanelComponent, GraphContextMenuComponent],
})
export class GraphCanvasComponent implements AfterViewInit, OnDestroy {
    @Input() visualiser: GraphVisualiser | null = null;
    @Input() status: GraphCanvasStatus = "ok";
    @Input() graphPercent = 75;
    @Input() stylesPanePercent = 25;
    @Input() maximised = false;
    /** The run that owns this canvas's graph. Passed through to the side panel
     *  so the Inspector knows where to push instances/attributes/links. */
    @Input() run: RunOutputState | null = null;
    /** True if the parent surface tracks a "Reset changes" capability and the
     *  graph currently has something to reset (e.g. a graph-view tab whose
     *  contents have diverged from the initial query). Drives the
     *  reset-changes button's enabled state in the zoom-controls panel. */
    @Input() hasChanges = false;

    @Output() maximisedChange = new EventEmitter<boolean>();
    @Output() graphPercentChange = new EventEmitter<number>();
    @Output() stylesPanePercentChange = new EventEmitter<number>();
    @Output() statusAction = new EventEmitter<GraphCanvasStatusAction>();
    @Output() resetChangesClicked = new EventEmitter<void>();

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

    ngAfterViewInit() {
        this.applyBackground();
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
