import {
    Attribute, AttributeType, Concept, ConceptRow, ConceptRowsQueryResponse,
    ConstraintComparison, ConstraintExpression, ConstraintFunction,
    ConstraintHas, ConstraintIid, ConstraintIs, ConstraintIsa, ConstraintIsaExact, ConstraintKind,
    ConstraintLabel, ConstraintLinks, ConstraintOwns, ConstraintPlays, ConstraintRelates,
    ConstraintSpan, ConstraintSub, ConstraintSubExact, ConstraintValue, ConstraintVertexAny,
    ConstraintExpressionLegacy, ConstraintLinksLegacy,
    Entity, EntityType, InstantiableType,
    Relation, RelationType, RoleType, ThingKind, Type, TypeKind, Value, ValueKind
} from "@typedb/driver-http";

///////////////////////
// TypeDB Data Graph //
///////////////////////

export type SpecialVertexKind = "unavailable" | "expression" | "functionCall";

export type VertexUnavailable = { kind: "unavailable", variable: string, answerIndex: number, vertexMapKey: string };
export type VertexExpression = { tag: "expression", kind: "expression", repr: string, answerIndex: number, vertexMapKey: string };
export type VertexFunction = { tag: "functionCall", kind: "functionCall", repr: string, answerIndex: number, vertexMapKey: string };
export type VertexSpecial = VertexUnavailable | VertexFunction | VertexExpression;

export type VertexKind = ThingKind | TypeKind | ValueKind | SpecialVertexKind;
export type DataVertex = Concept | VertexSpecial;

export function getTypeLabel(vertex: DataVertex): string | undefined {
    if ("type" in vertex && vertex.type && "label" in vertex.type) return vertex.type.label;
    if ("label" in vertex) return vertex.label;
    return undefined;
}

export type QueryCoordinates = { branch: number, constraint: number };

export type StructuredAnswer = {
    constraints: DataConstraintAny[];
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
