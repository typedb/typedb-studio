import { Component, HostBinding, inject, Input, OnChanges, OnDestroy, SimpleChanges } from "@angular/core";
import { NgTemplateOutlet } from "@angular/common";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatMenuModule } from "@angular/material/menu";
import { ResizableDirective } from "@hhangular/resizable";
import { Subscription } from "rxjs";
import { GraphStyleService, GraphSidePanelDock } from "../../../service/graph-style.service";
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
        NgTemplateOutlet,
        MatTooltipModule, MatMenuModule,
        ResizableDirective,
        ElementsTabComponent, ThemesTabComponent, CustomiseTabComponent,
        GraphInspectorComponent, GraphTypeInspectorComponent,
    ],
})
export class GraphSidePanelComponent implements OnChanges, OnDestroy {

    // Force the dock-driven layout via inline style. The view-encapsulated SCSS
    // sets these too, but on tab switch the host's stylesheet sometimes lags one
    // frame behind the child resizable directive's `ngAfterViewInit`. That
    // directive calls `getComputedStyle(parent).flexDirection` on the inspector
    // pane's parent (which is :host now that the Inspector + tabbed panel are
    // direct flex children again); without the inline style it reads the
    // browser default "row" and latches into the wrong orientation.
    @HostBinding("style.flexDirection") get hostFlexDirection(): string {
        return this.dock === "bottom" ? "row" : "column";
    }
    @HostBinding("style.display") readonly hostDisplay = "flex";

    /** Dock class on the host: `.dock-bottom` flips the inner Inspector/tabbed
     *  split to horizontal; `.dock-right` carries the divider border on the
     *  left edge. */
    @HostBinding("class.dock-bottom") get isDockBottom(): boolean { return this.dock === "bottom"; }
    @HostBinding("class.dock-right") get isDockRight(): boolean { return this.dock === "right"; }

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

    // -- Dock side --

    get dock(): GraphSidePanelDock {
        return this.styleService.sidePanelDock;
    }

    setDock(dock: GraphSidePanelDock): void {
        this.styleService.sidePanelDock = dock;
    }

    // -- Footer actions --

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
