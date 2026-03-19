import { getVariableName, ConstraintVertexAny } from "@typedb/driver-http";
import {
    AbstractGraphBuilder,
    StructuredAnswer, DataConstraintAny, DataVertex, getTypeLabel,
    DataConstraintIsa, DataConstraintIsaExact, DataConstraintHas, DataConstraintLinks,
    DataConstraintSub, DataConstraintSubExact, DataConstraintOwns, DataConstraintRelates, DataConstraintPlays,
    DataConstraintExpression, DataConstraintFunction, DataConstraintKind,
    DataConstraintComparison, DataConstraintIs, DataConstraintIid, DataConstraintLabel, DataConstraintValue,
    VertexExpression, VertexFunction,
} from "@typedb/graph-utils";
import {
    AnalyzedPipelineBackCompat,
    ConstraintBackCompat,
    backCompat_expressionAssigned,
} from "@typedb/graph-utils";
import {
    EdgeAttributes,
    EdgeMetadata,
    GraphBuilderStructureParams,
    VertexAttributes,
    VertexMetadata,
    Graph,
} from "./graph";
import { GraphStyles } from "./styles";

type ConstraintVertexOrSpecial = ConstraintVertexAny | VertexFunction | VertexExpression;

export class GraphBuilder extends AbstractGraphBuilder {

    constructor(
        public readonly graph: Graph, public readonly queryStructure: AnalyzedPipelineBackCompat,
        public readonly isFollowupQuery: boolean, public readonly structureParameters: GraphBuilderStructureParams,
        public readonly styleParameters: GraphStyles
    ) {
        super();
    }

    private vertexMetadata(vertex: DataVertex): VertexMetadata {
        return {
            defaultLabel: this.styleParameters.vertexDefaultLabel(vertex),
            hoverLabel: this.styleParameters.vertexHoverLabel(vertex),
            concept: vertex,
        };
    }

    private vertexAttributes(vertex: DataVertex): VertexAttributes {
        // Extend as you please: https://www.sigmajs.org/docs/advanced/data/
        const typeLabel = getTypeLabel(vertex);
        const color = (typeLabel && this.styleParameters.vertexTypeColors?.[typeLabel])
            ?? this.styleParameters.vertexColors[vertex.kind];
        const borderColor = (typeLabel && this.styleParameters.vertexTypeBorderColors?.[typeLabel])
            ?? this.styleParameters.vertexBorderColors[vertex.kind];
        const shape = (typeLabel && this.styleParameters.vertexTypeShapes?.[typeLabel])
            ?? this.styleParameters.vertexShapes[vertex.kind];
        const width = (typeLabel ? this.styleParameters.vertexTypeWidths?.[typeLabel] : undefined)
            ?? this.styleParameters.vertexWidths?.[vertex.kind]
            ?? this.styleParameters.vertexHeight;
        const height = (typeLabel ? this.styleParameters.vertexTypeHeights?.[typeLabel] : undefined)
            ?? this.styleParameters.vertexHeights?.[vertex.kind]
            ?? this.styleParameters.vertexHeight;
        return {
            label: this.styleParameters.vertexDefaultLabel(vertex),
            color: color,
            borderColor: borderColor,
            width: width,
            height: height,
            size: Math.max(width, height),
            type: shape,
            x: Math.random(),
            y: Math.random(),
            metadata: this.vertexMetadata(vertex),
            highlighted: false,
        }
    }

    private edgeMetadata(answerIndex: number, edge: DataConstraintAny): EdgeMetadata {
        if (this.isFollowupQuery) {
            return { answerIndex: -1, dataEdge: edge };
        } else {
            return { answerIndex, dataEdge: edge };
        }
    }

    private edgeAttributes(label: string, metadata: EdgeMetadata): EdgeAttributes {
        // Extend as you please: https://www.sigmajs.org/docs/advanced/data/
        const tag = metadata.dataEdge.tag;
        const colorHex = this.styleParameters.edgeLabelColors?.[tag]
            ?? this.styleParameters.edgeColor.hex();
        return {
            label: label,
            color: colorHex,
            size: this.styleParameters.edgeSize,
            type: "line",
            metadata: metadata,
        }
    }

    private edgeKey(fromId: string, toId: string, edgeTypeId: string) : string {
        return `${fromId}:${toId}:${edgeTypeId}`;
    }

    private shouldCreateNode(queryVertex: ConstraintVertexOrSpecial) {
        return shouldCreateNode(this.queryStructure, queryVertex);
    }

    private shouldCreateEdge(edge: DataConstraintAny, from: ConstraintVertexOrSpecial, to: ConstraintVertexOrSpecial) {
        return shouldCreateEdge(this.queryStructure, edge.queryConstraint, from, to);
    }

    private createVertex(key: string, attributes: VertexAttributes) {
        if (!this.graph.hasNode(key))  {
            this.graph.addNode(key, attributes);
        }
    }

    private createEdge(edgeKey: string, from: string, to: string, attributes: EdgeAttributes) {
        if (!this.graph.hasDirectedEdge(edgeKey)) {
            // TODO: If there is an edge between the two vertices, make it curved
            if (this.graph.hasDirectedEdge(from, to)) {
                attributes.type = "curved";
            }
            this.graph.addDirectedEdgeWithKey(edgeKey, from, to, attributes);
        }
    }

    private maybeCreateEdge(answerIndex: number, edge: DataConstraintAny, label: string, from: DataVertex, to: DataVertex, queryFrom: ConstraintVertexOrSpecial, queryTo: ConstraintVertexOrSpecial) {
        // Don't create edges if either vertex is unavailable
        if (from.kind === "unavailable" || to.kind === "unavailable") {
            return;
        }

        if (this.shouldCreateEdge(edge, queryFrom, queryTo)) {
            let fromKey = this.vertex(answerIndex, from, queryFrom);
            let toKey = this.vertex(answerIndex, to, queryTo);
            let edgeKey = this.edgeKey(fromKey, toKey, label);
            const attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, edge));
            this.createEdge(edgeKey, fromKey, toKey, attributes);
        } else {
            if (this.shouldCreateNode(queryFrom)) {
                this.vertex(answerIndex, from, queryFrom);
            }
            if (this.shouldCreateNode(queryTo)) {
                this.vertex(answerIndex, to, queryTo);
            }
        }
    }

    // AbstractGraphBuilder
    // Vertices
    vertex(_answerIndex: number, vertex: DataVertex, queryVertex: ConstraintVertexOrSpecial): string {
        const key = vertexMapKey(vertex);
        if (this.shouldCreateNode(queryVertex) && vertex.kind !== "unavailable") {
            this.createVertex(key, this.vertexAttributes(vertex));
        }
        return key;
    }

    // Edges
    isa(answerIndex: number, constraint: DataConstraintIsa): void {
        let queryConstraint = constraint.queryConstraint;
        this.maybeCreateEdge(answerIndex, constraint, constraint.tag, constraint.instance, constraint.type, queryConstraint.instance, queryConstraint.type);
    }

    isaExact(answerIndex: number, constraint: DataConstraintIsaExact): void {
        let queryConstraint = constraint.queryConstraint;
        this.maybeCreateEdge(answerIndex, constraint, constraint.tag, constraint.instance, constraint.type, queryConstraint.instance, queryConstraint.type);
    }

    has(answerIndex: number, constraint: DataConstraintHas): void {
        let queryConstraint = constraint.queryConstraint;
        this.maybeCreateEdge(answerIndex, constraint, constraint.tag, constraint.owner, constraint.attribute, queryConstraint.owner, queryConstraint.attribute);
    }

    links(answerIndex: number, constraint: DataConstraintLinks): void {
        let queryConstraint = constraint.queryConstraint;
        const label = constraint.role.kind === "roleType" ? constraint.role.label.split(":").at(-1) : `?`;
        if (!label) throw `${this.links.name}: invalid role label '${JSON.stringify(constraint.role)}'`;
        this.maybeCreateEdge(answerIndex, constraint, label, constraint.relation, constraint.player, queryConstraint.relation, queryConstraint.player);
    }

    sub(answerIndex: number, constraint: DataConstraintSub): void {
        let queryConstraint = constraint.queryConstraint;
        this.maybeCreateEdge(answerIndex, constraint, constraint.tag, constraint.subtype, constraint.supertype, queryConstraint.subtype, queryConstraint.supertype);
    }

    subExact(answerIndex: number, constraint: DataConstraintSubExact): void {
        let queryConstraint = constraint.queryConstraint;
        this.maybeCreateEdge(answerIndex, constraint, constraint.tag, constraint.subtype, constraint.supertype, queryConstraint.subtype, queryConstraint.supertype);
    }

    owns(answerIndex: number, constraint: DataConstraintOwns): void {
        let queryConstraint = constraint.queryConstraint;
        this.maybeCreateEdge(answerIndex, constraint, constraint.tag, constraint.owner, constraint.attribute, queryConstraint.owner, queryConstraint.attribute);
    }

    relates(answerIndex: number, constraint: DataConstraintRelates): void {
        let queryConstraint = constraint.queryConstraint;
        this.maybeCreateEdge(answerIndex, constraint, constraint.tag, constraint.relation, constraint.role, queryConstraint.relation, queryConstraint.role);
    }

    plays(answerIndex: number, constraint: DataConstraintPlays): void {
        let queryConstraint = constraint.queryConstraint;
        this.maybeCreateEdge(answerIndex, constraint, constraint.tag, constraint.player, constraint.role, queryConstraint.player, queryConstraint.role);
    }

    expression(answerIndex: number, constraint: DataConstraintExpression): void {
        let expressionVertexKey = expressionVertexKeyFromArgsAndAssigned(constraint);
        let expressionVertex: VertexExpression = {
            tag: "expression",
            kind: "expression",
            answerIndex: answerIndex,
            repr: constraint.text,
            vertexMapKey: expressionVertexKey
        }

        let queryVertex = backCompat_expressionAssigned(constraint.queryConstraint);
        let varNameOrId = getVariableName(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
        let label = `assign[${varNameOrId}]`;
        this.maybeCreateEdge(answerIndex, constraint, label, expressionVertex, constraint.assigned, expressionVertex, queryVertex);
        constraint.arguments
            .forEach((arg, i) => {
                let queryVertex = constraint.queryConstraint.arguments[i];
                let varNameOrId = getVariableName(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `arg[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, arg, expressionVertex, queryVertex, expressionVertex);
            });
    }

    function(answerIndex: number, constraint: DataConstraintFunction): void {
        let functionVertexKey = functionVertexKeyFromArgsAndAssigned(constraint);
        let functionVertex: VertexFunction = {
            tag: "functionCall",
            kind: "functionCall", answerIndex: answerIndex,
            repr: constraint.name,
            vertexMapKey: functionVertexKey
        }
        constraint.assigned
            .forEach((assigned, i) => {
                let queryVertex = constraint.queryConstraint.assigned[i];
                let varNameOrId = getVariableName(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `assign[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, functionVertex, assigned, functionVertex, queryVertex);
            });
        constraint.arguments
            .forEach((arg, i) => {
                let queryVertex = constraint.queryConstraint.arguments[i];
                let varNameOrId = getVariableName(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `arg[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, arg, functionVertex, queryVertex, functionVertex);
            });
    }

    kind(answerIndex: number, constraint: DataConstraintKind): void {
        this.vertex(answerIndex, constraint.type, constraint.queryConstraint.type);
    }

    // TODO

    comparison(_answerIndex: number, _constraint: DataConstraintComparison): void {}

    is(_answerIndex: number, _constraint: DataConstraintIs): void {}

    iid(_answerIndex: number, _constraint: DataConstraintIid): void {}

    label(_answerIndex: number, _constraint: DataConstraintLabel): void {}

    value(_answerIndex: number, _constraint: DataConstraintValue): void {}
}

export function shouldCreateNode(structure: AnalyzedPipelineBackCompat, vertex: ConstraintVertexOrSpecial) {
    // Labels should not create nodes
    if (vertex.tag === "label") {
        return false;
    }

    // For variables, check if they're in outputs or if outputs is empty (show all)
    if (vertex.tag === "variable") {
        const outputs = structure.outputs || [];
        // TODO: Remove this check once we drop support for TypeDB 3.7
        // TypeDB 3.7 returns an empty outputs array, so we show all variables when outputs is empty
        if (outputs.length === 0) {
            return true;
        }
        // Otherwise, only show variables that are in outputs
        return outputs.includes(vertex.id);
    }

    // For other vertex types (expression, functionCall, etc.), include them
    return true;
}

export function shouldCreateEdge(structure: AnalyzedPipelineBackCompat, _edge: ConstraintBackCompat, from: ConstraintVertexOrSpecial, to: ConstraintVertexOrSpecial) {
    return shouldCreateNode(structure, from) && shouldCreateNode(structure, to);
}

export function vertexMapKey(vertex: DataVertex): string {
    switch (vertex.kind) {
        case "attribute":
            return `${vertex.type.label}:${vertex.value}`;
        case "entity":
        case "relation":
            return vertex.iid;
        case "attributeType":
        case "entityType":
        case "relationType":
        case "roleType":
            return vertex.label;
        case "value":
            return `${vertex.valueType}:${vertex.value}`;
        case "expression":
        case "functionCall":
            return vertex.vertexMapKey;
        case "unavailable":
            return `unavailable[${vertex.variable}][${vertex.answerIndex}]`;
        default:
            throw `Unexpected vertex type: ${vertex}`;
    }
}

function functionVertexKeyFromArgsAndAssigned(constraint: DataConstraintFunction): string {
    let args = constraint.arguments.map(v => vertexMapKey(v)).join(",");
    let assigned = constraint.assigned.map(v => vertexMapKey(v)).join(",");
    return `${constraint.name}(${args}) -> ${assigned}`;
}

function expressionVertexKeyFromArgsAndAssigned(constraint: DataConstraintExpression): string {
    let args = constraint.arguments.map(v => vertexMapKey(v)).join(",");
    let assigned = constraint.assigned;
    return `${constraint.text}(${args}) -> ${assigned}`;
}
