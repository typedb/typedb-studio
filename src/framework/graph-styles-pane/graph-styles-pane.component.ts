import { Component, inject, Input } from "@angular/core";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { GraphStyleService } from "../../service/graph-style.service";
import { GraphVisualiser } from "../graph-visualiser";
import { HighlightsTabComponent } from "./highlights-tab.component";
import { ThemesTabComponent } from "./themes-tab.component";
import { EditorTabComponent } from "./editor-tab.component";

@Component({
    selector: "ts-graph-styles-pane",
    templateUrl: "graph-styles-pane.component.html",
    styleUrls: ["graph-styles-pane.component.scss"],
    imports: [
        MatSelectModule, MatTooltipModule, MatFormFieldModule, MatSlideToggleModule,
        HighlightsTabComponent, ThemesTabComponent, EditorTabComponent,
    ],
})
export class GraphStylesPaneComponent {

    @Input() visualiser: GraphVisualiser | null = null;

    styleService = inject(GraphStyleService);
    topTab: "highlights" | "presets" | "customise" = "highlights";

    get isHighlightActive(): boolean {
        return this.styleService.isHighlightActive();
    }

    // -- Footer controls --

    get colorEdgesByConstraint(): boolean {
        return this.styleService.colorEdgesByConstraint;
    }

    toggleEdgeColoring(): void {
        this.styleService.colorEdgesByConstraint = !this.styleService.colorEdgesByConstraint;
        this.visualiser?.colorEdgesByConstraintIndex(!this.styleService.colorEdgesByConstraint);
    }

    get labelMode(): "border" | "fixed" | "hidden" {
        if (!this.styleService.labelsVisible) return "hidden";
        return this.styleService.labelUseBorderColor ? "border" : "fixed";
    }

    setLabelMode(mode: "border" | "fixed" | "hidden"): void {
        if (mode === "hidden") {
            this.styleService.labelsVisible = false;
        } else {
            this.styleService.labelsVisible = true;
            this.styleService.labelUseBorderColor = mode === "border";
        }
        this.visualiser?.restoreLabels();
    }

    get showHoverLabel(): boolean {
        return this.styleService.showHoverLabel;
    }

    toggleShowHoverLabel(): void {
        this.styleService.showHoverLabel = !this.styleService.showHoverLabel;
        this.visualiser?.restoreLabels();
    }

    get degreeScaling(): boolean {
        return this.styleService.degreeScaling;
    }

    toggleDegreeScaling(): void {
        this.styleService.degreeScaling = !this.styleService.degreeScaling;
        if (this.styleService.degreeScaling) {
            this.visualiser?.applyStructureMode();
        } else {
            this.visualiser?.applyStyleUpdate();
        }
    }

    reLayout(): void {
        this.visualiser?.reLayout();
    }

    resetAll(): void {
        this.styleService.resetToDefaults();
        this.visualiser?.applyStyleUpdate();
        this.visualiser?.applyEdgeStyleUpdate();
        this.visualiser?.colorEdgesByConstraintIndex(true);
    }
}
