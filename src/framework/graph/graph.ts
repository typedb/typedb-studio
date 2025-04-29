import { ConceptRow } from "../typedb-driver/answer";
import { ConceptRowsQueryResponse } from "../typedb-driver/response";
import {
    Attribute,
    ConceptAny,
    EdgeKind,
    ObjectAny,
    RoleType,
    ThingKind,
    TypeAny,
    TypeDBValue,
    TypeKind,
    ValueKind
} from "./typedb/concept";
import {
    TypeDBRowData,
    StructureEdge,
    StructureEdgTypeAny,
    StructureVertex,
    StructureVertexKind, StructureVertexLabel,
    StructureVertexVariable, StructureVertexUnavailable, StructureVertexFunction, StructureVertexExpression
} from "./typedb/answer"

//////////////////////////
// Logical TypeDB Graph //
//////////////////////////

export enum SpecialVertexKind {
    unavailable = "unavailable",
    expr = "expression",
    func = "functionCall",
}

export type VertexUnavailable = { kind: SpecialVertexKind.unavailable, variable: string, answerIndex: number, vertex_map_key: string };
export type VertexExpression = { kind: SpecialVertexKind.expr, repr: string, answerIndex: number, vertex_map_key: string };
export type VertexFunction = { kind: SpecialVertexKind.func, repr: string, answerIndex: number, vertex_map_key: string };
export type LogicalVertexSpecial = VertexUnavailable | VertexFunction | VertexExpression;
export type EdgeParameter = RoleType | VertexUnavailable | string | null;

export type LogicalVertexKind = ThingKind | TypeKind | ValueKind | SpecialVertexKind;
export type LogicalVertex = ConceptAny | LogicalVertexSpecial;
export type LogicalVertexID = string;

export type StructureEdgeCoordinates = { branchIndex: number, constraintIndex: number };
export type LogicalEdge = { structureEdgeCoordinates: StructureEdgeCoordinates, type: LogicalEdgeType, from: LogicalVertexID, to: LogicalVertexID };
export type LogicalEdgeType = { kind: EdgeKind, param: EdgeParameter };

export type VertexMap = Map<LogicalVertexID, LogicalVertex>;

export type LogicalGraph = {
  vertices: VertexMap;
  answers: Array<Array<LogicalEdge>>;
}

///////////////////////////////////
// TypeDB server -> logical graph
///////////////////////////////////
export function constructGraphFromRowsResult(rows_result: ConceptRowsQueryResponse) : LogicalGraph {
    return new LogicalGraphBuilder().build(rows_result);
}

function is_branch_involved(provenanceBitArray: Array<number>, branchIndex: number) {
    let provenanceByteIndex = branchIndex >> 3; // divide by 8
    let provenanceBitWithinByte = (1 << (branchIndex % 8));
    return 0 == branchIndex || 0 != (provenanceBitArray[provenanceByteIndex] & provenanceBitWithinByte)
}

class LogicalGraphBuilder {
    vertexMap: VertexMap;
    answers : Array<Array<LogicalEdge>> = [];
    constructor() {
        this.vertexMap = new Map();
        this.answers = [];
    }

    build(rows_result: ConceptRowsQueryResponse) : LogicalGraph {
        rows_result.answers.forEach((row, answerIndex) => {
            let current_answer_edges: Array<LogicalEdge> = [];
            rows_result.queryStructure!.branches.forEach((branch, branchIndex) => {
                if ( is_branch_involved(row.provenanceBitArray, branchIndex) ){
                    current_answer_edges.push(...this.substitute_variables(branchIndex, answerIndex, branch.edges, row.data))
                }
            });
            this.answers.push(current_answer_edges);
        });
        return { vertices: this.vertexMap, answers: this.answers };
    }

    substitute_variables(branchIndex: number, answerIndex: number, branch: Array<StructureEdge>, data: ConceptRow) : Array<LogicalEdge> {
        return branch.map((structure_edge, constraintIndex) => {
            let coordinates = { branchIndex: branchIndex, constraintIndex: constraintIndex } ;
            let edge_type = this.extract_edge_type(structure_edge.type, answerIndex, data);
            let from = this.register_vertex(structure_edge.from, answerIndex, data);
            let to = this.register_vertex(structure_edge.to, answerIndex, data);
            return { structureEdgeCoordinates: coordinates, type: edge_type, from: from, to: to }
        });
    }

    register_vertex(structure_vertex: StructureVertex, answerIndex: number, data: ConceptRow): LogicalVertexID {
        let vertex = this.translate_vertex(structure_vertex, answerIndex, data);
        let key = null;
        switch (vertex.kind) {
            case ThingKind.attribute:{
                let attribute = vertex as Attribute;
                key = attribute.type.label + ":" + attribute.value;
                break;
            }
            case ThingKind.entity:
            case ThingKind.relation: {
                key = (vertex as ObjectAny).iid;
                break;
            }
            case TypeKind.attributeType:
            case TypeKind.entityType:
            case TypeKind.relationType:
            case TypeKind.roleType: {
                key = (vertex as TypeAny).label;
                break;
            }
            case "value": {
                let value = vertex as TypeDBValue;
                key = (value.valueType + ":" + value.value);
                break;
            }
            case SpecialVertexKind.unavailable: {
                key = (vertex as VertexUnavailable).vertex_map_key;
                break;
            }
            case SpecialVertexKind.func: {
                key = (vertex as VertexFunction).vertex_map_key;
                break;
            }
            case SpecialVertexKind.expr:{
                key = (vertex as VertexExpression).vertex_map_key;
                break;
            }
        }
        let vertex_id = key;
        this.vertexMap.set(vertex_id, vertex);
        return vertex_id;
    }

    translate_vertex(structure_vertex: StructureVertex, answerIndex: number, data: ConceptRow): LogicalVertex {
        switch (structure_vertex.kind) {
            case StructureVertexKind.variable: {
                return data[(structure_vertex.value as StructureVertexVariable).variable] as ConceptAny;
            } 
            case StructureVertexKind.label:{
                let vertex= structure_vertex.value as StructureVertexLabel;
                return { kind: vertex.kind, label: vertex.label } as TypeAny;
            }
            case StructureVertexKind.value:{
                return structure_vertex.value as TypeDBValue;
            }
            case StructureVertexKind.unavailable: {
                let vertex = structure_vertex.value as StructureVertexUnavailable;
                let key = "unavailable[" + vertex.variable + "][" + answerIndex + "]";
                return { kind: "unavailable", vertex_map_key: key, answerIndex: answerIndex, variable: vertex.variable } as VertexUnavailable;
            }
            case StructureVertexKind.expr: {
                let vertex = structure_vertex.value as StructureVertexExpression;
                let key = vertex.repr + "[" + answerIndex + "]";
                return { kind: SpecialVertexKind.expr , vertex_map_key: key, answerIndex: answerIndex, repr: vertex.repr } as VertexExpression;
            }
            case StructureVertexKind.func:{
                let vertex = structure_vertex.value as StructureVertexFunction;
                let key = vertex.repr + "[" + answerIndex + "]";
                return { kind: SpecialVertexKind.func, vertex_map_key: key, answerIndex: answerIndex, repr: vertex.repr } as VertexFunction;
            }
            default: {
                throw new Error("Unsupported vertex type: " + structure_vertex.kind);
            }
        }
    }

    extract_edge_type(structure_edge_type: StructureEdgTypeAny, answerIndex: number, data: ConceptRow): LogicalEdgeType {
        switch (structure_edge_type.kind) {
            case EdgeKind.isa:
            case EdgeKind.has:
            case EdgeKind.sub:
            case EdgeKind.owns:
            case EdgeKind.relates:
            case EdgeKind.plays:
            case EdgeKind.isaExact:
            case EdgeKind.subExact:
            {
                return { kind: structure_edge_type.kind, param: null };
            }
            case EdgeKind.links: {
                let role = this.translate_vertex(structure_edge_type.param as StructureVertex, answerIndex, data);
                return { kind: structure_edge_type.kind, param: role as RoleType | VertexUnavailable };
            }
            case EdgeKind.assigned:
            case EdgeKind.argument:{
                return { kind: structure_edge_type.kind, param: structure_edge_type.param as string };
            }
            default: {
                console.log("Unsupported EdgeKind:"+ structure_edge_type)
                throw new Error("Unsupported EdgeKind:"+ structure_edge_type.kind);
            }
        }
    }
}
