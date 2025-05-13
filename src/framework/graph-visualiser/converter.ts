import { Attribute, AttributeType, Entity, EntityType, Relation, RelationType, RoleType, Type, Value } from "../typedb-driver/concept";
import { QueryEdge, QueryStructure, QueryVertex, QueryVertexKind } from "../typedb-driver/query-structure";
import { EdgeAttributes, EdgeMetadata, DataVertex, QueryCoordinates, VertexAttributes, VertexExpression, VertexFunction, VertexMetadata, VertexUnavailable, VisualGraph, DataEdge } from "./graph";
import {ILogicalGraphConverter} from "./visualisation";
import {StudioConverterStructureParameters, StudioConverterStyleParameters} from "./config";

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
            color: color.hex(),
            size: vertex.kind === "roleType" ? 5 : this.styleParameters.vertex_size,
            type: shape,
            x: Math.random(),
            y: Math.random(),
            metadata: this.vertexMetadata(vertex),
            highlighted: false,
        }
    }

    private edgeMetadata(answerIndex: number, edge: DataEdge): EdgeMetadata {
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

    private maybeCreateEdge(edge: DataEdge, from: string, to:string, edge_label:string, attributes: EdgeAttributes) {
        if (this.shouldCreateEdge(edge)) {
            let key = this.edgeKey(from, to, edge_label);
            if (!this.graph.hasDirectedEdge(key)) {
                // TODO: If there is an edge between the two vertices, make it curved
                if (this.graph.hasDirectedEdge(from, to)) {
                    attributes.type = "curved";
                }
                this.graph.addDirectedEdgeWithKey(key, from, to, attributes);
            }
        }
    }

    // ILogicalGraphConverter
    // Vertices
    put_vertex(answerIndex: number, vertex: DataVertex, queryVertex: QueryVertex): void {
        if (!this.shouldCreateNode(vertex, queryVertex)) return;

        const key = vertexMapKey(vertex);
        if (this.graph.hasNode(key)) return;

        this.graph.addNode(key, this.vertexAttributes(vertex));
    }

    // Edges
    put_isa(answerIndex: number, edge: DataEdge, thing: Entity | Relation | Attribute, type: EntityType | RelationType | AttributeType): void {
        const attributes = this.edgeAttributes("isa", this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(thing), vertexMapKey(type), "isa", attributes);
    }

    put_has(answerIndex: number, edge: DataEdge, owner: Entity | Relation, attribute: Attribute): void {
        let attributes = this.edgeAttributes("has", this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(owner), vertexMapKey(attribute), "has", attributes);
    }

    put_links(answerIndex: number, edge: DataEdge, relation: Relation, player: Entity | Relation, role: RoleType | VertexUnavailable): void {
        const roleLabel = role.kind === "roleType" ? role.label.split(":").at(-1) : `?`;
        if (!roleLabel) throw `${this.put_links.name}: invalid role label '${JSON.stringify(role)}'`;
        let attributes = this.edgeAttributes(roleLabel, this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(relation), vertexMapKey(player), roleLabel, attributes);
    }

    put_sub(answerIndex: number, edge: DataEdge, subtype: EntityType | RelationType | AttributeType, supertype: EntityType | RelationType | AttributeType): void {
        const attributes = this.edgeAttributes("sub", this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(subtype), vertexMapKey(supertype), "sub", attributes);
    }

    put_owns(answerIndex: number, edge: DataEdge, owner: EntityType | RelationType, attribute: AttributeType): void {
        let attributes = this.edgeAttributes("owns", this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(owner), vertexMapKey(attribute), "owns", attributes);
    }

    put_relates(answerIndex: number, edge: DataEdge, relation: RelationType, role: RoleType): void {
        let attributes = this.edgeAttributes("relates", this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(relation), vertexMapKey(role), "relates", attributes);
    }

    put_plays(answerIndex: number, edge: DataEdge, player: EntityType | RelationType, role: RoleType): void {
        let attributes = this.edgeAttributes("plays", this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(player), vertexMapKey(role), "plays", attributes);
    }

    put_isa_exact(answerIndex: number, edge: DataEdge, thing: Entity | Relation | Attribute, type: EntityType | RelationType | AttributeType): void {
        let attributes = this.edgeAttributes("isaExact", this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(thing), vertexMapKey(type), "isaExact", attributes);
    }

    put_sub_exact(answerIndex: number, edge: DataEdge, subtype: EntityType | RelationType | AttributeType, supertype: EntityType | RelationType | AttributeType): void {
        let attributes = this.edgeAttributes("subExact", this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, vertexMapKey(subtype), vertexMapKey(supertype), "subExact", attributes);
    }

    put_assigned(answerIndex: number, edge: DataEdge, expr_or_func: VertexExpression | VertexFunction, assigned: Value, var_name: string): void {
        let label = "assign[" + var_name + "]";
        let attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, edge));
        this.maybeCreateEdge(edge, expr_or_func.vertex_map_key, vertexMapKey(assigned), "assigned", attributes);
    }

    put_argument(answerIndex: number, edge: DataEdge, argument: Value | Attribute, expr_or_func: VertexExpression | VertexFunction, var_name: string): void {
        const label = `arg[${var_name}]`;
        const attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, edge));
        let from_vertex_key = null;
        switch (argument.kind) {
            case "value": {
                from_vertex_key = vertexMapKey(argument);
                break;
            }
            case "attribute": {
                from_vertex_key = vertexMapKey(argument);
                break;
            }
        }
        this.maybeCreateEdge(edge, from_vertex_key, expr_or_func.vertex_map_key, "argument", attributes);
    }

    private shouldCreateNode(vertex: DataVertex, queryVertex: QueryVertex) {
        return shouldCreateNode(queryVertex);
    }

    private shouldCreateEdge(edge: DataEdge) {
        return shouldCreateEdge(edge.queryEdge);
    }
}

export function shouldCreateNode(vertex: QueryVertex) {
    return !["unavailableVariable", "label"].includes(vertex.kind);
}

export function shouldCreateEdge(edge: QueryEdge) {
    return shouldCreateNode(edge.from) && shouldCreateNode(edge.to);
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
