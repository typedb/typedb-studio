/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import {Type, Value} from "./concept";

export type QueryVertexKind = "variable" | "label" | "value";

export interface QueryVertexVariable {
    tag: "variable";
    id: string,
}

export interface QueryVertexLabel {
    tag: "label";
    type: Type;
}

export interface QueryVertexValue {
    tag: "value";
    value: Value;
}

export type QueryVertex = QueryVertexVariable | QueryVertexLabel | QueryVertexValue;
// TODO:
// export enum VertexKindOther = { }
export type QueryStructure = {
    blocks: { constraints: QueryConstraintAny[] }[],
    variables: {[name: string]: QueryVariableInfo },
    outputs: string[],
};

export function get_variable_name(structure: QueryStructure, variable: QueryVertexVariable) : string | null {
    return structure.variables[variable.id]?.name;
}

export type QueryVariableInfo = { name: string | null };

export type QueryConstraintAny = QueryConstraintIsa | QueryConstraintIsaExact | QueryConstraintHas | QueryConstraintLinks |
    QueryConstraintSub | QueryConstraintSubExact | QueryConstraintOwns | QueryConstraintRelates | QueryConstraintPlays |
    QueryConstraintExpression | QueryConstraintFunction | QueryConstraintComparison |
    QueryConstraintIs | QueryConstraintIid | QueryConstraintKind | QueryConstraintLabel;

export type QueryConstraintSpan = { begin: number, end: number };

// Instance
export interface QueryConstraintIsa {
    tag: "isa",
    textSpan: QueryConstraintSpan,

    instance: QueryVertexVariable,
    type: QueryVertexVariable | QueryVertexLabel,
}

export interface QueryConstraintIsaExact {
    tag: "isa!",
    textSpan: QueryConstraintSpan,

    instance: QueryVertexVariable,
    type: QueryVertexVariable | QueryVertexLabel,
}

export interface QueryConstraintHas {
    tag: "has",
    textSpan: QueryConstraintSpan,

    owner: QueryVertexVariable
    attribute: QueryVertexVariable,
}


export interface QueryConstraintLinks {
    tag: "links",
    textSpan: QueryConstraintSpan,

    relation: QueryVertexVariable,
    player: QueryVertexVariable,
    role: QueryVertexVariable | QueryVertexLabel,
}

// Type
export interface QueryConstraintSub {
    tag: "sub",
    textSpan: QueryConstraintSpan,

    subtype: QueryVertexVariable | QueryVertexLabel,
    supertype: QueryVertexVariable | QueryVertexLabel,
}

export interface QueryConstraintSubExact {
    tag: "sub!",
    textSpan: QueryConstraintSpan,

    subtype: QueryVertexVariable | QueryVertexLabel,
    supertype: QueryVertexVariable | QueryVertexLabel,
}

export interface QueryConstraintOwns {
    tag: "owns",
    textSpan: QueryConstraintSpan,

    owner: QueryVertexVariable | QueryVertexLabel,
    attribute: QueryVertexVariable | QueryVertexLabel,
}

export interface QueryConstraintRelates {
    tag: "relates",
    textSpan: QueryConstraintSpan,

    relation: QueryVertexVariable | QueryVertexLabel,
    role: QueryVertexVariable | QueryVertexLabel,
}

export interface QueryConstraintPlays {
    tag: "plays",
    textSpan: QueryConstraintSpan,

    player: QueryVertexVariable | QueryVertexLabel,
    role: QueryVertexVariable | QueryVertexLabel,
}

// Function
export interface QueryConstraintExpression {
    tag: "expression",
    textSpan: QueryConstraintSpan,

    text: string,
    arguments: QueryVertexVariable[],
    assigned: QueryVertexVariable[],
}

export interface QueryConstraintFunction {
    tag: "functionCall",
    textSpan: QueryConstraintSpan,

    name: string,
    arguments: QueryVertexVariable[],
    assigned: QueryVertexVariable[],
}

export interface QueryConstraintComparison {
    tag: "comparison",
    textSpan: QueryConstraintSpan,

    lhs: QueryVertexVariable | QueryVertexValue,
    rhs: QueryVertexVariable | QueryVertexValue,
    comparator: string,
}

export interface QueryConstraintIs {
    tag: "is",
    textSpan: QueryConstraintSpan,

    lhs: QueryVertexVariable,
    rhs: QueryVertexVariable,
}

export interface QueryConstraintIid {
    tag: "iid",
    textSpan: QueryConstraintSpan,

    concept: QueryVertexVariable,
    iid: string,
}

export interface QueryConstraintLabel {
    tag: "label",
    textSpan: QueryConstraintSpan,

    type: QueryVertexVariable,
    label: string,
}

export interface QueryConstraintKind {
    tag: "kind",
    textSpan: QueryConstraintSpan,

    type: QueryVertexVariable,
    kind: string,
}
