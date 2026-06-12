/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, inject, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from "@angular/core";
import { MatMenu, MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { MatDividerModule } from "@angular/material/divider";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Subscription } from "rxjs";
import { GraphViewState } from "../../../service/graph-view-state.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SchemaConcept, SchemaState } from "../../../service/schema-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { GraphVisualiser } from "../engine";
import { InspectableSelection } from "../engine/interaction-handler";

type SelectionKind = "entity" | "relation" | "attribute";

function typeKindFromSelectionKind(kind: SelectionKind): "entityType" | "relationType" | "attributeType" {
    switch (kind) {
        case "entity": return "entityType";
        case "relation": return "relationType";
        case "attribute": return "attributeType";
    }
}

/** True if the type, or any of its subtypes, plays a role in a relation. */
function typeOrSubtypePlaysRoles(type: SchemaConcept): boolean {
    if ("playedRoles" in type && type.playedRoles.length > 0) return true;
    if ("subtypes" in type) {
        for (const sub of type.subtypes) {
            if (typeOrSubtypePlaysRoles(sub)) return true;
        }
    }
    return false;
}

/** True if the type, or any of its subtypes, owns an attribute type. */
function typeOrSubtypeOwnsAttributes(type: SchemaConcept): boolean {
    if ("ownedAttributes" in type && type.ownedAttributes.length > 0) return true;
    if ("subtypes" in type) {
        for (const sub of type.subtypes) {
            if (typeOrSubtypeOwnsAttributes(sub)) return true;
        }
    }
    return false;
}

/**
 * Right-click context menu for graph instance nodes.
 *
 * Sits invisibly in the canvas; subscribes to `interactionHandler.nodeContextMenu$`,
 * positions a hidden trigger element at the click coordinates, then opens a
 * mat-menu. Regardless of selection mode, the menu shows two groups:
 *
 *   - "This instance"            → Load links / Load attributes for just the
 *                                  clicked IID
 *   - "All '<type>' instances"   → Load links / Load attributes for every IID
 *                                  of the clicked node's type currently in the
 *                                  graph
 *
 * Each action calls the corresponding `GraphViewState.fetch*` method, then
 * triggers a soft reheat with camera preserved and re-evaluates the highlight
 * set so the new nodes join the lit area.
 */
@Component({
    selector: "ts-graph-context-menu",
    templateUrl: "./graph-context-menu.component.html",
    styleUrls: ["./graph-context-menu.component.scss"],
    imports: [MatMenuModule, MatDividerModule, MatTooltipModule],
})
export class GraphContextMenuComponent implements OnChanges, OnDestroy {
    @Input() visualiser: GraphVisualiser | null = null;
    @Input() run: RunOutputState | null = null;

    @ViewChild("trigger", { static: true }) trigger!: MatMenuTrigger;
    @ViewChild(MatMenu, { static: true }) menu!: MatMenu;

    triggerPosition = { x: 0, y: 0 };
    target: InspectableSelection | null = null;

    private graphViewState = inject(GraphViewState);
    private schemaState = inject(SchemaState);
    private snackbar = inject(SnackbarService);
    private sub: Subscription | null = null;

    ngOnChanges(changes: SimpleChanges): void {
        if (changes["visualiser"]) {
            this.sub?.unsubscribe();
            this.sub = null;
            const v = this.visualiser;
            if (v) {
                this.sub = v.interactionHandler.nodeContextMenu$.subscribe(ev => {
                    this.triggerPosition = { x: ev.clientX, y: ev.clientY };
                    this.target = ev.target;
                    // setTimeout so position styles flush to the DOM before
                    // mat-menu measures the trigger. Setting `_openedBy` to
                    // "mouse" lets FocusMonitor suppress the focus ring on
                    // the first auto-focused item — equivalent to what would
                    // happen if the user had clicked the trigger directly
                    // instead of us calling openMenu() programmatically.
                    setTimeout(() => {
                        (this.trigger as any)._openedBy = "mouse";
                        this.trigger.openMenu();
                    });
                });
            }
        }
    }

    ngOnDestroy(): void {
        this.sub?.unsubscribe();
    }

    // -- "This instance" actions --

    loadLinksForThisInstance(): void {
        if (!this.target) return;
        const type = this.lookupType(this.target);
        if (!type || type.kind === "attributeType") return; // attributes don't have links
        this.runFetch(state => state.fetchLinksOf(this.run!, type, [this.target!.instanceId]));
    }

    loadAttributesForThisInstance(): void {
        if (!this.target) return;
        const type = this.lookupType(this.target);
        if (!type || type.kind === "attributeType") return;
        this.runFetch(state => state.fetchAttributesOf(this.run!, type, [this.target!.instanceId]));
    }

    // -- "All instances of type 'X'" actions --

    loadLinksForAllInstancesOfType(): void {
        if (!this.target) return;
        const type = this.lookupType(this.target);
        if (!type || type.kind === "attributeType") return;
        const iids = this.collectInstanceIidsOfType(this.target.typeLabel);
        if (iids.length === 0) return;
        this.runFetch(state => state.fetchLinksOf(this.run!, type, iids));
    }

    loadAttributesForAllInstancesOfType(): void {
        if (!this.target) return;
        const type = this.lookupType(this.target);
        if (!type || type.kind === "attributeType") return;
        const iids = this.collectInstanceIidsOfType(this.target.typeLabel);
        if (iids.length === 0) return;
        this.runFetch(state => state.fetchAttributesOf(this.run!, type, iids));
    }

    /** True when both "instance" actions should be hidden — e.g. attributes don't
     *  have links/attributes themselves, only owners. */
    get isAttributeTarget(): boolean {
        return this.target?.kind === "attribute";
    }

    /** False when the target type (or any subtype) plays no role in any
     *  relation — there are no links to load, so the action is disabled. */
    get canLoadLinks(): boolean {
        const type = this.target ? this.lookupType(this.target) : null;
        return !!type && type.kind !== "attributeType" && typeOrSubtypePlaysRoles(type);
    }

    /** False when the target type (or any subtype) owns no attribute types —
     *  there are no attributes to load, so the action is disabled. */
    get canLoadAttributes(): boolean {
        const type = this.target ? this.lookupType(this.target) : null;
        return !!type && type.kind !== "attributeType" && typeOrSubtypeOwnsAttributes(type);
    }

    readonly noLinksTooltip = "This type has no links";
    readonly noAttributesTooltip = "This type owns no attributes";

    private lookupType(target: InspectableSelection): SchemaConcept | null {
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return null;
        }
        const kind = typeKindFromSelectionKind(target.kind);
        switch (kind) {
            case "entityType": return schema.entities[target.typeLabel] ?? null;
            case "relationType": return schema.relations[target.typeLabel] ?? null;
            case "attributeType": return schema.attributes[target.typeLabel] ?? null;
        }
    }

    /** Walk the graph, gathering IIDs of every entity/relation node whose
     *  concept type label matches. */
    private collectInstanceIidsOfType(typeLabel: string): string[] {
        if (!this.visualiser) return [];
        const out: string[] = [];
        const seen = new Set<string>();
        this.visualiser.graph.nodes().forEach(key => {
            try {
                const concept = this.visualiser!.graph.getNodeAttributes(key)?.["metadata"]?.concept;
                if (!concept) return;
                if ((concept.kind === "entity" || concept.kind === "relation")
                    && concept.type?.label === typeLabel
                    && concept.iid
                    && !seen.has(concept.iid)) {
                    seen.add(concept.iid);
                    out.push(concept.iid);
                }
            } catch { /* missing metadata mid-mutation */ }
        });
        return out;
    }

    private runFetch(op: (state: GraphViewState) => Promise<void>): void {
        if (!this.run || !this.visualiser) return;
        this.visualiser.freezeViewport();
        op(this.graphViewState).then(() => {
            this.visualiser?.reheat({ soft: true, preserveCamera: true });
            this.visualiser?.interactionHandler.recomputeHighlightSet();
        });
    }
}
