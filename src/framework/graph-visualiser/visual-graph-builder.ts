import { getVariableName, ConstraintVertexAny } from "@typedb/driver-http";
import {
    LogicalGraph,
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
} from "./logical-graph";
import {
    AnalyzedPipelineBackCompat,
    ConstraintBackCompat,
    backCompat_expressionAssigned,
} from "./logical-graph-builder";
import {
    EdgeAttributes,
    EdgeMetadata,
    VisualGraphBuilderStructureParams,
    VertexAttributes,
    VertexMetadata,
    VisualGraph,
} from "./visual-graph";
import { GraphStyles } from "./styles";

/////////////////////////////////
// Logical Graph -> Graphology //
/////////////////////////////////

/**
 * You will majorly need:
 *  graph.addNode(id, attributes)
 *  graph.addNode(from, to,  attributes)
 * See: https://www.sigmajs.org/docs/advanced/data/ for attributes
 */
export interface IVisualGraphBuilder {
  // TODO: Functional vertices & edges like expressions, comparisons & function calls

  // Vertices
  put_vertex(answer_index: number, vertex: DataVertex, queryVertex: ConstraintVertexAny): void;

  // Edges
  put_isa(answer_index: number, constraint: DataConstraintIsa): void;

  put_isa_exact(answerIndex: number, constraint: DataConstraintIsaExact): void

  put_has(answer_index: number, constraint: DataConstraintHas): void;

  put_links(answer_index: number, constraint: DataConstraintLinks): void;

  put_sub(answer_index: number, constraint: DataConstraintSub): void;

  put_sub_exact(answerIndex: number, constraint: DataConstraintSubExact): void;

  put_owns(answer_index: number, constraint: DataConstraintOwns): void;

  put_relates(answer_index: number, constraint: DataConstraintRelates): void;

  put_plays(answer_index: number, constraint: DataConstraintPlays): void;

  put_expression(answer_index: number, constraint: DataConstraintExpression): void;

  put_function(answer_index: number, constraint: DataConstraintFunction): void;

  put_kind(answer_index: number, constraint: DataConstraintKind): void;
}

export function buildVisualGraph(dataGraph: LogicalGraph, builder: IVisualGraphBuilder) {
    dataGraph.answers.forEach((edgeList, answerIndex) => {
        edgeList.forEach(edge => {
            putConstraint(builder, answerIndex, edge);
        });
    });
}

function putConstraint(builder: IVisualGraphBuilder, answer_index: number, constraint: DataConstraintAny) {
  switch (constraint.tag) {
    case "isa":{
      builder.put_isa(answer_index, constraint);
      break;
    }
    case "isa!":{
      builder.put_isa_exact(answer_index, constraint);
      break;
    }
    case "has": {
      builder.put_has(answer_index, constraint);
      break;
    }
    case "links": {
      builder.put_links(answer_index, constraint);
      break;
    }
    case "sub": {
      builder.put_sub(answer_index, constraint);
      break;
    }
    case "sub!": {
      builder.put_sub_exact(answer_index, constraint);
      break;
    }
    case "owns": {
      builder.put_owns(answer_index, constraint);
      break;
    }
    case "relates": {
      builder.put_relates(answer_index, constraint);
      break;
    }
    case "plays": {
      builder.put_plays(answer_index, constraint);
      break;
    }
    case "expression" : {
      builder.put_expression(answer_index, constraint);
      break;
    }
    case "function" : {
      builder.put_function(answer_index, constraint);
      break;
    }
    case "kind": {
      builder.put_kind(answer_index, constraint);
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

export class VisualGraphBuilder implements IVisualGraphBuilder {

    constructor(
        public readonly graph: VisualGraph, public readonly queryStructure: AnalyzedPipelineBackCompat,
        public readonly isFollowupQuery: boolean, public readonly structureParameters: VisualGraphBuilderStructureParams,
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

    private edgeKey(from_id: string, to_id: string, edge_type_id: string) : string {
        return `${from_id}:${to_id}:${edge_type_id}`;
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
            let fromKey = this.put_vertex(answerIndex, from, queryFrom);
            let toKey = this.put_vertex(answerIndex, to, queryTo);
            let edgeKey = this.edgeKey(fromKey, toKey, label);
            const attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, edge));
            this.createEdge(edgeKey, fromKey, toKey, attributes);
        } else {
            if (this.shouldCreateNode(queryFrom)) {
                this.put_vertex(answerIndex, from, queryFrom);
            }
            if (this.shouldCreateNode(queryTo)) {
                this.put_vertex(answerIndex, to, queryTo);
            }
        }
    }

    // IVisualGraphBuilder
    // Vertices
    put_vertex(answerIndex: number, vertex: DataVertex, queryVertex: ConstraintVertexOrSpecial): string {
        const key = vertexMapKey(vertex);
        if (this.shouldCreateNode(queryVertex) && vertex.kind !== "unavailable") {
            this.createVertex(key, this.vertexAttributes(vertex))
        }
        return key;
    }

    // Edges
    put_isa(answerIndex: number, constraint: DataConstraintIsa): void {
        let isa =  constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, isa.instance, isa.type, queryConstraint.instance, queryConstraint.type);
    }

    put_isa_exact(answerIndex: number, constraint: DataConstraintIsaExact): void {
        let isa =  constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, isa.instance, isa.type, queryConstraint.instance, queryConstraint.type);
    }

    put_has(answerIndex: number, constraint: DataConstraintHas): void {
        let has =  constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, has.owner, has.attribute, queryConstraint.owner, queryConstraint.attribute);
    }

    put_links(answerIndex: number, constraint: DataConstraintLinks): void {
        let links = constraint;
        let queryConstraint =  constraint.queryConstraint;
        const label = links.role.kind === "roleType" ? links.role.label.split(":").at(-1) : `?`;
        if (!label) throw `${this.put_links.name}: invalid role label '${JSON.stringify(links.role)}'`;
        this.maybeCreateEdge(answerIndex, constraint, label, links.relation, links.player, queryConstraint.relation, queryConstraint.player);
    }

    put_sub(answerIndex: number, constraint: DataConstraintSub): void {
        let sub = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, sub.subtype, sub.supertype, queryConstraint.subtype, queryConstraint.supertype);
    }

    put_sub_exact(answerIndex: number, constraint: DataConstraintSubExact): void {
        let sub = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, sub.subtype, sub.supertype, queryConstraint.subtype, queryConstraint.supertype);
    }

    put_owns(answerIndex: number, constraint: DataConstraintOwns): void {
        let owns = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, owns.owner, owns.attribute, queryConstraint.owner, queryConstraint.attribute);
    }

    put_relates(answerIndex: number, constraint: DataConstraintRelates): void {
        let relates = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, relates.relation, relates.role, queryConstraint.relation, queryConstraint.role);
    }

    put_plays(answerIndex: number, constraint: DataConstraintPlays): void {
        let plays = constraint;
        let queryConstraint =  constraint.queryConstraint;
        let label = constraint.tag;
        this.maybeCreateEdge(answerIndex, constraint, label, plays.player, plays.role, queryConstraint.player, queryConstraint.role);
    }

    put_expression(answerIndex: number, constraint: DataConstraintExpression): void {
        let expression = constraint;
        let expressionVertexKey = expressionVertexKeyFromArgsAndAssigned(constraint);
        let expressionVertex: VertexExpression = {
            tag: "expression",
            kind: "expression",
            answerIndex: answerIndex,
            repr: constraint.text,
            vertex_map_key: expressionVertexKey
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

    put_function(answerIndex: number, constraint: DataConstraintFunction): void {
        let functionCall = constraint;
        let functionVertexKey = functionVertexKeyFromArgsAndAssigned(constraint);
        let functionVertex: VertexFunction = {
            tag: "functionCall",
            kind: "functionCall", answerIndex: answerIndex,
            repr: constraint.name,
            vertex_map_key: functionVertexKey
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

    put_kind(answer_index: number, constraint: DataConstraintKind): void {
        this.put_vertex(answer_index, constraint.type, constraint.queryConstraint.type);
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
            return vertex.vertex_map_key;
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
