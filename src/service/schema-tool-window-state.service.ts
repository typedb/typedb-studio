/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { BehaviorSubject, Subject } from "rxjs";
import { SchemaState, Schema, SchemaAttribute, SchemaRole, SchemaConcept } from "./schema-state.service";
import { Injectable } from "@angular/core";
import { AppData } from "./app-data.service";

export type SchemaTreeNodeKind = "root" | "concept" | "link";

export interface SchemaTreeNodeBase {
    nodeKind: SchemaTreeNodeKind;
    children?: SchemaTreeNode[];
    visible: boolean;
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

@Injectable({
    providedIn: "root",
})
export class SchemaToolWindowState {
    dataSource$ = new BehaviorSubject<SchemaTreeRootNode[]>([]);
    viewMode$ = new BehaviorSubject<"flat" | "hierarchical">(this.appData.viewState.schemaToolWindowState().viewMode);
    linksVisibility$ = new BehaviorSubject<Record<SchemaTreeLinkKind, boolean>>(this.appData.viewState.schemaToolWindowState().linksVisibility);
    rootNodesCollapsed: Record<SchemaTreeRootNode["label"], boolean> = this.appData.viewState.schemaToolWindowState().rootNodesCollapsed;
    highlightedConceptLabel$ = new BehaviorSubject<string | null>(null);

    constructor(public schema: SchemaState, private appData: AppData) {
        schema.value$.subscribe(() => {
            this.buildView();
        });
        this.viewMode$.subscribe(() => {
            this.buildView();
        });
        this.linksVisibility$.subscribe(() => {
            for (const conceptNode of this.dataSource$.value.flatMap(x => x.children)) {
                this.updateViewVisibility(conceptNode);
            }
        });
    }

    hasChild = (_: number, node: SchemaTreeNode) => !!node.children?.length;

    childrenAccessor = (node: SchemaTreeNode): SchemaTreeNode[] => {
        return (node.children || []).filter(child => child.visible === true);
    };

    private buildView() {
        const schema = this.schema.value$.value;
        const linksVisibility = this.linksVisibility$.value;

        if (!schema) {
            this.dataSource$.next([]);
            return;
        }

        const data: SchemaTreeRootNode[] = [{
            nodeKind: "root",
            label: "entities",
            visible: true,
            children: Object.values(schema.entities).sort((a, b) => a.label.localeCompare(b.label)).map(x => ({
                nodeKind: "concept",
                concept: x,
                visible: true,
                children: ([
                    ...(x.supertype ? [{ nodeKind: "link", linkKind: "sub", supertype: x.supertype, visible: linksVisibility.sub }] : []),
                    ...x.ownedAttributes.map(y => ({ nodeKind: "link", linkKind: "owns", ownedAttribute: y, visible: linksVisibility.owns })),
                    ...x.playedRoles.map(y => ({ nodeKind: "link", linkKind: "plays", role: y, visible: linksVisibility.plays })),
                ] as SchemaTreeChildNode[]),
            })),
        }, {
            nodeKind: "root",
            label: "relations",
            visible: true,
            children: Object.values(schema.relations).sort((a, b) => a.label.localeCompare(b.label)).map(x => ({
                nodeKind: "concept",
                concept: x,
                visible: true,
                children: ([
                    ...(x.supertype ? [{ nodeKind: "link", linkKind: "sub", supertype: x.supertype, visible: linksVisibility.sub }] : []),
                    ...x.relatedRoles.map(y => ({ nodeKind: "link", linkKind: "relates", role: y, visible: linksVisibility.relates })),
                    ...x.ownedAttributes.map(y => ({ nodeKind: "link", linkKind: "owns", ownedAttribute: y, visible: linksVisibility.owns })),
                    ...x.playedRoles.map(y => ({ nodeKind: "link", linkKind: "plays", role: y, visible: linksVisibility.plays })),
                ] as SchemaTreeChildNode[]),
            })),
        }, {
            nodeKind: "root",
            label: "attributes",
            visible: true,
            children: Object.values(schema.attributes).sort((a, b) => a.label.localeCompare(b.label)).map(x => ({
                nodeKind: "concept",
                concept: x,
                visible: true,
                children: ([
                    ...(x.supertype ? [{ nodeKind: "link", linkKind: "sub", supertype: x.supertype, visible: linksVisibility.sub }] : []),
                ] as SchemaTreeChildNode[]),
            })),
        }];

        if (this.viewMode$.value === "hierarchical") {
            const nodeMap = Object.fromEntries(data.flatMap(rootNode => rootNode.children.map(conceptNode => ([
                conceptNode.concept.label, conceptNode
            ]))));

            data.forEach(rootNode => {
                rootNode.children.forEach(conceptNode => {
                    const subNode = conceptNode.children.find(node => node.nodeKind === "link" && node.linkKind === "sub");
                    if (subNode) {
                        const superNode = nodeMap[subNode.supertype.label];
                        if (!superNode) throw new Error(`Missing supertype node for ${subNode.supertype.label} (nodeMap is: ${JSON.stringify(nodeMap)})`);
                        superNode.children.unshift(conceptNode);
                    }
                });
            });

            data.flatMap(rootNode => rootNode.children).forEach(conceptNode => {
                conceptNode.children = conceptNode.children.filter(child => child.nodeKind !== "link" || child.linkKind !== "sub");
            });

            data.forEach(x => x.children = x.children.filter(conceptNode => !conceptNode.concept.supertype));
        }

        this.dataSource$.next(data);
    }

    private updateViewVisibility(conceptNode: SchemaTreeConceptNode) {
        for (const childNode of conceptNode.children) {
            switch (childNode.nodeKind) {
                case "link":
                    childNode.visible = this.linksVisibility$.value[childNode.linkKind];
                    break;
                case "concept":
                    this.updateViewVisibility(childNode);
                    break;
            }
        }
        this.dataSource$.next([...this.dataSource$.value]);
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

    collapseAll$ = new Subject<void>();

    collapseAll() {
        const state = this.appData.viewState.schemaToolWindowState();

        this.dataSource$.value.forEach(node => {
            this.rootNodesCollapsed[node.label] = true;
            state.rootNodesCollapsed[node.label] = true;
        });

        this.appData.viewState.setSchemaToolWindowState(state);
        this.collapseAll$.next();
    }

    expandAll$ = new Subject<void>();

    expandAll() {
        const state = this.appData.viewState.schemaToolWindowState();

        this.dataSource$.value.forEach(node => {
            this.rootNodesCollapsed[node.label] = false;
            state.rootNodesCollapsed[node.label] = false;
        });

        this.appData.viewState.setSchemaToolWindowState(state);
        this.expandAll$.next();
    }
}
