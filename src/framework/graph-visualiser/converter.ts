import { Attribute, AttributeType, Entity, EntityType, Relation, RelationType, RoleType, Type, Value } from "../typedb-driver/concept";
import { Edge, QueryStructure } from "../typedb-driver/query-structure";
import {LogicalVertex, StructureEdgeCoordinates, VertexExpression, VertexFunction, VertexUnavailable} from "./graph";
import chroma from "chroma-js";
import {ILogicalGraphConverter} from "./visualisation";
import MultiGraph from "graphology";
import {StudioConverterStructureParameters, StudioConverterStyleParameters} from "./config";

export class StudioConverter implements ILogicalGraphConverter {
    graph: MultiGraph;
    styleParameters: StudioConverterStyleParameters;
    structureParameters: StudioConverterStructureParameters;
    edgesToDraw: Array<Array<number>>;
    isFollowupQuery: boolean;

    constructor(graph: MultiGraph, queryStructure: QueryStructure, isFollowupQuery: boolean, structureParameters: StudioConverterStructureParameters, styleParameters: StudioConverterStyleParameters) {
        this.graph = graph;
        this.edgesToDraw = determineEdgesToDraw(queryStructure, structureParameters);
        this.isFollowupQuery = isFollowupQuery;
        this.styleParameters = styleParameters;
        this.structureParameters = structureParameters;
    }

    private vertexAttributes(vertex: LogicalVertex): any {
        // Extend as you please: https://www.sigmajs.org/docs/advanced/data/
        let color = this.styleParameters.vertex_colors[vertex.kind];
        let shape = this.styleParameters.vertex_shapes[vertex.kind];
        return {
            label: this.styleParameters.vertex_default_label(vertex),
            color: color.hex(),
            size: this.styleParameters.vertex_size,
            type: shape,
            x: Math.random(),
            y: Math.random(),
            metadata: {
                defaultLabel: this.styleParameters.vertex_default_label(vertex),
                hoverLabel: this.styleParameters.vertex_hover_label(vertex),
                concept: vertex,
            },
        }
    }

    private edgeMetadata(answerIndex: number, coordinates: StructureEdgeCoordinates) {
        if (this.isFollowupQuery) {
            return { answerIndex: -1, structureEdgeCoordinates: coordinates };
        } else {
            return { answerIndex: answerIndex, structureEdgeCoordinates: coordinates };
        }
    }

    private edgeAttributes(label: string, metadata: any | undefined): any {
        // Extend as you please: https://www.sigmajs.org/docs/advanced/data/
        let color = this.styleParameters.edge_color;
        return {
            label: label,
            color: color.hex(),
            size: this.styleParameters.edge_size,
            type: "arrow",
            metadata: metadata,
        }
    }

    private edgeKey(from_id: string, to_id: string, edge_type_id: string) : string {
        return from_id + ":" + to_id + ":" + edge_type_id;
    }

    private  mayAddNode(structureEdgeCoordinates: StructureEdgeCoordinates, key: string, attributes: any) {
        if (this.shouldDrawEdge(structureEdgeCoordinates)) {
            if (!this.graph.hasNode(key)) {
                this.graph.addNode(key, attributes);
            }
        }
    }

    private mayAddEdge(coordinates: StructureEdgeCoordinates, from: string, to:string, edge_label:string, attributes: any) {
        if (this.shouldDrawEdge(coordinates)) {
            let key = this.edgeKey(from, to, edge_label);
            if (!this.graph.hasDirectedEdge(key)) {
                // TODO: If there is an edge between the two vertices, make it curved
                if (this.graph.hasDirectedEdge(from, to)) {
                    attributes.type = "curved"
                }
                this.graph.addDirectedEdgeWithKey(key, from, to, attributes)
            }
        }
    }

    // ILogicalGraphConverter
    // Vertices
    put_attribute(answerIndex: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: Attribute): void {
        this.mayAddNode(structureEdgeCoordinates, safe_attribute(vertex), this.vertexAttributes(vertex));
    }

    put_entity(answerIndex: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: Entity): void {
        this.mayAddNode(structureEdgeCoordinates, vertex.iid, this.vertexAttributes(vertex));
    }

    put_relation(answerIndex: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: Relation): void {
        this.mayAddNode(structureEdgeCoordinates, vertex.iid, this.vertexAttributes(vertex));
    }
    put_attribute_type(answerIndex: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: AttributeType): void {
        this.mayAddNode(structureEdgeCoordinates, vertex.label, this.vertexAttributes(vertex));
    }
    put_entity_type(answerIndex: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: EntityType): void {
        this.mayAddNode(structureEdgeCoordinates, vertex.label, this.vertexAttributes(vertex));
    }
    put_relation_type(answerIndex: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: RelationType): void {
        this.mayAddNode(structureEdgeCoordinates, vertex.label, this.vertexAttributes(vertex));
    }

    put_role_type_for_type_constraint(answerIndex: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: RoleType): void {
        let label = vertex.label;
        if (!this.graph.hasNode(vertex.label)) {
            this.graph.addNode(vertex.label, { label: label, color: chroma('darkorange').alpha(0.5).hex(), size: 5, x: Math.random(), y: Math.random() });
        }
    }

    put_vertex_value(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: Value): void {
        this.mayAddNode(structureEdgeCoordinates, safe_value(vertex), this.vertexAttributes(vertex));
    }

    put_vertex_expression(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: VertexExpression): void {
        this.mayAddNode(structureEdgeCoordinates, vertex.vertex_map_key, this.vertexAttributes(vertex));
    }

    put_vertex_function(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: VertexFunction): void {
        this.mayAddNode(structureEdgeCoordinates, vertex.vertex_map_key, this.vertexAttributes(vertex));
    }

    put_vertex_unavailable(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: VertexUnavailable): void {
        this.mayAddNode(structureEdgeCoordinates, unavailable_key(vertex), this.vertexAttributes(vertex));
    }

    // Edges
    put_isa(answerIndex: number, coordinates: StructureEdgeCoordinates, thing: Entity | Relation | Attribute, type: EntityType | RelationType | AttributeType): void {
        let attributes = this.edgeAttributes("isa", this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_iid(thing), safe_label(type), "isa", attributes);
    }

    put_has(answerIndex: number, coordinates: StructureEdgeCoordinates, owner: Entity | Relation, attribute: Attribute): void {
        let attributes = this.edgeAttributes("has", this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates,safe_iid(owner), safe_attribute(attribute), "has", attributes);
    }

    put_links(answerIndex: number, coordinates: StructureEdgeCoordinates, relation: Relation, player: Entity | Relation, role: RoleType | VertexUnavailable): void {
        let role_label = (role.kind == "roleType") ? safe_role_name(role) : (`links_[${coordinates.branchIndex},${coordinates.constraintIndex}]`);
        let attributes = this.edgeAttributes(role_label, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_iid(relation), safe_iid(player), role_label, attributes);
    }

    put_sub(answerIndex: number, coordinates: StructureEdgeCoordinates, subtype: EntityType | RelationType | AttributeType, supertype: EntityType | RelationType | AttributeType): void {
        let attributes = this.edgeAttributes("sub", coordinates);
        this.mayAddEdge(coordinates, safe_label(subtype), safe_label(supertype), "sub", attributes);
    }

    put_owns(answerIndex: number, coordinates: StructureEdgeCoordinates, owner: EntityType | RelationType, attribute: AttributeType): void {
        let attributes = this.edgeAttributes("owns", this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_label(owner), safe_label(attribute), "owns", attributes);
    }

    put_relates(answerIndex: number, coordinates: StructureEdgeCoordinates, relation: RelationType, role: RoleType): void {
        let attributes = this.edgeAttributes("relates", this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_label(relation), safe_label(role), "relates", attributes);
    }

    put_plays(answerIndex: number, coordinates: StructureEdgeCoordinates, player: EntityType | RelationType, role: RoleType): void {
        let attributes = this.edgeAttributes("plays", this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_label(player), safe_label(role), "plays", attributes);
    }

    put_isa_exact(answerIndex: number, coordinates: StructureEdgeCoordinates, thing: Entity | Relation | Attribute, type: EntityType | RelationType | AttributeType): void {
        let attributes = this.edgeAttributes("isaExact", this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_iid(thing), safe_label(type), "isaExact", attributes);
    }

    put_sub_exact(answerIndex: number, coordinates: StructureEdgeCoordinates, subtype: EntityType | RelationType | AttributeType, supertype: EntityType | RelationType | AttributeType): void {
        let attributes = this.edgeAttributes("subExact", this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_label(subtype), safe_label(supertype), "subExact", attributes);
    }

    put_assigned(answerIndex: number, coordinates: StructureEdgeCoordinates, expr_or_func: VertexExpression | VertexFunction, assigned: Value, var_name: string): void {
        let label = "assign[" + var_name + "]";
        let attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, expr_or_func.vertex_map_key, safe_value(assigned), "assigned", attributes);
    }

    put_argument(answerIndex: number, coordinates: StructureEdgeCoordinates, argument: Value | Attribute, expr_or_func: VertexExpression | VertexFunction, var_name: string): void {
        let label = "arg[" + var_name + "]";
        let attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, coordinates));
        let from_vertex_key = null;
        switch (argument.kind) {
            case "value": {
                from_vertex_key = safe_value(argument);
                break;
            }
            case "attribute": {
                from_vertex_key = safe_iid(argument);
                break;
            }
        }
        this.mayAddEdge(coordinates, from_vertex_key, expr_or_func.vertex_map_key, "argument", attributes);
    }

    private shouldDrawEdge(edgeCoordinates: StructureEdgeCoordinates) {
        return this.edgesToDraw[edgeCoordinates.branchIndex].includes(edgeCoordinates.constraintIndex);
    }
}

function determineEdgesToDraw(queryStructure: QueryStructure, structureParameters: StudioConverterStructureParameters): number[][] {
    let edgesToDraw: Array<Array<number>> = [];
    queryStructure.branches.forEach((_) => {
        edgesToDraw.push([]);
    });
    queryStructure.branches.flatMap((branch, branchIndex) =>
        branch.edges.map((edge, constraintIndex) => {
            return {edge: edge, coordinates: {branchIndex: branchIndex, constraintIndex: constraintIndex}};
        })
    ).filter((element) => mustDrawEdge(element.edge, structureParameters))
    .forEach((element) => {
        edgesToDraw[element.coordinates.branchIndex].push(element.coordinates.constraintIndex);
    });
    return edgesToDraw;
}

export function mustDrawEdge(edge: Edge, structureParameters: StudioConverterStructureParameters): boolean {
    const isLabelledEdge = edge.from.kind == "label" || edge.to.kind == "label";
    return !isLabelledEdge || !structureParameters.ignoreEdgesInvolvingLabels.includes(edge.type.kind);
}

function safe_iid(vertex: Entity | Relation | Attribute | VertexUnavailable) {
    return (vertex.kind == "unavailable") ? unavailable_key(vertex) : vertex.iid;
}

function safe_label(vertex: Type | VertexUnavailable) {
    return (vertex.kind == "unavailable") ? unavailable_key(vertex) : vertex.label;
}

function safe_value(vertex: Value | VertexUnavailable) {
    return vertex.kind == "unavailable" ? unavailable_key(vertex) : `${vertex.valueType}:${vertex.value}`;
}

function safe_attribute(vertex: Attribute | VertexUnavailable) {
    return vertex.kind == "unavailable" ? unavailable_key(vertex) : `${vertex.type.label}:${vertex.value}`;
}

function safe_role_name(vertex: RoleType | VertexUnavailable) {
    if (vertex.kind == "unavailable") {
        return unavailable_key(vertex);
    } else {
        let parts = vertex.label.split(":");
        return parts[parts.length - 1];
    }
}

export function unavailable_key(vertex: VertexUnavailable): string {
    return `unavailable[${vertex.variable}][${vertex.answerIndex}]`;
}
