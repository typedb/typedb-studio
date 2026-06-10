import { AsyncPipe } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { GraphVisualiser } from "../../engine";

const ZOOM_FACTOR = 0.7;

@Component({
    selector: "ts-graph-zoom-controls",
    templateUrl: "graph-zoom-controls.component.html",
    styleUrls: ["graph-zoom-controls.component.scss"],
    imports: [MatTooltipModule, AsyncPipe],
})
export class GraphZoomControlsComponent {

    @Input() visualiser: GraphVisualiser | null = null;
    @Input() queryRunning = false;
    @Input() hasChanges = false;

    @Output() resetChangesClicked = new EventEmitter<void>();

    redrawCooldown = false;
    private cooldownTimer: ReturnType<typeof setTimeout> | null = null;

    zoomIn(): void {
        const camera = this.visualiser?.sigma.getCamera();
        if (!camera) return;
        camera.animatedUnzoom({ duration: 150, factor: ZOOM_FACTOR });
    }

    zoomOut(): void {
        const camera = this.visualiser?.sigma.getCamera();
        if (!camera) return;
        camera.animatedZoom({ duration: 150, factor: ZOOM_FACTOR });
    }

    /** When something is selected, frame it; otherwise reset to the global view. */
    resetOrFocus(): void {
        if (this.visualiser?.interactionHandler?.state?.selectedNode != null) {
            this.visualiser.focusSelection();
        } else {
            this.visualiser?.centerCamera();
        }
    }

    stopLayout(): void {
        this.visualiser?.stopLayout();
        this.redrawCooldown = true;
        if (this.cooldownTimer) clearTimeout(this.cooldownTimer);
        this.cooldownTimer = setTimeout(() => { this.redrawCooldown = false; }, 1000);
    }

    reLayout(): void {
        this.visualiser?.reLayout();
    }

    collapse(): void {
        this.visualiser?.collapse();
    }

    get canCollapse(): boolean {
        return !this.queryRunning
            && !this.visualiser?.isLayoutRunning
            && (this.visualiser?.graph.order ?? 0) > 0;
    }
}
