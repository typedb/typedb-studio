import { getVariableName, ConstraintVertexAny } from "@typedb/driver-http";
import {
    StructuredAnswer,
    DataConstraintExpression,
    DataConstraintFunction,
    DataConstraintAny,
    DataConstraintIsa,
    DataConstraintLinks,
    DataConstraintHas,
    DataConstraintSub,
    DataConstraintOwns,
    DataConstraintRelates,
    DataConstraintPlays,
    VertexFunction,
    VertexExpression, DataConstraintSubExact, DataConstraintIsaExact, DataVertex,
    DataConstraintKind, getTypeLabel,
} from "./structured-answers";
import {
    AnalyzedPipelineBackCompat,
    ConstraintBackCompat,
    backCompat_expressionAssigned,
} from "./structured-answers-builder";
import {
    EdgeAttributes,
    EdgeMetadata,
    GraphBuilderStructureParams,
    VertexAttributes,
    VertexMetadata,
    Graph,
} from "./graph";
import { GraphStyles } from "./styles";

/////////////////////////////////
// Structured Answers -> Graphology //
/////////////////////////////////

/**
 * You will majorly need:
 *  graph.addNode(id, attributes)
 *  graph.addNode(from, to,  attributes)
 * See: https://www.sigmajs.org/docs/advanced/data/ for attributes
 */
export interface IGraphBuilder {
  // TODO: Functional vertices & edges like expressions, comparisons & function calls

  // Vertices
  putVertex(answerIndex: number, vertex: DataVertex, queryVertex: ConstraintVertexAny): void;

  // Edges
  putIsa(answerIndex: number, constraint: DataConstraintIsa): void;

  putIsaExact(answerIndex: number, constraint: DataConstraintIsaExact): void

  putHas(answerIndex: number, constraint: DataConstraintHas): void;

  putLinks(answerIndex: number, constraint: DataConstraintLinks): void;

  putSub(answerIndex: number, constraint: DataConstraintSub): void;

  putSubExact(answerIndex: number, constraint: DataConstraintSubExact): void;

  putOwns(answerIndex: number, constraint: DataConstraintOwns): void;

  putRelates(answerIndex: number, constraint: DataConstraintRelates): void;

  putPlays(answerIndex: number, constraint: DataConstraintPlays): void;

  putExpression(answerIndex: number, constraint: DataConstraintExpression): void;

  putFunction(answerIndex: number, constraint: DataConstraintFunction): void;

  putKind(answerIndex: number, constraint: DataConstraintKind): void;
}

export function buildGraph(answers: StructuredAnswer[], builder: IGraphBuilder) {
    answers.forEach((answer, answerIndex) => {
        answer.constraints.forEach(constraint => {
            putConstraint(builder, answerIndex, constraint);
        });
    });
}

function putConstraint(builder: IGraphBuilder, answerIndex: number, constraint: DataConstraintAny) {
  switch (constraint.tag) {
    case "isa":{
      builder.putIsa(answerIndex, constraint);
      break;
    }
    case "isa!":{
      builder.putIsaExact(answerIndex, constraint);
      break;
    }
    case "has": {
      builder.putHas(answerIndex, constraint);
      break;
    }
    case "links": {
      builder.putLinks(answerIndex, constraint);
      break;
    }
    case "sub": {
      builder.putSub(answerIndex, constraint);
      break;
    }
    case "sub!": {
      builder.putSubExact(answerIndex, constraint);
      break;
    }
    case "owns": {
      builder.putOwns(answerIndex, constraint);
      break;
    }
    case "relates": {
      builder.putRelates(answerIndex, constraint);
      break;
    }
    case "plays": {
      builder.putPlays(answerIndex, constraint);
      break;
    }
    case "expression" : {
      builder.putExpression(answerIndex, constraint);
      break;
    }
    case "function" : {
      builder.putFunction(answerIndex, constraint);
      break;
    }
    case "kind": {
      builder.putKind(answerIndex, constraint);
      break;
    }
    case "comparison": break;
    case "is": break;
    case "iid": break;
    case "label": break;
    case "value": break;
  }
}

type ConstraintVertexOrSpecial = ConstraintVertexAny | VertexFunction | VertexExpression;

export class GraphBuilder implements IGraphBuilder {

    constructor(
        public readonly graph: Graph, public readonly queryStructure: AnalyzedPipelineBackCompat,
        public readonly isFollowupQuery: boolean, public readonly structureParameters: GraphBuilderStructureParams,
        public readonly styleParameters: GraphStyles
    ) {
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
            size: Math.min(width, height),
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
            let fromKey = this.putVertex(answerIndex, from, queryFrom);
            let toKey = this.putVertex(answerIndex, to, queryTo);
            let edgeKey = this.edgeKey(fromKey, toKey, label);
            const attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, edge));
            this.createEdge(edgeKey, fromKey, toKey, attributes);
        } else {
            if (this.shouldCreateNode(queryFrom)) {
                this.putVertex(answerIndex, from, queryFrom);
            }
            if (this.shouldCreateNode(queryTo)) {
                this.putVertex(answerIndex, to, queryTo);
            }
        }
    }

    // IGraphBuilder
    // Vertices
    putVertex(answerIndex: number, vertex: DataVertex, queryVertex: ConstraintVertexOrSpecial): string {
        const key = vertexMapKey(vertex);
        if (this.shouldCreateNode(queryVertex) && vertex.kind !== "unavailable") {
            this.createVertex(key, this.vertexAttributes(vertex))
        }
        return key;
    }

    // Edges
    putIsa(answerIndex: number, constraint: DataConstraintIsa): void {
        let isa =  constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, isa.instance, isa.type, queryConstraint.instance, queryConstraint.type);
    }

    putIsaExact(answerIndex: number, constraint: DataConstraintIsaExact): void {
        let isa =  constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, isa.instance, isa.type, queryConstraint.instance, queryConstraint.type);
    }

    putHas(answerIndex: number, constraint: DataConstraintHas): void {
        let has =  constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, has.owner, has.attribute, queryConstraint.owner, queryConstraint.attribute);
    }

    putLinks(answerIndex: number, constraint: DataConstraintLinks): void {
        let links = constraint;
        let queryConstraint =  constraint.queryConstraint;
        const label = links.role.kind === "roleType" ? links.role.label.split(":").at(-1) : `?`;
        if (!label) throw `${this.putLinks.name}: invalid role label '${JSON.stringify(links.role)}'`;
        this.maybeCreateEdge(answerIndex, constraint, label, links.relation, links.player, queryConstraint.relation, queryConstraint.player);
    }

    putSub(answerIndex: number, constraint: DataConstraintSub): void {
        let sub = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, sub.subtype, sub.supertype, queryConstraint.subtype, queryConstraint.supertype);
    }

    putSubExact(answerIndex: number, constraint: DataConstraintSubExact): void {
        let sub = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, sub.subtype, sub.supertype, queryConstraint.subtype, queryConstraint.supertype);
    }

    putOwns(answerIndex: number, constraint: DataConstraintOwns): void {
        let owns = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, owns.owner, owns.attribute, queryConstraint.owner, queryConstraint.attribute);
    }

    putRelates(answerIndex: number, constraint: DataConstraintRelates): void {
        let relates = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, relates.relation, relates.role, queryConstraint.relation, queryConstraint.role);
    }

    putPlays(answerIndex: number, constraint: DataConstraintPlays): void {
        let plays = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, plays.player, plays.role, queryConstraint.player, queryConstraint.role);
    }

    putExpression(answerIndex: number, constraint: DataConstraintExpression): void {
        let expression = constraint;
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
        this.maybeCreateEdge(answerIndex, constraint, label, expressionVertex, expression.assigned, expressionVertex, queryVertex);
        expression.arguments
            .forEach((arg, i) => {
                let queryVertex = constraint.queryConstraint.arguments[i];
                let varNameOrId = getVariableName(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `arg[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, arg, expressionVertex, queryVertex, expressionVertex);
            });
    }

    putFunction(answerIndex: number, constraint: DataConstraintFunction): void {
        let functionCall = constraint;
        let functionVertexKey = functionVertexKeyFromArgsAndAssigned(constraint);
        let functionVertex: VertexFunction = {
            tag: "functionCall",
            kind: "functionCall", answerIndex: answerIndex,
            repr: constraint.name,
            vertexMapKey: functionVertexKey
        }
        functionCall.assigned
            .forEach((assigned, i) => {
                let queryVertex = constraint.queryConstraint.assigned[i];
                let varNameOrId = getVariableName(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `assign[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, functionVertex, assigned, functionVertex, queryVertex);
            });
        functionCall.arguments
            .forEach((arg, i) => {
                let queryVertex = constraint.queryConstraint.arguments[i];
                let varNameOrId = getVariableName(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `arg[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, arg, functionVertex, queryVertex, functionVertex);
            });
    }

    putKind(answerIndex: number, constraint: DataConstraintKind): void {
        this.putVertex(answerIndex, constraint.type, constraint.queryConstraint.type);
    }
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
