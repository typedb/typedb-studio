import {
    get_variable_name,
    QueryConstraintAny,
    QueryStructure,
    QueryVertex,
} from "../typedb-driver/query-structure";
import {
    EdgeAttributes,
    EdgeMetadata,
    VertexAttributes,
    VertexMetadata,
    VisualGraph,
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
    VertexExpression, DataConstraintSubExact, DataConstraintIsaExact, DataVertex
} from "./graph";
import {ILogicalGraphConverter} from "./visualisation";
import {StudioConverterStructureParameters, StudioConverterStyleParameters} from "./config";

type QueryVertexOrSpecial = QueryVertex | VertexFunction | VertexExpression;

export class StudioConverter implements ILogicalGraphConverter {

    constructor(
        public readonly graph: VisualGraph, public readonly queryStructure: QueryStructure,
        public readonly isFollowupQuery: boolean, public readonly structureParameters: StudioConverterStructureParameters,
        public readonly styleParameters: StudioConverterStyleParameters
    ) {
    }

    private vertexMetadata(vertex: DataVertex): VertexMetadata {
        return {
            defaultLabel: this.styleParameters.vertex_default_label(vertex),
            hoverLabel: this.styleParameters.vertex_hover_label(vertex),
            concept: vertex,
        };
    }

    private vertexAttributes(vertex: DataVertex): VertexAttributes {
        // Extend as you please: https://www.sigmajs.org/docs/advanced/data/
        const color = this.styleParameters.vertex_colors[vertex.kind];
        const shape = this.styleParameters.vertex_shapes[vertex.kind];
        return {
            label: this.styleParameters.vertex_default_label(vertex),
            color: color,
            size: vertex.kind === "roleType" ? 5 : this.styleParameters.vertex_size,
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
        const color = this.styleParameters.edge_color;
        return {
            label: label,
            color: color.hex(),
            size: this.styleParameters.edge_size,
            type: "arrow",
            metadata: metadata,
        }
    }

    private edgeKey(from_id: string, to_id: string, edge_type_id: string) : string {
        return `${from_id}:${to_id}:${edge_type_id}`;
    }

    private shouldCreateNode(queryVertex: QueryVertexOrSpecial) {
        return shouldCreateNode(this.queryStructure, queryVertex);
    }

    private shouldCreateEdge(edge: DataConstraintAny, from: QueryVertexOrSpecial, to: QueryVertexOrSpecial) {
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

    private maybeCreateEdge(answerIndex: number, edge: DataConstraintAny, label: string, from: DataVertex, to: DataVertex, queryFrom: QueryVertexOrSpecial, queryTo: QueryVertexOrSpecial) {
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

    // ILogicalGraphConverter
    // Vertices
    put_vertex(answerIndex: number, vertex: DataVertex, queryVertex: QueryVertexOrSpecial): string {
        const key = vertexMapKey(vertex);
        if (this.shouldCreateNode(queryVertex)) {
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
        let expressionVertex: VertexExpression = {
            tag: "expression",
            kind: "expression",
            answerIndex: answerIndex,
            repr: constraint.text,
            vertex_map_key: `expr_${answerIndex}_${constraint.queryCoordinates.branch}_{${constraint.queryCoordinates.constraint}}`
        }
        expression.assigned
            .forEach((assigned, i) => {
                let queryVertex = constraint.queryConstraint.assigned[i];
                let varNameOrId = get_variable_name(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `assign[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, expressionVertex, assigned, expressionVertex, queryVertex);
            });
        expression.arguments
            .forEach((arg, i) => {
                let queryVertex = constraint.queryConstraint.arguments[i];
                let varNameOrId = get_variable_name(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `arg[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, arg, expressionVertex, queryVertex, expressionVertex);
            });
    }

    put_function(answerIndex: number, constraint: DataConstraintFunction): void {
        let functionCall = constraint;
        let functionVertexKey = `f_${answerIndex}_${constraint.queryCoordinates.branch}_{${constraint.queryCoordinates.constraint}}`;
        let functionVertex: VertexFunction = {
            tag: "functionCall",
            kind: "functionCall", answerIndex: answerIndex,
            repr: constraint.name,
            vertex_map_key: functionVertexKey
        }
        functionCall.assigned
            .forEach((assigned, i) => {
                let queryVertex = constraint.queryConstraint.assigned[i];
                let varNameOrId = get_variable_name(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `assign[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, functionVertex, assigned, functionVertex, queryVertex);
            });
        functionCall.arguments
            .forEach((arg, i) => {
                let queryVertex = constraint.queryConstraint.arguments[i];
                let varNameOrId = get_variable_name(this.queryStructure, queryVertex) ?? `$_${queryVertex.id}`;
                let label = `arg[${varNameOrId}]`;
                this.maybeCreateEdge(answerIndex, constraint, label, arg, functionVertex, queryVertex, functionVertex);
            });
    }
}

export function shouldCreateNode(structure: QueryStructure, vertex: QueryVertexOrSpecial) {
    return !(
        (vertex.tag === "label" ||
            (vertex.tag == "variable" && !structure.outputs.includes(vertex.id))
        )
    );
}

export function shouldCreateEdge(structure: QueryStructure, _edge: QueryConstraintAny, from: QueryVertexOrSpecial, to: QueryVertexOrSpecial) {
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
