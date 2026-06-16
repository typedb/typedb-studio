import type { GraphVisualiser } from "./index";
import Sigma from "sigma";
import MultiGraph from "graphology";
import chroma from "chroma-js";
import { BehaviorSubject, Subject } from "rxjs";
import {SigmaEventPayload, SigmaNodeEventPayload, SigmaStageEventPayload} from "sigma/types";
import {GraphStyles} from "./styles";
import {LayoutWrapper} from "./layout";
import type {GraphStyleService} from "../../../service/graph-style.service";
import type { SelectionMode } from "../../../service/graph-view-state.service";

/**
 * Selection payload emitted when a graph instance node is clicked. Carries
 * just enough info for an upstream consumer (e.g. the Inspector) to look up
 * the full SchemaConcept via the application's schema state. Type nodes
 * (entityType / relationType / attributeType) are not surfaced — only
 * concrete instances are inspectable.
 */
export interface InspectableSelection {
    kind: "entity" | "relation" | "attribute";
    typeLabel: string;
    /** For entity/relation: the IID. For attribute: the value (attributes have
     *  no IID in TypeDB; values uniquely identify them per type). */
    instanceId: string;
}

/**
 * Emitted when the user right-clicks a graph instance node — gives upstream
 * UI (the context menu) what it needs to know which instance was clicked and
 * where on screen to open the menu.
 */
export interface NodeContextMenuEvent {
    target: InspectableSelection;
    /** Viewport coordinates of the click, for positioning the menu trigger. */
    clientX: number;
    clientY: number;
}

/** Emitted when a TYPE is selected in the graph (type-mode click). */
export interface TypeSelection {
    typeKind: "entityType" | "relationType" | "attributeType";
    typeLabel: string;
}

// Ref: https://www.sigmajs.org/docs/advanced/events/
// and: https://www.sigmajs.org/storybook/?path=/story/mouse-manipulations--story

export interface StudioState {
    activeQueryDatabase: string | null;
}

interface InteractionState {
    draggedNode: string | null;
    hoveredNode: string | null;
    didDrag: boolean;
    highlightedAnswer: number | null; // demonstrative
    selectedNode: string | null;
    selectedNeighbors: Set<string> | null;
}

export class InteractionHandler {
    state: InteractionState;
    layout: LayoutWrapper | null = null;
    visualiser: GraphVisualiser | null = null;
    selection$ = new BehaviorSubject<InspectableSelection | null>(null);
    /**
     * Emitted on click when `selectionMode === "types"` — carries the type
     * of whatever was clicked. Parallel to `selection$` but for type-level
     * exploration. The two are mutually exclusive: when one fires non-null
     * the other is reset to null.
     */
    typeSelection$ = new BehaviorSubject<TypeSelection | null>(null);
    /**
     * Currently-selected type label in type-mode. Stored separately so
     * `recomputeHighlightSet` can light up every instance of this type
     * across the graph in addition to (or instead of) the BFS neighborhood.
     */
    selectedTypeLabel: string | null = null;
    /**
     * Drives `onClickNode`'s dispatch. Owned externally (the GraphTab
     * pushes the tab's `selectionMode` here on creation + on user toggle).
     * Defaults to "types" — type-level exploration is the primary flow.
     */
    selectionMode: SelectionMode = "types";
    /**
     * Fires every time the user right-clicks an instance node. Consumers
     * (typically a context-menu component) use this to open a menu at the
     * click coordinates. Type nodes don't emit — they're not actionable.
     */
    nodeContextMenu$ = new Subject<NodeContextMenuEvent>();
    /**
     * True iff the most recent selection change came from the user clicking a
     * node that was already in the previous selection's highlighted neighbor
     * set. The Inspector reads this to decide whether to extend the breadcrumb
     * trail or start a fresh one. Reset to false for programmatic selections
     * and selection clears.
     */
    lastSelectionWasFromHighlight = false;
    /**
     * Additional anchor nodes whose neighborhoods are highlighted alongside
     * the primary selection — populated by the Inspector with the node keys
     * for breadcrumb ancestors, so the union of every step in the exploration
     * chain stays lit up.
     */
    private secondaryAnchors: Set<string> = new Set();

    constructor(public graph: MultiGraph, public renderer: Sigma, private studioState: StudioState, public styleParams: GraphStyles, public styleService?: GraphStyleService) {
        this.state = {
            draggedNode : null,
            hoveredNode: null,
            didDrag: false,
            highlightedAnswer: null,
            selectedNode: null,
            selectedNeighbors: null,
        };
        this.registerAll(renderer);
        // Sigma's rightClickNode fires from the mousedown event, but the
        // browser still fires its own `contextmenu` afterwards which would
        // overlay the default browser menu on top of ours. Suppress it on
        // the canvas container so only our menu appears.
        renderer.getContainer().addEventListener("contextmenu", e => e.preventDefault());
    }

    registerAll(renderer: Sigma) {
        renderer.on(StudioSigmaEventType.enterNode, (e) => this.onEnterNode(e));
        renderer.on(StudioSigmaEventType.leaveNode, (e) => this.onLeaveNode(e));
        renderer.on(StudioSigmaEventType.moveBody, (e) => this.onMoveBody(e));
        renderer.on(StudioSigmaEventType.downNode, (e) => this.onDownNode(e));
        renderer.on(StudioSigmaEventType.upStage, (e) => this.onUpStage(e));
        renderer.on(StudioSigmaEventType.upNode, (e) => this.onUpNode(e));
        renderer.on(StudioSigmaEventType.clickNode, (e) => this.onClickNode(e));
        renderer.on(StudioSigmaEventType.clickStage, () => this.onClickStage());
        renderer.on(StudioSigmaEventType.doubleClickNode, (e) => this.onDoubleClickNode(e));
        renderer.on(StudioSigmaEventType.rightClickNode, (e) => this.onRightClickNode(e));
    }


    onEnterNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        const color = this.graph.getNodeAttribute(node, "color");
        this.graph.setNodeAttribute(node, "_originalColor", color);
        this.graph.setNodeAttribute(node, "color", chroma(color).darken(0.3).hex());

        if (this.styleService?.showHoverLabel && !this.styleService.labelsVisible) {
            const attrs = this.graph.getNodeAttributes(node);
            const concept = attrs["metadata"]?.concept;
            if (concept) {
                this.graph.setNodeAttribute(node, "label", this.styleParams.vertexDefaultLabel(concept));
            }
        }
        // Lift the hovered node to the front so its body AND label stack on top
        // of neighbours as one group (the reducer reads this). Hovering doesn't
        // change any layout-impacting attribute, so force a re-indexation to
        // rebuild the body/label draw order with the new zIndex.
        this.state.hoveredNode = node;
        this.renderer.refresh({ partialGraph: { nodes: [node] }, skipIndexation: false, schedule: true });
    }

    onLeaveNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        const original = this.graph.getNodeAttribute(node, "_originalColor");
        if (original) {
            this.graph.setNodeAttribute(node, "color", original);
            this.graph.removeNodeAttribute(node, "_originalColor");
        }

        if (this.styleService?.showHoverLabel && !this.styleService.labelsVisible) {
            this.graph.setNodeAttribute(node, "label", "");
        }
        if (this.state.hoveredNode === node) this.state.hoveredNode = null;
        this.renderer.refresh({ partialGraph: { nodes: [node] }, skipIndexation: false, schedule: true });
    }

    onDownNode(event: SigmaNodeEventPayload) {
        // Only the primary (left) mouse button initiates a drag — right-click
        // is reserved for the context menu, middle-click for browser default.
        const button = (event.event?.original as MouseEvent | undefined)?.button;
        if (button != null && button !== 0) return;
        const node = event.node;
        this.state.draggedNode = node;
        const original = this.graph.getNodeAttribute(node, "_originalColor") ?? this.graph.getNodeAttribute(node, "color");
        this.graph.setNodeAttribute(node, "color", chroma(original).darken(0.9).hex());
        const attrs = this.graph.getNodeAttributes(node);
        this.layout?.fixNode(node, attrs["x"], attrs["y"]);
        if (!this.renderer.getCustomBBox()) {
            this.renderer.setCustomBBox(this.renderer.getBBox());
        }
    }

    onMoveBody(event: SigmaStageEventPayload) {
        // On mouse move, if the drag mode is enabled, we change the position of the draggedNode
        let mouseCoords = event.event;
        if (this.state.draggedNode == null) return;

        // Get new position of node
        const pos = this.renderer.viewportToGraph(mouseCoords);
        this.graph.setNodeAttribute(this.state.draggedNode, "x", pos.x);
        this.graph.setNodeAttribute(this.state.draggedNode, "y", pos.y);
        this.layout?.fixNode(this.state.draggedNode, pos.x, pos.y);
        this.state.didDrag = true;

        // Prevent sigma to move camera:
        mouseCoords.preventSigmaDefault();
        mouseCoords.original.preventDefault();
        mouseCoords.original.stopPropagation();
    }

    onUpNode(_event: SigmaNodeEventPayload) {
        if (this.state.draggedNode != null) {
            this.layout?.unfixNode(this.state.draggedNode);
            const original = this.graph.getNodeAttribute(this.state.draggedNode, "_originalColor");
            if (original) {
                this.graph.setNodeAttribute(this.state.draggedNode, "color", chroma(original).darken(0.3).hex());
            }
            this.state.draggedNode = null;
        }
    }

    onUpStage(_event: SigmaEventPayload) {
        if (this.state.draggedNode != null) {
            this.layout?.unfixNode(this.state.draggedNode);
            const original = this.graph.getNodeAttribute(this.state.draggedNode, "_originalColor");
            if (original) {
                this.graph.setNodeAttribute(this.state.draggedNode, "color", original);
                this.graph.removeNodeAttribute(this.state.draggedNode, "_originalColor");
            }
            this.state.draggedNode = null;
        }
    }

    onClickNode(event: SigmaNodeEventPayload) {
        if (this.state.didDrag) {
            this.state.didDrag = false;
            return;
        }
        this.visualiser?.clearSearch();
        const node = event.node;
        if (this.selectionMode === "types") this.handleTypeSelectionClick(node);
        else this.handleInstanceSelectionClick(node);
    }

    private handleInstanceSelectionClick(node: string) {
        if (this.state.selectedNode === node && this.selectedTypeLabel == null) return; // no-op re-click
        this.lastSelectionWasFromHighlight = this.state.selectedNeighbors?.has(node) ?? false;
        this.state.selectedNode = node;
        this.selectedTypeLabel = null;
        this.recomputeHighlightSet();
        this.selection$.next(this.extractInspectableSelection(node));
        if (this.typeSelection$.value !== null) this.typeSelection$.next(null);
    }

    private handleTypeSelectionClick(node: string) {
        const concept = this.safeReadConcept(node);
        if (!concept) return;
        const ext = this.extractTypeFromConcept(concept);
        if (!ext) return;
        if (this.selectedTypeLabel === ext.typeLabel) return; // no-op re-click on same type
        this.lastSelectionWasFromHighlight = false;
        this.state.selectedNode = node; // populated so the existing reducer fade path runs
        this.selectedTypeLabel = ext.typeLabel;
        this.recomputeHighlightSet();
        this.typeSelection$.next({ typeKind: ext.typeKind, typeLabel: ext.typeLabel });
        if (this.selection$.value !== null) this.selection$.next(null);
    }

    /**
     * Update the click-dispatch mode and clear any current selection — the
     * two modes select different things, so the previous selection is no
     * longer meaningful when the user switches.
     */
    setSelectionMode(mode: SelectionMode): void {
        if (this.selectionMode === mode) return;
        this.selectionMode = mode;
        this.clearSelection();
    }

    private safeReadConcept(node: string): any | null {
        try {
            return this.graph.getNodeAttributes(node)?.["metadata"]?.concept ?? null;
        } catch { return null; }
    }

    /**
     * Pull a `{ typeKind, typeLabel }` pair out of a concept regardless of
     * whether the clicked node is an instance (entity/relation/attribute) or
     * a schema-type node (entityType/relationType/attributeType).
     */
    private extractTypeFromConcept(concept: any): { typeKind: "entityType" | "relationType" | "attributeType"; typeLabel: string } | null {
        switch (concept?.kind) {
            case "entity":     return concept.type?.label ? { typeKind: "entityType",    typeLabel: concept.type.label } : null;
            case "relation":   return concept.type?.label ? { typeKind: "relationType",  typeLabel: concept.type.label } : null;
            case "attribute":  return concept.type?.label ? { typeKind: "attributeType", typeLabel: concept.type.label } : null;
            case "entityType":    return concept.label ? { typeKind: "entityType",    typeLabel: concept.label } : null;
            case "relationType":  return concept.label ? { typeKind: "relationType",  typeLabel: concept.label } : null;
            case "attributeType": return concept.label ? { typeKind: "attributeType", typeLabel: concept.label } : null;
            default: return null;
        }
    }

    private extractInspectableSelection(node: string): InspectableSelection | null {
        try {
            const concept = this.graph.getNodeAttributes(node)?.["metadata"]?.concept;
            if (!concept) return null;
            if (concept.kind === "entity" || concept.kind === "relation") {
                if (!concept.iid || !concept.type?.label) return null;
                return { kind: concept.kind, typeLabel: concept.type.label, instanceId: concept.iid };
            }
            if (concept.kind === "attribute") {
                if (concept.value == null || !concept.type?.label) return null;
                return { kind: "attribute", typeLabel: concept.type.label, instanceId: String(concept.value) };
            }
            return null;
        } catch {
            return null;
        }
    }

    /**
     * Recursively collects neighbors to highlight when a node is selected.
     *
     * Starting from the clicked node's direct neighbors:
     * - Entity / entityType: also highlight its connected attributes / attributeTypes
     * - Relation / relationType: also highlight all its connected concepts (entities, relations, attributes / their type equivalents)
     * - Attribute / attributeType / value: only the direct owners — we do NOT
     *   expand to the owners' other attributes, since that would highlight
     *   most of the graph any time a common attribute (e.g. a name) is clicked.
     *
     * Recurses up to maxDepth (4) to follow relation chains without blowing up.
     */
    private collectHighlightedNeighbors(root: string, maxDepth = 4): Set<string> {
        const highlighted = new Set<string>();
        const rootKind = this.getNodeKind(root);

        // Seed with direct neighbors of the root
        const directNeighbors = this.graph.neighbors(root);
        const queue: { node: string; depth: number }[] = [];
        for (const neighbor of directNeighbors) {
            if (!highlighted.has(neighbor)) {
                highlighted.add(neighbor);
                queue.push({ node: neighbor, depth: 1 });
            }
        }

        // Attribute clicks stop at direct owners — no further expansion.
        if (rootKind === "attribute" || rootKind === "attributeType") {
            return highlighted;
        }

        while (queue.length > 0) {
            const { node, depth } = queue.shift()!;
            if (depth >= maxDepth) continue;

            const kind = this.getNodeKind(node);
            let shouldExpand = false;
            let expandFilter: ((neighborKind: string | null) => boolean) | null = null;

            if (kind === "entity" || kind === "entityType") {
                // Expand to connected attributes/attributeTypes only
                shouldExpand = true;
                expandFilter = (nk) => nk === "attribute" || nk === "attributeType";
            } else if (kind === "relation" || kind === "relationType") {
                // Expand to all connected concepts
                shouldExpand = true;
                expandFilter = () => true;
            }

            if (shouldExpand) {
                for (const neighbor of this.graph.neighbors(node)) {
                    if (neighbor === root || highlighted.has(neighbor)) continue;
                    const neighborKind = this.getNodeKind(neighbor);
                    if (expandFilter!(neighborKind)) {
                        highlighted.add(neighbor);
                        queue.push({ node: neighbor, depth: depth + 1 });
                    }
                }
            }
        }

        return highlighted;
    }

    private getNodeKind(node: string): string | null {
        try {
            return this.graph.getNodeAttributes(node)?.["metadata"]?.concept?.kind ?? null;
        } catch {
            return null;
        }
    }

    onClickStage() {
        if (this.state.selectedNode != null) {
            this.clearSelection();
        }
    }

    clearSelection() {
        this.state.selectedNode = null;
        this.selectedTypeLabel = null;
        this.lastSelectionWasFromHighlight = false;
        this.recomputeHighlightSet();
        this.selection$.next(null);
        this.typeSelection$.next(null);
    }

    /**
     * Programmatically select a node — same effect as clicking it: highlight
     * ring, neighbor fade, and selection$ emission for the Inspector.
     */
    selectNode(node: string): void {
        if (this.state.selectedNode === node) return;
        this.lastSelectionWasFromHighlight = false;
        this.state.selectedNode = node;
        this.recomputeHighlightSet();
        this.selection$.next(this.extractInspectableSelection(node));
    }

    /**
     * Switch the highlight to a specific instance regardless of selection
     * mode — used by the "load connections" flows (context menu, instance
     * inspector) so the just-touched instance becomes the focus even when
     * the panel is in type-selection mode. Clears any active type selection
     * (and notifies the type-selection stream) so `recomputeHighlightSet`
     * runs the instance-mode branch.
     */
    focusInstance(node: string): void {
        const typeWasSet = this.selectedTypeLabel != null;
        this.lastSelectionWasFromHighlight = false;
        this.selectedTypeLabel = null;
        this.state.selectedNode = node;
        this.recomputeHighlightSet();
        this.selection$.next(this.extractInspectableSelection(node));
        if (typeWasSet) this.typeSelection$.next(null);
    }

    /**
     * Highlight every instance of `node`'s type — the type-mode counterpart to
     * `focusInstance`. Used after a context-menu "every '<type>'" load so the
     * highlight covers all instances the connections were loaded for, not just
     * the originally-clicked one. Works regardless of the current selection
     * mode and clears any active instance selection. `node` is just a
     * representative used to read the concept's type.
     */
    focusType(node: string): void {
        const concept = this.safeReadConcept(node);
        if (!concept) return;
        const ext = this.extractTypeFromConcept(concept);
        if (!ext) return;
        this.lastSelectionWasFromHighlight = false;
        this.state.selectedNode = node;
        this.selectedTypeLabel = ext.typeLabel;
        this.recomputeHighlightSet();
        this.typeSelection$.next({ typeKind: ext.typeKind, typeLabel: ext.typeLabel });
        if (this.selection$.value !== null) this.selection$.next(null);
    }

    /**
     * Replace the set of secondary anchor nodes — additional nodes whose
     * neighborhoods should also stay highlighted alongside the primary
     * selection. The Inspector calls this with the breadcrumb ancestors'
     * node keys whenever the trail changes.
     */
    setSecondaryAnchors(anchors: Set<string>): void {
        this.secondaryAnchors = anchors;
        this.recomputeHighlightSet();
    }

    /**
     * Rebuild `selectedNeighbors` as the union of:
     *   - the primary selection's highlighted neighbors, and
     *   - each secondary anchor (itself) and its highlighted neighbors.
     * Leaves it null when nothing is selected and no anchors are set so the
     * reducer treats the graph as un-highlighted (no fade).
     *
     * Public so callers that mutate the graph (e.g. bulk-add fetches in the
     * Inspector) can ask for the highlight to re-evaluate against the new
     * node set without having to change the selection.
     */
    recomputeHighlightSet(): void {
        if (this.state.selectedNode == null && this.secondaryAnchors.size === 0 && this.selectedTypeLabel == null) {
            this.state.selectedNeighbors = null;
            this.renderer.refresh();
            return;
        }
        const all = new Set<string>();
        if (this.selectedTypeLabel != null) {
            // Type-selection: every node sharing the selected type label is in
            // the highlight set, plus each of their highlighted neighborhoods.
            // Done in two passes so that ALL matching instances get added
            // first, independent of the BFS results (which only finds
            // *neighbors* of each instance — instances with no edges yet
            // would otherwise only show up implicitly via the early
            // `all.add(node)`, which we keep but make explicit here).
            const target = this.selectedTypeLabel;
            const typeMembers: string[] = [];
            this.graph.nodes().forEach(node => {
                const concept = this.safeReadConcept(node);
                if (!concept) return;
                const ext = this.extractTypeFromConcept(concept);
                if (ext?.typeLabel !== target) return;
                typeMembers.push(node);
                all.add(node);
            });
            typeMembers.forEach(node => {
                this.collectHighlightedNeighbors(node).forEach(n => all.add(n));
            });
        } else if (this.state.selectedNode != null) {
            // Instance-selection: BFS from the primary node only.
            this.collectHighlightedNeighbors(this.state.selectedNode).forEach(n => all.add(n));
        }
        this.secondaryAnchors.forEach(anchor => {
            all.add(anchor);
            this.collectHighlightedNeighbors(anchor).forEach(n => all.add(n));
        });
        // Defensive: the primary selected node should always be in the
        // highlight set. (In type-mode it's already covered by the iteration
        // above; in instance-mode the reducer's `node === state.selectedNode`
        // path covers it. Explicit add here in case either of those paths
        // misses for an unexpected reason.)
        if (this.state.selectedNode != null) all.add(this.state.selectedNode);
        this.state.selectedNeighbors = all;
        this.renderer.refresh();
    }

    onDoubleClickNode(event: SigmaNodeEventPayload) {
    }

    onRightClickNode(event: SigmaNodeEventPayload) {
        // Sigma's right-click event already preventsDefault on the underlying
        // mousedown event so the browser context menu won't show — we just
        // need to surface the target + coordinates.
        const target = this.extractInspectableSelection(event.node);
        if (!target) return; // type nodes etc. aren't actionable
        const mouseEvent = event.event?.original as MouseEvent | undefined;
        if (!mouseEvent) return;
        this.nodeContextMenu$.next({
            target,
            clientX: mouseEvent.clientX,
            clientY: mouseEvent.clientY,
        });
    }

    highlightAnswer(answerIndex: number) {
        // TODO: Maybe add indexing so I don't have to iterate
        if (this.state.highlightedAnswer != null) {
            this.removeHighlightFromAnswer(this.state.highlightedAnswer);
            this.state.highlightedAnswer = null;
        }
        this.graph.edges().forEach(edge => {
            if (answerIndex == this.graph.getEdgeAttributes(edge)["metadata"].answerIndex) {
                this.graph.setEdgeAttribute(edge, "color", this.styleParams.edgeHighlightColor.hex());
            }
        })
        this.state.highlightedAnswer = answerIndex;
    }

    removeHighlightFromAnswer(answerIndex: number) {
        // TODO: Maybe add indexing so I don't have to iterate
        this.graph.edges().forEach(edge => {
            const metadata = this.graph.getEdgeAttributes(edge)["metadata"];
            if (answerIndex == metadata.answerIndex) {
                const tag = metadata.dataEdge.tag;
                const color = this.styleParams.edgeLabelColors?.[tag]
                    ?? this.styleParams.edgeColor.hex();
                this.graph.setEdgeAttribute(edge, "color", color);
            }
        })
    }

}

enum StudioSigmaEventType {
    enterNode = "enterNode",
    leaveNode = "leaveNode",
    downNode = "downNode",
    upNode = "upNode",
    clickNode = "clickNode",
    rightClickNode = "rightClickNode",
    doubleClickNode = "doubleClickNode",
    wheelNode = "wheelNode",

    enterEdge = "enterEdge",
    leaveEdge = "leaveEdge",
    downEdge = "downEdge",
    clickEdge = "clickEdge",
    rightClickEdge = "rightClickEdge",
    doubleClickEdge = "doubleClickEdge",
    wheelEdge = "wheelEdge",

    moveBody = "moveBody",
    upStage = "upStage",
    clickStage = "clickStage",

    // Remaining: downStage, rightClickStage, doubleClickStage, wheelStage
    // Remaining: beforeRender, afterRender, resize, kill
}
