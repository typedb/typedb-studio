import {LogicalVertex, StructureEdgeCoordinates, VertexExpression, VertexFunction, VertexUnavailable} from "../graph";
import chroma from "chroma-js";
import {
    Attribute,
    AttributeType,
    EdgeKind,
    Entity,
    EntityType,
    ObjectAny,
    ObjectType,
    Relation,
    RelationType,
    RoleType, ThingKind, TypeAny, TypeDBValue,
    TypeKind
} from "../typedb/concept";
import {ILogicalGraphConverter} from "../visualisation";
import MultiGraph from "graphology";
import {StudioConverterStructureParameters, StudioConverterStyleParameters} from "./config";
import {StructureEdge, StructureVertexKind, TypeDBQueryStructure} from "../typedb/answer";

export class StudioConverter implements ILogicalGraphConverter {
    graph: MultiGraph;
    styleParameters: StudioConverterStyleParameters;
    structureParameters: StudioConverterStructureParameters;
    edgesToDraw: Array<Array<number>>;
    isFollowupQuery: boolean;

    constructor(graph: MultiGraph, queryStructure: TypeDBQueryStructure, isFollowupQuery: boolean, structureParameters: StudioConverterStructureParameters, styleParameters: StudioConverterStyleParameters) {
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

    put_vertex_value(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: TypeDBValue): void {
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
    put_isa(answerIndex: number, coordinates: StructureEdgeCoordinates, thing: Attribute | ObjectAny, type: AttributeType | ObjectType): void {
        let attributes = this.edgeAttributes(EdgeKind.isa, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_iid(thing), safe_label(type), EdgeKind.isa, attributes);
    }

    put_has(answerIndex: number, coordinates: StructureEdgeCoordinates, owner: Entity | Relation, attribute: Attribute): void {
        let attributes = this.edgeAttributes(EdgeKind.has, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates,safe_iid(owner), safe_attribute(attribute), EdgeKind.has, attributes);
    }

    put_links(answerIndex: number, coordinates: StructureEdgeCoordinates, relation: Relation, player: Entity | Relation, role: RoleType | VertexUnavailable): void {
        let role_label = (role.kind == TypeKind.roleType) ?
            safe_role_name(role as RoleType) :
            ("links_[" + coordinates.branchIndex + "," + coordinates.constraintIndex + "]");
        let attributes = this.edgeAttributes(role_label, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_iid(relation), safe_iid(player), role_label, attributes);
    }

    put_sub(answerIndex: number, coordinates: StructureEdgeCoordinates, subtype: AttributeType | ObjectType, supertype: AttributeType | ObjectType): void {
        let attributes = this.edgeAttributes(EdgeKind.sub, coordinates);
        this.mayAddEdge(coordinates, safe_label(subtype), safe_label(supertype), EdgeKind.sub, attributes);
    }

    put_owns(answerIndex: number, coordinates: StructureEdgeCoordinates, owner: ObjectType, attribute: AttributeType): void {
        let attributes = this.edgeAttributes(EdgeKind.owns, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_label(owner), safe_label(attribute), EdgeKind.owns, attributes);
    }

    put_relates(answerIndex: number, coordinates: StructureEdgeCoordinates, relation: RelationType, role: RoleType): void {
        let attributes = this.edgeAttributes(EdgeKind.relates, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_label(relation), safe_label(role), EdgeKind.relates, attributes);
    }

    put_plays(answerIndex: number, coordinates: StructureEdgeCoordinates, player: EntityType | RelationType, role: RoleType): void {
        let attributes = this.edgeAttributes(EdgeKind.plays, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_label(player), safe_label(role), EdgeKind.plays, attributes);
    }

    put_isa_exact(answerIndex: number, coordinates: StructureEdgeCoordinates, thing: Attribute | ObjectAny, type: AttributeType | ObjectType): void {
        let attributes = this.edgeAttributes(EdgeKind.isaExact, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_iid(thing), safe_label(type), EdgeKind.isaExact, attributes);
    }

    put_sub_exact(answerIndex: number, coordinates: StructureEdgeCoordinates, subtype: AttributeType | ObjectType, supertype: AttributeType | ObjectType): void {
        let attributes = this.edgeAttributes(EdgeKind.subExact, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, safe_label(subtype), safe_label(supertype), EdgeKind.subExact, attributes);
    }

    put_assigned(answerIndex: number, coordinates: StructureEdgeCoordinates, expr_or_func: VertexExpression | VertexFunction, assigned: TypeDBValue, var_name: string): void {
        let label = "assign[" + var_name + "]";
        let attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, coordinates));
        this.mayAddEdge(coordinates, expr_or_func.vertex_map_key, safe_value(assigned), EdgeKind.assigned, attributes);
    }

    put_argument(answerIndex: number, coordinates: StructureEdgeCoordinates, argument: TypeDBValue | Attribute, expr_or_func: VertexExpression | VertexFunction, var_name: string): void {
        let label = "arg[" + var_name + "]";
        let attributes = this.edgeAttributes(label, this.edgeMetadata(answerIndex, coordinates));
        let from_vertex_key = null;
        switch (argument.kind) {
            case "value": {
                from_vertex_key = safe_value(argument);
                break;
            }
            case ThingKind.attribute: {
                from_vertex_key = safe_iid(argument);
                break;
            }
        }
        this.mayAddEdge(coordinates, from_vertex_key, expr_or_func.vertex_map_key, EdgeKind.argument, attributes);
    }

    private shouldDrawEdge(edgeCoordinates: StructureEdgeCoordinates) {
        return this.edgesToDraw[edgeCoordinates.branchIndex].includes(edgeCoordinates.constraintIndex);
    }
}

function determineEdgesToDraw(queryStructure: TypeDBQueryStructure, structureParameters: StudioConverterStructureParameters): Array<Array<number>> {
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

export function mustDrawEdge(edge: StructureEdge, structureParameters: StudioConverterStructureParameters) : boolean {
    let isLabelledEdge = (edge.from.kind == StructureVertexKind.label || edge.to.kind == StructureVertexKind.label);
    if (isLabelledEdge && structureParameters.ignoreEdgesInvolvingLabels.includes(edge.type.kind)) {
        return false;
    }
    return true;
}

function safe_iid(vertex: ObjectAny | Attribute | VertexUnavailable) {
    return (vertex.kind == "unavailable") ? unavailable_key(vertex) : vertex.iid;
}

function safe_label(vertex: TypeAny | VertexUnavailable) {
    return (vertex.kind == "unavailable") ? unavailable_key(vertex) : vertex.label;
}

function safe_value(vertex: TypeDBValue | VertexUnavailable) {
    return (vertex.kind == "unavailable") ? unavailable_key(vertex) : (vertex.valueType + ":" + vertex.value);
}

function safe_attribute(vertex: Attribute | VertexUnavailable) {
    return (vertex.kind == "unavailable")? unavailable_key(vertex) : (vertex.type.label + ":" + vertex.value);
}

function safe_role_name(vertex: RoleType | VertexUnavailable) {
    if (vertex.kind == "unavailable") {
        return unavailable_key(vertex);
    } else {
        let parts = (vertex as RoleType).label.split(":");
        return parts[parts.length - 1];
    }
}

export function unavailable_key(vertex: VertexUnavailable) : string {
    return "unavailable[" + vertex.variable + "][" + vertex.answerIndex + "]";
}
