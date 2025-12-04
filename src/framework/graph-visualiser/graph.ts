import {
    Attribute, AttributeType, Concept, ConceptRow, ConceptRowsQueryResponse, Entity, EntityType, InstantiableType,
    getVariableName, ConstraintComparison, ConstraintExpression, ConstraintFunction,
    ConstraintHas, ConstraintIid, ConstraintIs, ConstraintIsa, ConstraintIsaExact, ConstraintKind,
    ConstraintLabel, ConstraintLinks, ConstraintOwns, ConstraintPlays, ConstraintRelates,
    ConstraintSpan, ConstraintSub, ConstraintSubExact, ConstraintValue, ConstraintVertexAny,
    ConstraintExpressionLegacy, ConstraintLinksLegacy,
    Relation, RelationType, RoleType, ThingKind, Type, TypeKind, Value, ValueKind
} from "@typedb/driver-http";
import {MultiGraph} from "graphology";
import {
    ConstraintBackCompat,
    ConceptRowsQueryResponseBackCompat,
    backCompat_expressionAssigned,
    backCompat_pipelineBlocks,
    AnalyzedPipelineBackCompat
} from "./index";

///////////////////////
// TypeDB Data Graph //
///////////////////////

export type SpecialVertexKind = "unavailable" | "expression" | "functionCall";

export type VertexUnavailable = { kind: "unavailable", variable: string, answerIndex: number, vertex_map_key: string };
export type VertexExpression = { tag: "expression", kind: "expression", repr: string, answerIndex: number, vertex_map_key: string };
export type VertexFunction = { tag: "functionCall", kind: "functionCall", repr: string, answerIndex: number, vertex_map_key: string };
export type DataVertexSpecial = VertexUnavailable | VertexFunction | VertexExpression;

export type DataVertexKind = ThingKind | TypeKind | ValueKind | SpecialVertexKind;
export type DataVertex = Concept | DataVertexSpecial;

export type QueryCoordinates = { branch: number, constraint: number };

export type DataGraph = {
    answers: DataConstraintAny[][];
}

export type DataConstraintAny = DataConstraintIsa | DataConstraintIsaExact | DataConstraintHas | DataConstraintLinks |
    DataConstraintSub | DataConstraintSubExact | DataConstraintOwns | DataConstraintRelates | DataConstraintPlays |
    DataConstraintExpression | DataConstraintFunction | DataConstraintComparison |
    DataConstraintIs | DataConstraintIid | DataConstraintLabel | DataConstraintValue | DataConstraintKind;

export type DataConstraintSpan = ConstraintSpan;

// Instance
export interface DataConstraintIsa {
    tag: "isa",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintIsa,

    instance: Entity | Relation | Attribute | VertexUnavailable,
    type: InstantiableType | VertexUnavailable,
}

export interface DataConstraintIsaExact {
    tag: "isa!",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintIsaExact,

    instance: Entity | Relation | Attribute | VertexUnavailable,
    type: InstantiableType | VertexUnavailable,
}

export interface DataConstraintHas {
    tag: "has",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintHas,

    owner: Entity | Relation | VertexUnavailable,
    attribute: Attribute | VertexUnavailable,
}


export interface DataConstraintLinks {
    tag: "links",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintLinks | ConstraintLinksLegacy,

    relation: Relation | VertexUnavailable,
    player: Relation | Entity | VertexUnavailable,
    role: RoleType | VertexUnavailable,
}

// Type
export interface DataConstraintSub {
    tag: "sub",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintSub,

    subtype: Type | VertexUnavailable,
    supertype: Type | VertexUnavailable,
}

export interface DataConstraintSubExact {
    tag: "sub!",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintSubExact,

    subtype: Type | VertexUnavailable,
    supertype: Type | VertexUnavailable,
}

export interface DataConstraintOwns {
    tag: "owns",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintOwns,

    owner: EntityType | RelationType | VertexUnavailable,
    attribute: AttributeType | VertexUnavailable,
}

export interface DataConstraintRelates {
    tag: "relates",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintRelates,

    relation: RelationType | VertexUnavailable,
    role: RoleType | VertexUnavailable,
}

export interface DataConstraintPlays {
    tag: "plays",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintPlays,

    player: EntityType | RelationType | VertexUnavailable,
    role: RoleType | VertexUnavailable,
}

// Function
export interface DataConstraintExpression {
    tag: "expression",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintExpression | ConstraintExpressionLegacy,

    text: string,
    arguments: (Entity | Relation | Attribute | Value | VertexUnavailable)[],
    assigned: (Entity | Relation | Attribute | Value | VertexUnavailable),
}

export interface DataConstraintFunction {
    tag: "function",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintFunction,

    name: string,
    arguments: (Entity | Relation | Attribute | Value | VertexUnavailable)[],
    assigned: (Entity | Relation | Attribute | Value | VertexUnavailable)[],
}

export interface DataConstraintComparison {
    tag: "comparison",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintComparison,

    lhs: Value | Attribute | VertexUnavailable,
    rhs: Value | Attribute | VertexUnavailable,
    comparator: string,
}

export interface DataConstraintIs {
    tag: "is",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintIs,

    lhs: Concept | VertexUnavailable,
    rhs: Concept | VertexUnavailable,
}

export interface DataConstraintIid {
    tag: "iid",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintIid,

    concept: Concept | VertexUnavailable,
    iid: string,
}

export interface DataConstraintLabel {
    tag: "label",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintLabel,

    type: Type | VertexUnavailable,
    label: string,
}

export interface DataConstraintValue {
    tag: "value",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintValue,

    attributeType: AttributeType | VertexUnavailable,
    valueType: string,
}

export interface DataConstraintKind {
    tag: "kind",
    textSpan: DataConstraintSpan,
    queryCoordinates: QueryCoordinates,
    queryConstraint: ConstraintKind,

    kind: string,
    type: Type | VertexUnavailable,

}

export interface VertexMetadata {
    defaultLabel: string;
    hoverLabel: string;
    concept: DataVertex;
}

export interface VertexAttributes {
    label: string;
    color: string;
    size: number;
    type: string;
    x: number;
    y: number;
    metadata: VertexMetadata;
    highlighted: boolean;
}

export interface EdgeMetadata {
    answerIndex: number;
    dataEdge: DataConstraintAny;
}

export interface EdgeAttributes {
    label: string;
    color: string;
    size: number;
    type: string;
    metadata: EdgeMetadata;
}

export interface GraphAttributes {
}

export type VisualGraph = MultiGraph<VertexAttributes, EdgeAttributes, GraphAttributes>;

export const newVisualGraph: () => VisualGraph = () => new MultiGraph<VertexAttributes, EdgeAttributes, GraphAttributes>();

///////////////////////////////////
// TypeDB server -> logical graph
///////////////////////////////////
export function constructGraphFromRowsResult(rows_result: ConceptRowsQueryResponseBackCompat): DataGraph {
    return new LogicalGraphBuilder().build(rows_result);
}

class LogicalGraphBuilder {
    constructor() {
    }

    build(rows_result: ConceptRowsQueryResponseBackCompat): DataGraph {
        let answers: DataConstraintAny[][] = [];
        rows_result.answers.forEach((row, answerIndex) => {
            let current_answer_edges = row.involvedBlocks!.flatMap(branchIndex => {
                return backCompat_pipelineBlocks(rows_result.query!)[branchIndex].constraints.map((constraint, constraintIndex) => {
                    return this.toDataConstraint(rows_result.query!, answerIndex, constraint, row.data, {
                        branch: branchIndex,
                        constraint: constraintIndex
                    });
                }).filter(x => x != null);
            });
            answers.push(current_answer_edges);
        });
        return {answers: answers};
    }

    translate_vertex(structure: AnalyzedPipelineBackCompat, structure_vertex: ConstraintVertexAny, answerIndex: number, data: ConceptRow): DataVertex {
        switch (structure_vertex.tag) {
            case "variable": {
                let name = getVariableName(structure, structure_vertex);
                if (name != null && data[name] != null ) {
                    return data[name]!;
                } else {
                    let nameOrId = name ?? `$_${structure_vertex.id}`;
                    let key = `unavailable[${nameOrId}][${answerIndex}]`;
                    return {
                        kind: "unavailable",
                        vertex_map_key: key,
                        answerIndex: answerIndex,
                        variable: nameOrId,
                    } as VertexUnavailable;
                }
            }
            case "label": {
                let vertex = structure_vertex.type;
                return {kind: vertex.kind, label: vertex.label} as Type;
            }
            case "value": {
                return structure_vertex.value;
            }
            case "namedRole": {
                return { kind: "roleType", label: structure_vertex.name };
            }
        }
    }

    private toDataConstraint(structure: AnalyzedPipelineBackCompat, answerIndex: number, constraint: ConstraintBackCompat, data: ConceptRow, coordinates: QueryCoordinates): DataConstraintAny | null{
        switch (constraint.tag) {
            case "isa": {
                return {
                    tag: "isa",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    instance: this.translate_vertex(structure, constraint.instance, answerIndex, data) as (Entity | Relation | Attribute | VertexUnavailable),
                    type: this.translate_vertex(structure, constraint.type, answerIndex, data) as (InstantiableType | VertexUnavailable),
                }
            }
            case "isa!": {
                return {
                    tag: "isa!",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    instance: this.translate_vertex(structure, constraint.instance, answerIndex, data) as (Entity | Relation | Attribute | VertexUnavailable),
                    type: this.translate_vertex(structure, constraint.type, answerIndex, data) as (InstantiableType | VertexUnavailable),
                }
            }
            case "has": {
                return {
                    tag: "has",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    owner: this.translate_vertex(structure, constraint.owner, answerIndex, data) as (Entity | Relation | VertexUnavailable),
                    attribute: this.translate_vertex(structure, constraint.attribute, answerIndex, data) as (Attribute | VertexUnavailable),
                }
            }
            case "links": {
                return {
                    tag: "links",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    relation: this.translate_vertex(structure, constraint.relation, answerIndex, data) as (Relation | VertexUnavailable),
                    player: this.translate_vertex(structure, constraint.player, answerIndex, data) as (Entity | Relation | VertexUnavailable),
                    role: this.translate_vertex(structure, constraint.role, answerIndex, data) as (RoleType | VertexUnavailable),
                }
            }
            case "sub": {
                return {
                    tag: "sub",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    subtype: this.translate_vertex(structure, constraint.subtype, answerIndex, data) as (Type | VertexUnavailable),
                    supertype: this.translate_vertex(structure, constraint.supertype, answerIndex, data) as (Type | VertexUnavailable),
                }
            }
            case "sub!": {
                return {
                    tag: "sub!",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    subtype: this.translate_vertex(structure, constraint.subtype, answerIndex, data) as (Type | VertexUnavailable),
                    supertype: this.translate_vertex(structure, constraint.supertype, answerIndex, data) as (Type | VertexUnavailable),
                }
            }
            case "owns": {
                return {
                    tag: "owns",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    owner: this.translate_vertex(structure, constraint.owner, answerIndex, data) as (EntityType | RelationType | VertexUnavailable),
                    attribute: this.translate_vertex(structure, constraint.attribute, answerIndex, data) as (AttributeType | VertexUnavailable),
                }
            }
            case "relates": {
                return {
                    tag: "relates",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    relation: this.translate_vertex(structure, constraint.relation, answerIndex, data) as (RelationType | VertexUnavailable),
                    role: this.translate_vertex(structure, constraint.role, answerIndex, data) as (RoleType | VertexUnavailable),
                }
            }
            case "plays": {
                return {
                    tag: "plays",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    player: this.translate_vertex(structure, constraint.player, answerIndex, data) as (EntityType | RelationType | VertexUnavailable),
                    role: this.translate_vertex(structure, constraint.role, answerIndex, data) as (RoleType | VertexUnavailable),
                }
            }
            case "expression": {
                const queryAssigned = backCompat_expressionAssigned(constraint);
                return {
                    tag: "expression",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    text: constraint.text,
                    arguments: constraint.arguments.map(vertex => this.translate_vertex(structure, vertex, answerIndex, data) as (Entity | Relation | Attribute | Value | VertexUnavailable)),
                    assigned: this.translate_vertex(structure, queryAssigned, answerIndex, data) as (Entity | Relation | Attribute | Value | VertexUnavailable),
                }
            }
            case "functionCall": {
                return {
                    tag: "function",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    name: constraint.name,
                    arguments: constraint.arguments.map(vertex => this.translate_vertex(structure, vertex, answerIndex, data) as (Entity | Relation | Attribute | Value | VertexUnavailable)),
                    assigned: constraint.assigned.map(vertex => this.translate_vertex(structure, vertex, answerIndex, data) as (Entity | Relation | Attribute | Value | VertexUnavailable)),
                }
            }
            case "comparison" : {
                return  {
                    tag: "comparison",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    lhs: this.translate_vertex(structure, constraint.lhs, answerIndex, data) as (Value | Attribute | VertexUnavailable),
                    rhs: this.translate_vertex(structure, constraint.lhs, answerIndex, data) as (Value | Attribute | VertexUnavailable),
                    comparator: constraint.comparator,
                }
            }
            case "is" : {
                return {
                    tag: "is",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    lhs: this.translate_vertex(structure, constraint.lhs, answerIndex, data) as (Concept | VertexUnavailable),
                    rhs: this.translate_vertex(structure, constraint.lhs, answerIndex, data) as (Concept | VertexUnavailable),
                }
            }
            case "iid" : {
                return {
                    tag: "iid",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    concept: this.translate_vertex(structure, constraint.concept, answerIndex, data) as (Concept | VertexUnavailable),
                    iid: constraint.iid,
                }
            }
            case "label" : {
                return {
                    tag: "label",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    type: this.translate_vertex(structure, constraint.type, answerIndex, data) as (Type | VertexUnavailable),
                    label: constraint.label,
                }
            }
            case "value": {
                return {
                    tag: "value",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    attributeType: this.translate_vertex(structure, constraint.attributeType, answerIndex, data) as (AttributeType| VertexUnavailable),
                    valueType: constraint.valueType,
                }
            }
            case "kind" : {
                return {
                    tag: "kind",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    type: this.translate_vertex(structure, constraint.type, answerIndex, data) as (Type | VertexUnavailable),
                    kind: constraint.kind,
                }
            }
            case "or":
            case "not":
            case "try": {
                return null;
            }
        }
    }
}
