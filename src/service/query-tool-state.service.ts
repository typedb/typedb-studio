/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { FormControl } from "@angular/forms";
import MultiGraph from "graphology";
import { BehaviorSubject, combineLatest, first, map, Observable, ReplaySubject, shareReplay, startWith } from "rxjs";
import { DriverAction } from "../concept/action";
import { createSigmaRenderer, GraphVisualiser } from "../framework/graph-visualiser";
import { defaultSigmaSettings } from "../framework/graph-visualiser/defaults";
import { newVisualGraph } from "../framework/graph-visualiser/graph";
import { Layouts } from "../framework/graph-visualiser/layouts";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { DriverState } from "./driver-state.service";
import { SchemaState, Schema, SchemaAttribute, SchemaRole, SchemaConcept } from "./schema-state.service";
import { SnackbarService } from "./snackbar.service";
import { FlatTreeControl } from "@angular/cdk/tree";
import { MatTreeFlatDataSource, MatTreeFlattener } from "@angular/material/tree";
import {
    ApiResponse, Concept, ConceptDocument, ConceptRow, isApiErrorResponse, QueryResponse
} from "@samuel-butcher-typedb/typedb-http-driver";

export type OutputType = "raw" | "log" | "table" | "graph";

const NO_SERVER_CONNECTED = `No server connected`;
const NO_DATABASE_SELECTED = `No database selected`;
const NO_OPEN_TRANSACTION = `No open transaction`;
const QUERY_BLANK = `Query text is blank`;

const QUERY_HIGHLIGHT_DIV_ID = null; // "query-highlight-div"; // Add to body: <div id="query-highlight-div"></div>
@Injectable({
    providedIn: "root",
})
export class QueryToolState {

    queryControl = new FormControl("", {nonNullable: true});
    outputTypeControl = new FormControl("log" as OutputType, { nonNullable: true });
    outputTypes: OutputType[] = ["log", "table", "graph", "raw"];
    readonly schemaWindow = new SchemaWindowState(this.schema);
    readonly logOutput = new LogOutputState();
    readonly tableOutput = new TableOutputState();
    readonly graphOutput = new GraphOutputState();
    readonly rawOutput = new RawOutputState();
    readonly history = new HistoryWindowState(this.driver);
    answersOutputEnabled = true;
    readonly runDisabledReason$ = combineLatest(
        [this.driver.status$, this.driver.database$, this.driver.autoTransactionEnabled$, this.driver.transaction$, this.queryControl.valueChanges.pipe(startWith(this.queryControl.value))]
    ).pipe(map(([status, db, autoTransactionEnabled, tx, _query]) => {
        if (status !== "connected") return NO_SERVER_CONNECTED;
        else if (db == null) return NO_DATABASE_SELECTED;
        else if (!autoTransactionEnabled && !tx) return NO_OPEN_TRANSACTION;
        else if (!this.queryControl.value.length) return QUERY_BLANK; // _query becomes blank after a page navigation for some reason
        else return null;
    }), shareReplay(1));
    readonly runEnabled$ = this.runDisabledReason$.pipe(map(x => x == null));
    readonly outputDisabledReason$ = this.driver.status$.pipe(map(x => x === "connected" ? null : NO_SERVER_CONNECTED));
    readonly outputDisabled$ = this.outputDisabledReason$.pipe(map(x => x != null));

    constructor(private driver: DriverState, public schema: SchemaState, private snackbar: SnackbarService) {
        (window as any)["queryToolState"] = this;
        this.outputDisabled$.subscribe((disabled) => {
            if (disabled) this.outputTypeControl.disable();
            else this.outputTypeControl.enable();
        });
    }

    // TODO: LIMIT 1000 by default, configurable

    runQuery() {
        const query = this.queryControl.value;
        this.initialiseOutput(query);
        this.driver.query(query).subscribe({
            next: (res) => {
                this.outputQueryResponse(res);
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(res.err.message);
                }
            },
            error: (err) => {
                this.driver.checkHealth().subscribe({
                    next: () => {
                        const msg = err?.message || err?.toString() || `Unknown error`;
                        this.snackbar.errorPersistent(`Error: ${msg}\n`
                            + `Caused: Failed to execute query.`);
                    },
                    error: () => {
                        this.driver.connection$.pipe(first()).subscribe((connection) => {
                            if (connection && connection.url.includes(`localhost`)) {
                                this.snackbar.errorPersistent(`Unable to connect to TypeDB server.\n`
                                    + `Ensure the server is still running.`);
                            } else {
                                this.snackbar.errorPersistent(`Unable to connect to TypeDB server.\n`
                                    + `Check your network connection and ensure the server is still running.`);
                            }
                        });
                    }
                });
            },
        });
    }

    private initialiseOutput(query: string) {
        this.logOutput.clear();
        this.tableOutput.clear();
        this.graphOutput.destroy();
        this.rawOutput.clear();

        this.logOutput.appendLines(RUNNING, query, ``, `${TIMESTAMP}${new Date().toISOString()}`);
        this.tableOutput.status = "running";
        this.graphOutput.status = "running";
        this.graphOutput.query = query;
        this.graphOutput.database = this.driver.requireDatabase().name;
    }

    private outputQueryResponse(res: ApiResponse<QueryResponse>) {
        if (this.answersOutputEnabled) this.outputQueryResponseWithAnswers(res);
        else this.outputQueryResponseNoAnswers();
    }

    private outputQueryResponseWithAnswers(res: ApiResponse<QueryResponse>) {
        this.logOutput.appendBlankLine();
        this.logOutput.appendQueryResult(res);
        this.tableOutput.push(res);
        this.graphOutput.push(res);
        this.rawOutput.push(JSON.stringify(res, null, 2));
    }

    private outputQueryResponseNoAnswers() {
        this.logOutput.appendBlankLine();
        this.logOutput.appendLines(`${RESULT}${SUCCESS}`);
        this.tableOutput.status = "answerOutputDisabled";
        this.graphOutput.status = "answerOutputDisabled";
        this.rawOutput.push(SUCCESS_RAW);
    }
}

export type SchemaTreeNodeKind = "root" | "concept" | "link";

export interface SchemaTreeNodeBase {
    nodeKind: SchemaTreeNodeKind;
    children?: SchemaTreeNode[];
    visible: boolean;
}

export interface SchemaTreeRootNode extends SchemaTreeNodeBase {
    nodeKind: "root";
    label: string;
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

export class SchemaWindowState {
    dataSource: SchemaTreeRootNode[] = [];
    viewMode: "flat" | "hierarchical" = "flat";
    linksVisibility$ = new BehaviorSubject<Record<SchemaTreeLinkKind, boolean>>({
        "sub": true,
        "owns": true,
        "plays": true,
        "relates": true,
    });

    constructor(public schemaState: SchemaState) {
        schemaState.value$.subscribe(() => {
            this.buildView();
        });
        this.linksVisibility$.subscribe(() => {
            this.updateViewVisibility();
        });
    }

    hasChild = (_: number, node: SchemaTreeNode) => !!node.children?.length;

    childrenAccessor = (node: SchemaTreeNode): SchemaTreeNode[] => {
        return (node.children || []).filter(child => child.visible === true);
    };

    trackByFn = (_index: number, node: SchemaTreeNode) => {
        let id = node.nodeKind;
        switch (node.nodeKind) {
            case "root":
                id += `-${node.label}`;
                break;
            case "concept":
                id += `-${node.concept.label}`;
                break;
            case "link":
                switch (node.linkKind) {
                    case "sub":
                        id += `-${node.supertype.label}`;
                        break;
                    case "owns":
                        id += `-${node.ownedAttribute.label}`;
                        break;
                    case "plays":
                        id += `-${node.role.label}`;
                        break;
                    case "relates":
                        id += `-${node.role.label}`;
                        break;
                }
                break;
            default:
                throw `Unknown node kind: ${JSON.stringify(node)}`;
        }
        return id;
    }

    private buildView() {
        const schema = this.schemaState.value$.value;
        const linksVisibility = this.linksVisibility$.value;

        if (!schema) {
            this.dataSource.length = 0;
            return;
        }

        this.dataSource = [{
            nodeKind: "root",
            label: "Entities",
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
            label: "Relations",
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
            label: "Attributes",
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

        if (this.viewMode === "hierarchical") {
            const nodeMap = Object.fromEntries(this.dataSource.flatMap(rootNode => rootNode.children.map(conceptNode => ([
                conceptNode.concept.label, conceptNode
            ]))));

            this.dataSource.forEach(rootNode => {
                rootNode.children.forEach(conceptNode => {
                    const subNode = conceptNode.children.find(node => node.nodeKind === "link" && node.linkKind === "sub");
                    if (subNode) {
                        const superNode = nodeMap[subNode.supertype.label];
                        if (!superNode) throw new Error(`Missing supertype node for ${subNode.supertype.label} (nodeMap is: ${JSON.stringify(nodeMap)})`);
                        superNode.children.unshift(conceptNode);
                    }
                });
            });

            this.dataSource.flatMap(rootNode => rootNode.children).forEach(conceptNode => {
                conceptNode.children = conceptNode.children.filter(child => child.nodeKind !== "link" || child.linkKind !== "sub");
            });

            this.dataSource.forEach(x => x.children = x.children.filter(conceptNode => !conceptNode.concept.supertype));
        }
    }

    private updateViewVisibility() {
        this.dataSource.forEach(x => x.children.forEach(conceptNode => {
            conceptNode.children.filter(child => child.nodeKind === "link").forEach(linkNode => {
                linkNode.visible = this.linksVisibility$.value[linkNode.linkKind];
            });
        }));
        this.dataSource = [...this.dataSource];
    }

    useFlatView() {
        this.viewMode = "flat";
    }

    useHierarchicalView() {
        this.viewMode = "hierarchical";
        this.buildView();
    }

    toggleLinksVisibility(linkKind: SchemaTreeLinkKind) {
        this.linksVisibility$.next({
            ...this.linksVisibility$.value,
            [linkKind]: !this.linksVisibility$.value[linkKind]
        });
    }
}

export class HistoryWindowState {

    readonly entries: DriverAction[] = [];

    constructor(private driver: DriverState) {
        this.driver.actionLog$.subscribe((action) => {
            this.entries.unshift(action);
        });
    }
}

const RUNNING = `## Running> `;
const TIMESTAMP = `## Timestamp> `;
const RESULT = `## Result> `;
const SUCCESS = `Success`;
const SUCCESS_RAW = `success`;
const ERROR = `Error`;
const TABLE_INDENT = "   ";
const CONTENT_INDENT = "    ";
const TABLE_DASHES = 7;

function conceptDisplayString(concept: Concept | undefined): string {
    if (!concept) return "";

    switch (concept.kind) {
        case "entityType":
            return formatType(concept.label);
        case "relationType":
            return formatType(concept.label);
        case "roleType":
            return formatType(concept.label);
        case "attributeType":
            return formatType(concept.label);
        case "entity":
            return `${concept.type ? formatIsa(concept.type.label) : ""}, ${formatIid(concept.iid)}`;
        case "relation":
            return `${concept.type ? formatIsa(concept.type.label) : ""}, ${formatIid(concept.iid)}`;
        case "attribute":
            return `${concept.type ? formatIsa(concept.type.label) : ""} ${concept.value}`;
        case "value":
            return `${concept.value}`;
    }
}

// TODO: syntax highlighting
function formatType(label: string): string {
    return label;
    // return `\x1b[95m${label}\x1b[0m`;
}

function formatIid(iid: string): string {
    return `${formatKeyword("iid")} ${iid}`;
}

function formatIsa(label: string): string {
    return `${formatKeyword("isa")} ${label}`;
    // return `${this.formatKeyword("isa")} \x1b[95m${label}\x1b[0m`;
}

function formatKeyword(keyword: string): string {
    return keyword;
    // return `\x1b[94m${keyword}\x1b[0m`;
}

function indent(indentation: string, string: string): string {
    return string.split('\n').map(s => `${indentation}${s}`).join('\n');
}

export class LogOutputState {

    control = new FormControl("", {nonNullable: true});

    constructor() {}

    appendLines(...lines: string[]) {
        this.control.patchValue(`${this.control.value}${lines.join(`\n`)}\n`);
    }

    appendBlankLine() {
        this.appendLines(``);
    }

    appendQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) {
            this.appendLines(`${RESULT}${ERROR}`, ``, res.err.message);
            return;
        }

        let lines: string[] = [];
        lines.push(`${RESULT}${SUCCESS}`, ``);

        switch (res.ok.answerType) {
            case "ok": {
                lines.push(`Finished ${res.ok.queryType} query.`);
                break;
            }
            case "conceptRows": {
                lines.push(`Finished ${res.ok.queryType} query compilation and validation...`);
                if (res.ok.queryType === "write") lines.push(`Finished writes. Printing rows...`);
                else lines.push(`Printing rows...`);

                if (res.ok.answers.length) {
                    const varNames = Object.keys(res.ok.answers[0]);
                    if (varNames.length) {
                        const columnNames = Object.keys(res.ok.answers[0].data);
                        const variableColumnWidth = columnNames.length > 0 ? Math.max(...columnNames.map(s => s.length)) : 0;
                        res.ok.answers.forEach((rowAnswer, idx) => {
                            if (idx == 0) lines.push(this.lineDashSeparator(variableColumnWidth));
                            lines.push(this.conceptRowDisplayString(rowAnswer.data, variableColumnWidth))
                        })
                    } else lines.push(`No columns to show.`);
                }

                lines.push(`Finished. Total rows: ${res.ok.answers.length}`);
                break;
            }
            case "conceptDocuments": {
                lines.push(`Finished ${res.ok.queryType} query compilation and validation...`);
                if (res.ok.queryType === "write") lines.push(`Finished writes. Printing documents...`);
                else lines.push(`Printing documents...`);
                res.ok.answers.forEach(x => lines.push(this.conceptDocumentDisplayString(x)));
                lines.push(`Finished. Total documents: ${res.ok.answers.length}`);
                break;
            }
            default:
                throw INTERNAL_ERROR;
        }
        this.appendLines(...lines);
        this.appendBlankLine();
    }

    private conceptDocumentDisplayString(document: ConceptDocument): string {
        try {
            return JSON.stringify(document, null, 2);
        } catch (err) {
            return `Error trying to print JSON: ${err}`;
        }
    }

    private conceptRowDisplayString(row: ConceptRow, variableColumnWidth: number) {
        const columnNames = Object.keys(row);
        const contents = columnNames.map((columnName) =>
            `\$${columnName}${" ".repeat(variableColumnWidth - columnName.length + 1)}| ${conceptDisplayString(row[columnName])}`
        );
        const content = contents.join("\n");
        return `${indent(CONTENT_INDENT, content)}\n${this.lineDashSeparator(variableColumnWidth)}`;
    }

    private lineDashSeparator(additionalDashesNum: number): string {
        return indent(TABLE_INDENT, "-".repeat(TABLE_DASHES + additionalDashesNum));
    }

    clear() {
        this.control.patchValue(``);
    }
}

type TableOutputStatus = "ok" | "running" | "answerlessQueryType" | "answerOutputDisabled" | "noColumns" | "noAnswers" | "error";

type TableRow = { [column: string]: string };

export class TableOutputState {

    status: TableOutputStatus = "ok";
    private _data$ = new BehaviorSubject<TableRow[]>([]);
    private _columns: string[] = [];
    private _displayedColumns: string[] = [];

    constructor() {}

    get data$(): Observable<TableRow[]> {
        return this._data$;
    }

    get columns(): string[] {
        return this._columns;
    }

    get displayedColumns(): string[] {
        return this._displayedColumns;
    }

    handleMatSortChange(e: any) {
        // TODO
    }

    push(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) {
            this.status = "error";
            return;
        }

        switch (res.ok.answerType) {
            case "ok": {
                this.status = "answerlessQueryType";
                break;
            }
            case "conceptRows": {
                const answers = res.ok.answers;
                if (res.ok.answers.length) {
                    const varNames = Object.keys(answers[0].data);
                    if (varNames.length) {
                        this.status = "ok";
                        this.appendColumns(...varNames);
                        setTimeout(() => {
                            this.appendConceptRows(...answers.map(x => x.data));
                        });
                    } else this.status = "noColumns";
                } else this.status = "noAnswers";
                break;
            }
            case "conceptDocuments": {
                const answers = res.ok.answers;
                if (answers.length) {
                    const keys = Object.keys(answers[0]);
                    if (keys.length) {
                        this.status = "ok";
                        this.appendColumns(...keys);
                        setTimeout(() => {
                            this.appendRows(...answers.map(answer => Object.fromEntries(Object.entries(answer).map(([k, v]) => [k, JSON.stringify(v)]))));
                        });
                    } else this.status = "noColumns";
                } else this.status = "noAnswers";
                break;
            }
            default:
                throw INTERNAL_ERROR;
        }
    }

    private appendConceptRows(...rows: ConceptRow[]) {
        const tableRows: TableRow[] = rows.map(x => Object.fromEntries(Object.entries(x).map(
            ([varName, concept]) => [varName, this.conceptDisplayString(concept)]
        )));
        this.appendRows(...tableRows);
    }

    private appendColumns(...columns: string[]) {
        this.columns.push(...columns);
        this.displayedColumns.push(...columns);
    }

    private appendRows(...rows: { [column: string]: string }[]) {
        this._data$.value.push(...rows);
        this._data$.next(this._data$.value);
    }

    private conceptDisplayString(concept: Concept | undefined): string {
        if (!concept) return "";

        switch (concept.kind) {
            case "entityType":
            case "relationType":
            case "roleType":
            case "attributeType":
                return concept.label;
            case "entity":
            case "relation":
                return `iid ${(concept.iid)}`;
            case "attribute":
                return `${concept.value}`;
            case "value":
                return `${concept.value}`;
        }
    }

    clearStatus() {
        this.status = "ok";
    }

    clear() {
        this.clearStatus();
        this.columns.length = 0;
        this.displayedColumns.length = 0;
        this._data$.next([]);
    }
}

type GraphOutputStatus = "ok" | "running" | "graphlessQueryType" | "answerOutputDisabled" | "noAnswers" | "error";

export class GraphOutputState {

    status: GraphOutputStatus = "ok";
    canvasEl!: HTMLElement;
    visualiser: GraphVisualiser | null = null;
    query?: string;
    database?: string;

    constructor() {
    }

    push(res: ApiResponse<QueryResponse>) {
        if (!this.canvasEl) throw `Missing canvas element`;

        if (!this.visualiser) {
            const graph = newVisualGraph();
            const sigma = createSigmaRenderer(this.canvasEl, defaultSigmaSettings as any, graph);
            const layout = Layouts.createForceAtlasStatic(graph, undefined); // This is the safe option
            // const layout = Layouts.createForceLayoutSupervisor(graph, studioDefaults.defaultForceSupervisorSettings);
            this.visualiser = new GraphVisualiser(graph, sigma, layout);
        }

        if (isApiErrorResponse(res)) {
            this.status = "error";
            return;
        }

        switch (res.ok.answerType) {
            case "ok": {
                this.status = "graphlessQueryType";
                break;
            }
            case "conceptRows": {
                this.visualiser.handleQueryResponse(res, this.database!);
                let highlightedQuery = "";
                if (QUERY_HIGHLIGHT_DIV_ID != null) {
                    if (res.ok.query) {
                        highlightedQuery = this.visualiser.colorQuery(this.query!, res.ok.query);
                        document.getElementById("query-highlight-div")!.innerHTML = highlightedQuery;
                    }
                }
                this.visualiser.colorEdgesByConstraintIndex(false);
                this.status = "ok";
                // document.getElementById("query-highlight-div").innerHTML = highlightedQuery;
                // if (res.queryStructure != null) {
                //     highlightedQuery = studio.colorQuery(form.query.value, query_result.queryStructure);
                //     studio.colorEdgesByConstraintIndex(false);
                // }
                // document.getElementById("query-highlight-div").innerHTML = highlightedQuery;
                // if (res.answers.length) {
                //     const varNames = Object.keys(res.answers[0]);
                //     if (varNames.length) {
                //         this.status = "ok";
                //         this.appendColumns(...varNames);
                //         setTimeout(() => {
                //             this.appendConceptRows(...res.answers);
                //         });
                //     } else this.status = "noColumns";
                // } else this.status = "noAnswers";
                break;
            }
            case "conceptDocuments": {
                this.status = "graphlessQueryType";
                break;
            }
            default:
                throw INTERNAL_ERROR;
        }
    }

    destroy() {
        this.visualiser?.destroy();
        this.visualiser = null;
    }
}

export class RawOutputState {

    control = new FormControl("", {nonNullable: true});

    constructor() {}

    push(value: string) {
        this.control.patchValue(value);
    }

    clear() {
        this.control.patchValue(``);
    }
}
