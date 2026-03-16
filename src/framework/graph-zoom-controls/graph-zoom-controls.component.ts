import { Component, Input } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";
import { GraphVisualiser } from "../graph-visualiser";

const ZOOM_FACTOR = 0.7;

@Component({
    selector: "ts-graph-zoom-controls",
    templateUrl: "graph-zoom-controls.component.html",
    styleUrls: ["graph-zoom-controls.component.scss"],
    imports: [MatTooltipModule],
})
export class GraphZoomControlsComponent {

    @Input() visualiser: GraphVisualiser | null = null;
    @Input() queryRunning = false;

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

    resetZoom(): void {
        this.visualiser?.centerCamera();
    }

    stopLayout(): void {
        this.visualiser?.stopLayout();
    }

    reLayout(): void {
        this.visualiser?.reLayout();
    }
}
