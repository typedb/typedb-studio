/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { BehaviorSubject, map } from "rxjs";
import { SchemaState, Schema, SchemaAttribute, SchemaRole, SchemaConcept } from "./schema-state.service";
import { Injectable } from "@angular/core";
import { AppData } from "./app-data.service";

export type SchemaTreeNodeKind = "root" | "concept" | "link";

export interface SchemaTreeNodeBase {
    nodeKind: SchemaTreeNodeKind;
    children?: SchemaTreeNode[];
    /** Stable identifier for expansion tracking and `cdkVirtualFor` trackBy. */
    key: string;
    /** Precomputed CSS class string applied to the row host. */
    cssClass: string;
    clickable: boolean;
}

export interface SchemaTreeRootNode extends SchemaTreeNodeBase {
    nodeKind: "root";
    label: "entities" | "relations" | "attributes";
    children: SchemaTreeConceptNode[];
}

export interface SchemaTreeConceptNode extends SchemaTreeNodeBase {
    nodeKind: "concept";
    concept: SchemaConcept;
    children: SchemaTreeChildNode[];
}

export type SchemaTreeLinkKind = "sub" | "owns" | "plays" | "relates";

export interface SchemaTreeLinkNodeBase extends SchemaTreeNodeBase {
    nodeKind: "link";
    linkKind: SchemaTreeLinkKind;
}

export interface SchemaTreeSubLinkNode extends SchemaTreeLinkNodeBase {
    linkKind: "sub";
    supertype: SchemaConcept;
}

export interface SchemaTreeOwnsLinkNode extends SchemaTreeLinkNodeBase {
    linkKind: "owns";
    ownedAttribute: SchemaAttribute;
}

export interface SchemaTreePlaysLinkNode extends SchemaTreeLinkNodeBase {
    linkKind: "plays";
    role: SchemaRole;
}

export interface SchemaTreeRelatesLinkNode extends SchemaTreeLinkNodeBase {
    linkKind: "relates";
    role: SchemaRole;
}

export type SchemaTreeLinkNode = SchemaTreeSubLinkNode | SchemaTreeOwnsLinkNode | SchemaTreePlaysLinkNode | SchemaTreeRelatesLinkNode;

export type SchemaTreeNode = SchemaTreeRootNode | SchemaTreeConceptNode | SchemaTreeLinkNode;

export type SchemaTreeChildNode = SchemaTreeConceptNode | SchemaTreeLinkNode;

/** A node in the flattened render list. Only this gets handed to `cdkVirtualFor`. */
export interface FlatSchemaTreeNode {
    node: SchemaTreeNode;
    level: number;
    expandable: boolean;
    expanded: boolean;
}

function conceptCssClass(_node: SchemaTreeConceptNode): string {
    return "concept";
}

function rootCssClass(label: string): string {
    return `root ${label}`;
}

function linkCssClass(link: SchemaTreeLinkNode): string {
    return `link ${link.linkKind}${link.linkKind === "sub" ? ` ${link.supertype.kind}` : ""}`;
}

@Injectable({
    providedIn: "root",
})
export class SchemaToolWindowState {
    /** Hierarchical source — kept for any external consumer; rendering uses {@link flatNodes$}. */
    dataSource$ = new BehaviorSubject<SchemaTreeRootNode[]>([]);
    /** Flattened, render-ready list. Recomputed whenever the hierarchy, link visibility, or expansion changes. */
    flatNodes$ = new BehaviorSubject<FlatSchemaTreeNode[]>([]);
    isEmpty$ = this.dataSource$.pipe(map(data => data.length > 0 && data.every(root => !root.children.length)));
    viewMode$ = new BehaviorSubject<"flat" | "hierarchical">(this.appData.viewState.schemaToolWindowState().viewMode);
    linksVisibility$ = new BehaviorSubject<Record<SchemaTreeLinkKind, boolean>>(this.appData.viewState.schemaToolWindowState().linksVisibility);
    rootNodesCollapsed: Record<SchemaTreeRootNode["label"], boolean> = this.appData.viewState.schemaToolWindowState().rootNodesCollapsed;
    highlightedConceptLabel$ = new BehaviorSubject<string | null>(null);

    /** Live search query that filters the tree to concept nodes whose label
     *  contains it (case-insensitive). Setting an empty string disables the
     *  filter and restores the user's existing expansion state — the search
     *  overlay never mutates `expandedKeys`. */
    searchQuery$ = new BehaviorSubject<string>("");

    /** Keys of currently-expanded nodes. */
    private expandedKeys = new Set<string>();

    constructor(public schema: SchemaState, private appData: AppData) {
        schema.value$.subscribe(() => {
            this.buildView();
        });
        this.viewMode$.subscribe(() => {
            this.buildView();
        });
        this.linksVisibility$.subscribe(() => {
            this.rebuildFlatNodes();
        });
        this.searchQuery$.subscribe(() => {
            this.rebuildFlatNodes();
        });
    }

    isExpanded(node: SchemaTreeNode): boolean {
        return this.expandedKeys.has(node.key);
    }

    toggleExpand(node: SchemaTreeNode): void {
        if (this.expandedKeys.has(node.key)) {
            this.expandedKeys.delete(node.key);
            if (node.nodeKind === "root") {
                this.rootNodesCollapsed[node.label] = true;
                this.persistRootCollapse();
            }
        } else {
            this.expandedKeys.add(node.key);
            if (node.nodeKind === "root") {
                this.rootNodesCollapsed[node.label] = false;
                this.persistRootCollapse();
            }
        }
        this.rebuildFlatNodes();
    }

    private persistRootCollapse() {
        const state = this.appData.viewState.schemaToolWindowState();
        state.rootNodesCollapsed = this.rootNodesCollapsed;
        this.appData.viewState.setSchemaToolWindowState(state);
    }

    private visibleChildren(node: SchemaTreeNode): SchemaTreeNode[] {
        if (!node.children) return [];
        if (node.nodeKind === "root") return node.children;
        const vis = this.linksVisibility$.value;
        return node.children.filter(child => child.nodeKind !== "link" || vis[child.linkKind]);
    }

    private rebuildFlatNodes() {
        const query = this.searchQuery$.value.trim().toLowerCase();
        const searchActive = query.length > 0;
        const flat: FlatSchemaTreeNode[] = [];

        if (searchActive) {
            // Search mode. Only concept nodes are matched (per spec — "we
            // only search types, not capabilities like owns"). A subtree is
            // kept iff it contains at least one matching concept; matching
            // ancestors auto-expand so the path to every match is revealed.
            // Link nodes are filtered out entirely.
            const matchesQuery = (node: SchemaTreeNode): boolean =>
                node.nodeKind === "concept" && node.concept.label.toLowerCase().includes(query);

            const subtreeHasMatch = new Map<string, boolean>();
            const computeHasMatch = (node: SchemaTreeNode): boolean => {
                const cached = subtreeHasMatch.get(node.key);
                if (cached !== undefined) return cached;
                if (node.nodeKind === "link") { subtreeHasMatch.set(node.key, false); return false; }
                let any = matchesQuery(node);
                for (const child of node.children ?? []) {
                    if (computeHasMatch(child)) any = true;
                }
                subtreeHasMatch.set(node.key, any);
                return any;
            };
            for (const root of this.dataSource$.value) computeHasMatch(root);

            const visit = (node: SchemaTreeNode, level: number) => {
                if (node.nodeKind === "link") return;
                if (!subtreeHasMatch.get(node.key)) return;
                const keptChildren = (node.children ?? []).filter(c =>
                    c.nodeKind !== "link" && (subtreeHasMatch.get(c.key) ?? false));
                const expandable = keptChildren.length > 0;
                flat.push({ node, level, expandable, expanded: expandable });
                for (const child of keptChildren) visit(child, level + 1);
            };
            for (const root of this.dataSource$.value) visit(root, 0);
            this.flatNodes$.next(flat);
            return;
        }

        // Non-search: original behaviour, driven by the user's expandedKeys.
        const visit = (node: SchemaTreeNode, level: number) => {
            const children = this.visibleChildren(node);
            const expandable = children.length > 0;
            const expanded = expandable && this.expandedKeys.has(node.key);
            flat.push({ node, level, expandable, expanded });
            if (expanded) {
                for (const child of children) visit(child, level + 1);
            }
        };
        for (const root of this.dataSource$.value) visit(root, 0);
        this.flatNodes$.next(flat);
    }

    private buildView() {
        const schema = this.schema.value$.value;
        const linksVisibility = this.linksVisibility$.value;

        if (!schema) {
            this.dataSource$.next([]);
            this.flatNodes$.next([]);
            return;
        }

        const data: SchemaTreeRootNode[] = [
            this.buildRoot("entities", schema.entities, (x) => [
                ...(x.supertype ? [this.buildSubLink(x, x.supertype, linksVisibility.sub)] : []),
                ...x.ownedAttributes.sort((a, b) => a.label.localeCompare(b.label)).map(y => this.buildOwnsLink(x, y, linksVisibility.owns)),
                ...x.playedRoles.sort((a, b) => a.label.localeCompare(b.label)).map(y => this.buildPlaysLink(x, y, linksVisibility.plays)),
            ]),
            this.buildRoot("relations", schema.relations, (x) => [
                ...(x.supertype ? [this.buildSubLink(x, x.supertype, linksVisibility.sub)] : []),
                ...x.relatedRoles.sort((a, b) => a.label.localeCompare(b.label)).map(y => this.buildRelatesLink(x, y, linksVisibility.relates)),
                ...x.ownedAttributes.sort((a, b) => a.label.localeCompare(b.label)).map(y => this.buildOwnsLink(x, y, linksVisibility.owns)),
                ...x.playedRoles.sort((a, b) => a.label.localeCompare(b.label)).map(y => this.buildPlaysLink(x, y, linksVisibility.plays)),
            ]),
            this.buildRoot("attributes", schema.attributes, (x) => [
                ...(x.supertype ? [this.buildSubLink(x, x.supertype, linksVisibility.sub)] : []),
            ]),
        ];

        if (this.viewMode$.value === "hierarchical") {
            const nodeMap = Object.fromEntries(data.flatMap(rootNode => rootNode.children.map(conceptNode => ([
                conceptNode.concept.label, conceptNode
            ]))));

            data.forEach(rootNode => {
                rootNode.children.forEach(conceptNode => {
                    const subNode = conceptNode.children.find(node => node.nodeKind === "link" && node.linkKind === "sub");
                    if (subNode) {
                        const superNode = nodeMap[(subNode as SchemaTreeSubLinkNode).supertype.label];
                        if (!superNode) throw new Error(`Missing supertype node for ${(subNode as SchemaTreeSubLinkNode).supertype.label}`);
                        superNode.children.unshift(conceptNode);
                    }
                });
            });

            data.flatMap(rootNode => rootNode.children).forEach(conceptNode => {
                conceptNode.children = conceptNode.children.filter(child => child.nodeKind !== "link" || child.linkKind !== "sub");
            });

            data.forEach(x => x.children = x.children.filter(conceptNode => !conceptNode.concept.supertype));
        }

        // Seed expanded set: roots whose collapsed pref is `false` (i.e. expanded).
        // We don't carry over concept-level expansion across schema rebuilds.
        this.expandedKeys = new Set();
        for (const root of data) {
            if (!this.rootNodesCollapsed[root.label]) this.expandedKeys.add(root.key);
        }

        this.dataSource$.next(data);
        this.rebuildFlatNodes();
    }

    private buildRoot<T extends SchemaConcept>(
        label: SchemaTreeRootNode["label"],
        concepts: Record<string, T>,
        buildChildren: (c: T) => SchemaTreeChildNode[],
    ): SchemaTreeRootNode {
        return {
            nodeKind: "root",
            label,
            key: `root:${label}`,
            cssClass: rootCssClass(label),
            clickable: true,
            children: Object.values(concepts).sort((a, b) => a.label.localeCompare(b.label)).map(x => {
                const conceptNode: SchemaTreeConceptNode = {
                    nodeKind: "concept",
                    concept: x,
                    key: `concept:${x.label}`,
                    cssClass: conceptCssClass(null as any),
                    clickable: true,
                    children: buildChildren(x),
                };
                return conceptNode;
            }),
        };
    }

    private buildSubLink(owner: SchemaConcept, supertype: SchemaConcept, visible: boolean): SchemaTreeSubLinkNode {
        const node: SchemaTreeSubLinkNode = {
            nodeKind: "link",
            linkKind: "sub",
            supertype,
            key: `sub:${owner.label}:${supertype.label}`,
            cssClass: "",
            clickable: false,
        };
        node.cssClass = linkCssClass(node);
        return node;
    }

    private buildOwnsLink(owner: SchemaConcept, ownedAttribute: SchemaAttribute, visible: boolean): SchemaTreeOwnsLinkNode {
        const node: SchemaTreeOwnsLinkNode = {
            nodeKind: "link",
            linkKind: "owns",
            ownedAttribute,
            key: `owns:${owner.label}:${ownedAttribute.label}`,
            cssClass: "",
            clickable: false,
        };
        node.cssClass = linkCssClass(node);
        return node;
    }

    private buildPlaysLink(owner: SchemaConcept, role: SchemaRole, visible: boolean): SchemaTreePlaysLinkNode {
        const node: SchemaTreePlaysLinkNode = {
            nodeKind: "link",
            linkKind: "plays",
            role,
            key: `plays:${owner.label}:${role.label}`,
            cssClass: "",
            clickable: false,
        };
        node.cssClass = linkCssClass(node);
        return node;
    }

    private buildRelatesLink(owner: SchemaConcept, role: SchemaRole, visible: boolean): SchemaTreeRelatesLinkNode {
        const node: SchemaTreeRelatesLinkNode = {
            nodeKind: "link",
            linkKind: "relates",
            role,
            key: `relates:${owner.label}:${role.label}`,
            cssClass: "",
            clickable: false,
        };
        node.cssClass = linkCssClass(node);
        return node;
    }

    useFlatView() {
        this.viewMode$.next("flat");
        const state = this.appData.viewState.schemaToolWindowState();
        state.viewMode = "flat";
        this.appData.viewState.setSchemaToolWindowState(state);
    }

    useHierarchicalView() {
        this.viewMode$.next("hierarchical");
        const state = this.appData.viewState.schemaToolWindowState();
        state.viewMode = "hierarchical";
        this.appData.viewState.setSchemaToolWindowState(state);
    }

    toggleLinksVisibility(linkKind: SchemaTreeLinkKind) {
        this.linksVisibility$.next({
            ...this.linksVisibility$.value,
            [linkKind]: !this.linksVisibility$.value[linkKind]
        });
        const state = this.appData.viewState.schemaToolWindowState();
        state.linksVisibility = this.linksVisibility$.value;
        this.appData.viewState.setSchemaToolWindowState(state);
    }

    showAllLinks() {
        this.linksVisibility$.next({ sub: true, owns: true, relates: true, plays: true });
        const state = this.appData.viewState.schemaToolWindowState();
        state.linksVisibility = this.linksVisibility$.value;
        this.appData.viewState.setSchemaToolWindowState(state);
    }

    hideAllLinks() {
        this.linksVisibility$.next({ sub: false, owns: false, relates: false, plays: false });
        const state = this.appData.viewState.schemaToolWindowState();
        state.linksVisibility = this.linksVisibility$.value;
        this.appData.viewState.setSchemaToolWindowState(state);
    }

    areAllLinksVisible(): boolean {
        const v = this.linksVisibility$.value;
        return v.sub && v.owns && v.relates && v.plays;
    }

    areAllLinksHidden(): boolean {
        const v = this.linksVisibility$.value;
        return !v.sub && !v.owns && !v.relates && !v.plays;
    }

    expandAll() {
        // Expand every node in the current hierarchy.
        const visit = (node: SchemaTreeNode) => {
            this.expandedKeys.add(node.key);
            for (const child of node.children ?? []) visit(child);
        };
        for (const root of this.dataSource$.value) visit(root);
        for (const root of this.dataSource$.value) this.rootNodesCollapsed[root.label] = false;
        this.persistRootCollapse();
        this.rebuildFlatNodes();
    }

    collapseAll() {
        // Collapse everything except the roots — matches the previous behaviour.
        this.expandedKeys.clear();
        for (const root of this.dataSource$.value) {
            this.expandedKeys.add(root.key);
            this.rootNodesCollapsed[root.label] = false;
        }
        this.persistRootCollapse();
        this.rebuildFlatNodes();
    }
}
