import {ConceptAny, TypeDBValue, EdgeKind, ThingKind, TypeKind} from "./concept"
// For common data-structures that convert between what the server gives us and what we need

///////////
// QUERY //
///////////

export enum TypeDBQueryType {
    schema = "schema",
    write = "write",
    read = "read",
}

// Answers:
export type TypeDBRowData = { [id: string] : ConceptAny };


/////////////////////
// Query structure //
/////////////////////
export type TypeDBQueryStructure = { branches: Array<{ edges: Array<StructureEdge> }> };

export type StructureEdge = {
    type: StructureEdgTypeAny,
    to: StructureVertex,
    from: StructureVertex,
    span: { begin: number, end: number }
};
export type StructureEdgTypeAny = { kind: EdgeKind, param: StructureVertex | null | string };

// Vertex
export type StructureVertex = {
    kind: StructureVertexKind,
    value: StructureVertexAny,
}
export type StructureVertexVariable = { variable: string };
export type StructureVertexLabel = {  kind: TypeKind, label: string };
export type StructureVertexValue = TypeDBValue;
export type StructureVertexExpression = { repr: string };
export type StructureVertexFunction = { repr: string };
export type StructureVertexUnavailable = {  variable: string };
export type StructureVertexAny = StructureVertexVariable | StructureVertexLabel | StructureVertexValue | StructureVertexExpression | StructureVertexFunction | StructureVertexUnavailable;
// TODO:
// export enum VertexKindOther = { }

export enum StructureVertexKind {
    variable = "variable",
    label = "label",
    value = "value",
    unavailable = "unavailableVariable",
    expr = "expression",
    func = "functionCall",
}
