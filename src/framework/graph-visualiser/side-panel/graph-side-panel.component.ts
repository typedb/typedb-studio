import { Component, inject, Input } from "@angular/core";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { ResizableDirective } from "@hhangular/resizable";
import { GraphStyleService } from "../../../service/graph-style.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SchemaConcept } from "../../../service/schema-state.service";
import { GraphVisualiser } from "../engine";
import { ElementsTabComponent } from "./elements-tab.component";
import { ThemesTabComponent } from "./themes-tab.component";
import { CustomiseTabComponent } from "./customise-tab.component";
import { GraphInspectorComponent } from "../inspector/graph-inspector.component";

@Component({
    selector: "ts-graph-side-panel",
    templateUrl: "graph-side-panel.component.html",
    styleUrls: ["graph-side-panel.component.scss"],
    imports: [
        MatSelectModule, MatTooltipModule, MatFormFieldModule, MatSlideToggleModule,
        ResizableDirective,
        ElementsTabComponent, ThemesTabComponent, CustomiseTabComponent,
        GraphInspectorComponent,
    ],
})
export class GraphSidePanelComponent {

    @Input() visualiser: GraphVisualiser | null = null;
    @Input() run: RunOutputState | null = null;
    // Selection — wired in Phase C4. Until then the Inspector shows its placeholder.
    @Input() selectedType: SchemaConcept | null = null;
    @Input() selectedInstanceIID: string | null = null;

    // Internal vertical split between Inspector and the tabbed lower pane.
    inspectorPercent = 40;
    lowerPanelPercent = 60;

    styleService = inject(GraphStyleService);
    topTab: "elements" | "presets" | "customise" = "elements";

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

    get labelMode(): "auto" | "fixed" | "hidden" {
        if (!this.styleService.labelsVisible) return "hidden";
        return this.styleService.labelColorMode === "fixed" ? "fixed" : "auto";
    }

    setLabelMode(mode: "auto" | "fixed" | "hidden"): void {
        if (mode === "hidden") {
            this.styleService.labelsVisible = false;
        } else {
            this.styleService.labelsVisible = true;
            this.styleService.labelColorMode = mode === "fixed" ? "fixed" : "auto";
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
