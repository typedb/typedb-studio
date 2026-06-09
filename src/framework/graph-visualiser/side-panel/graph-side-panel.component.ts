import { Component, HostBinding, inject, Input, OnChanges, OnDestroy, SimpleChanges } from "@angular/core";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { ResizableDirective } from "@hhangular/resizable";
import { Subscription } from "rxjs";
import { GraphStyleService } from "../../../service/graph-style.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SchemaConcept, SchemaState } from "../../../service/schema-state.service";
import { SelectionMode } from "../../../service/graph-view-state.service";
import { GraphVisualiser } from "../engine";
import { InspectableSelection, TypeSelection } from "../engine/interaction-handler";
import { ElementsTabComponent } from "./elements-tab.component";
import { ThemesTabComponent } from "./themes-tab.component";
import { CustomiseTabComponent } from "./customise-tab.component";
import { GraphInspectorComponent } from "../inspector/graph-inspector.component";
import { GraphTypeInspectorComponent } from "../inspector/graph-type-inspector.component";

@Component({
    selector: "ts-graph-side-panel",
    templateUrl: "graph-side-panel.component.html",
    styleUrls: ["graph-side-panel.component.scss"],
    imports: [
        MatSelectModule, MatTooltipModule, MatFormFieldModule, MatSlideToggleModule,
        ResizableDirective,
        ElementsTabComponent, ThemesTabComponent, CustomiseTabComponent,
        GraphInspectorComponent, GraphTypeInspectorComponent,
    ],
})
export class GraphSidePanelComponent implements OnChanges, OnDestroy {

    // Force column layout via inline style. The view-encapsulated SCSS sets
    // this too, but on tab switch the host's stylesheet sometimes lags one
    // frame behind the child resizable directive's `ngAfterViewInit`. That
    // directive calls `getComputedStyle(parent).flexDirection`, gets the
    // browser default "row", and latches into horizontal mode — which is what
    // makes the inspector tile to the left of the customise pane.
    @HostBinding("style.flexDirection") readonly hostFlexDirection = "column";
    @HostBinding("style.display") readonly hostDisplay = "flex";

    @Input() visualiser: GraphVisualiser | null = null;
    @Input() run: RunOutputState | null = null;
    @Input() selectionMode: SelectionMode = "types";

    // Selection state, populated from the visualiser's interaction handler.
    selectedType: SchemaConcept | null = null;
    selectedInstanceIID: string | null = null;
    /** Type currently selected in type-mode (parallel to selectedType /
     *  selectedInstanceIID for the instance-mode inspector). */
    selectedTypeForTypeMode: SchemaConcept | null = null;

    // Internal vertical split between Inspector and the tabbed lower pane.
    inspectorPercent = 50;
    lowerPanelPercent = 50;

    private schemaState = inject(SchemaState);
    private selectionSub: Subscription | null = null;
    private typeSelectionSub: Subscription | null = null;

    ngOnChanges(changes: SimpleChanges) {
        if (changes["visualiser"]) {
            this.selectionSub?.unsubscribe();
            this.typeSelectionSub?.unsubscribe();
            this.selectionSub = null;
            this.typeSelectionSub = null;
            this.selectedType = null;
            this.selectedInstanceIID = null;
            this.selectedTypeForTypeMode = null;
            const v = this.visualiser;
            if (v) {
                this.selectionSub = v.interactionHandler.selection$.subscribe(sel => {
                    this.applyInstanceSelection(sel);
                });
                this.typeSelectionSub = v.interactionHandler.typeSelection$.subscribe(sel => {
                    this.applyTypeSelection(sel);
                });
            }
        }
    }

    ngOnDestroy() {
        this.selectionSub?.unsubscribe();
        this.typeSelectionSub?.unsubscribe();
    }

    private applyInstanceSelection(selection: InspectableSelection | null) {
        if (!selection) {
            this.selectedType = null;
            this.selectedInstanceIID = null;
            return;
        }
        const schema = this.schemaState.value$.value;
        if (!schema) return;
        const type = selection.kind === "entity"
            ? schema.entities[selection.typeLabel]
            : selection.kind === "relation"
                ? schema.relations[selection.typeLabel]
                : schema.attributes[selection.typeLabel];
        if (!type) return;
        this.selectedType = type;
        this.selectedInstanceIID = selection.instanceId;
    }

    private applyTypeSelection(selection: TypeSelection | null) {
        if (!selection) {
            this.selectedTypeForTypeMode = null;
            return;
        }
        const schema = this.schemaState.value$.value;
        if (!schema) return;
        const map = selection.typeKind === "entityType" ? schema.entities
                  : selection.typeKind === "relationType" ? schema.relations
                  : schema.attributes;
        const type = map[selection.typeLabel];
        if (!type) return;
        this.selectedTypeForTypeMode = type;
    }

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
