import { Attribute, Concept, RoleType, ThingKind, Type, TypeKind, ValueKind } from "../typedb-driver/concept";
import { EdgeKind, Edge, EdgeType, Vertex } from "../typedb-driver/query-structure";
import { ConceptRow, ConceptRowsQueryResponse } from "../typedb-driver/response";

//////////////////////////
// Logical TypeDB Graph //
//////////////////////////

export type SpecialVertexKind = "unavailable" | "expression" | "functionCall";

export type VertexUnavailable = { kind: "unavailable", variable: string, answerIndex: number, vertex_map_key: string };
export type VertexExpression = { kind: "expression", repr: string, answerIndex: number, vertex_map_key: string };
export type VertexFunction = { kind: "functionCall", repr: string, answerIndex: number, vertex_map_key: string };
export type LogicalVertexSpecial = VertexUnavailable | VertexFunction | VertexExpression;
export type EdgeParameter = RoleType | VertexUnavailable | string | null;

export type LogicalVertexKind = ThingKind | TypeKind | ValueKind | SpecialVertexKind;
export type LogicalVertex = Concept | LogicalVertexSpecial;
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

    substitute_variables(branchIndex: number, answerIndex: number, branch: Array<Edge>, data: ConceptRow) : Array<LogicalEdge> {
        return branch.map((structure_edge, constraintIndex) => {
            let coordinates = { branchIndex: branchIndex, constraintIndex: constraintIndex } ;
            let edge_type = this.extract_edge_type(structure_edge.type, answerIndex, data);
            let from = this.register_vertex(structure_edge.from, answerIndex, data);
            let to = this.register_vertex(structure_edge.to, answerIndex, data);
            return { structureEdgeCoordinates: coordinates, type: edge_type, from: from, to: to }
        });
    }

    register_vertex(structure_vertex: Vertex, answerIndex: number, data: ConceptRow): LogicalVertexID {
        let vertex = this.translate_vertex(structure_vertex, answerIndex, data);
        let key = null;
        switch (vertex.kind) {
            case "attribute": {
                key = vertex.type.label + ":" + vertex.value;
                break;
            }
            case "entity":
            case "relation": {
                key = vertex.iid;
                break;
            }
            case "attributeType":
            case "entityType":
            case "relationType":
            case "roleType": {
                key = vertex.label;
                break;
            }
            case "value": {
                key = (vertex.valueType + ":" + vertex.value);
                break;
            }
            case "unavailable": {
                key = vertex.vertex_map_key;
                break;
            }
            case "functionCall": {
                key = vertex.vertex_map_key;
                break;
            }
            case "expression": {
                key = vertex.vertex_map_key;
                break;
            }
        }
        let vertex_id = key;
        this.vertexMap.set(vertex_id, vertex);
        return vertex_id;
    }

    translate_vertex(structure_vertex: Vertex, answerIndex: number, data: ConceptRow): LogicalVertex {
        switch (structure_vertex.kind) {
            case "variable": {
                return data[structure_vertex.value.variable] as Concept;
            } 
            case "label": {
                let vertex= structure_vertex.value;
                return { kind: vertex.kind, label: vertex.label } as Type;
            }
            case "value": {
                return structure_vertex.value;
            }
            case "unavailableVariable": {
                let vertex = structure_vertex.value;
                let key = "unavailable[" + vertex.variable + "][" + answerIndex + "]";
                return { kind: "unavailable", vertex_map_key: key, answerIndex: answerIndex, variable: vertex.variable } as VertexUnavailable;
            }
            case "expression": {
                let vertex = structure_vertex.value;
                let key = vertex.repr + "[" + answerIndex + "]";
                return { kind: "expression", vertex_map_key: key, answerIndex: answerIndex, repr: vertex.repr } as VertexExpression;
            }
            case "functionCall": {
                let vertex = structure_vertex.value;
                let key = vertex.repr + "[" + answerIndex + "]";
                return { kind: "functionCall", vertex_map_key: key, answerIndex: answerIndex, repr: vertex.repr } as VertexFunction;
            }
            default: {
                throw new Error("Unsupported vertex type: " + structure_vertex);
            }
        }
    }

    extract_edge_type(structure_edge_type: EdgeType, answerIndex: number, data: ConceptRow): LogicalEdgeType {
        switch (structure_edge_type.kind) {
            case "isa":
            case "has":
            case "sub":
            case "owns":
            case "relates":
            case "plays":
            case "isaExact":
            case "subExact":
            {
                return { kind: structure_edge_type.kind, param: null };
            }
            case "links": {
                let role = this.translate_vertex(structure_edge_type.param as Vertex, answerIndex, data);
                return { kind: structure_edge_type.kind, param: role as RoleType | VertexUnavailable };
            }
            case "assigned":
            case "argument":{
                return { kind: structure_edge_type.kind, param: structure_edge_type.param as string };
            }
            default: {
                console.log("Unsupported EdgeKind:"+ structure_edge_type)
                throw new Error("Unsupported EdgeKind:"+ structure_edge_type.kind);
            }
        }
    }
}
