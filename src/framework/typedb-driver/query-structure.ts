/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import {EdgeKind, Type, TypeKind, Value} from "./concept";

export type QueryVertexKind = "variable" | "label" | "value" | "unavailableVariable" | "expression" | "functionCall";

export interface QueryVertexVariable {
    tag: "variable";
    variable: string,
}

export interface QueryVertexLabel {
    tag: "label";
    type: Type;
}

export interface QueryVertexValue {
    tag: "value";
    value: Value;
}

export interface QueryVertexUnavailable {
    tag: "unavailableVariable";
    variable: string,
}

export type QueryVertex = QueryVertexVariable | QueryVertexLabel | QueryVertexValue | QueryVertexUnavailable;
// TODO:
// export enum VertexKindOther = { }
export type QueryStructure = { blocks: { constraints: QueryConstraintAny[] }[] };

export type QueryConstraintAny = QueryConstraintIsa | QueryConstraintIsaExact | QueryConstraintHas | QueryConstraintLinks |
    QueryConstraintSub | QueryConstraintSubExact | QueryConstraintOwns | QueryConstraintRelates | QueryConstraintPlays |
    QueryConstraintExpression | QueryConstraintFunction;

export type QueryConstraintSpan = { begin: number, end: number };

// Instance
export interface QueryConstraintIsa {
    tag: "isa",
    textSpan: QueryConstraintSpan,

    instance: QueryVertexVariable | QueryVertexUnavailable,
    type: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
}

export interface QueryConstraintIsaExact {
    tag: "isa!",
    textSpan: QueryConstraintSpan,

    instance: QueryVertexVariable | QueryVertexUnavailable,
    type: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
}

export interface QueryConstraintHas {
    tag: "has",
    textSpan: QueryConstraintSpan,

    owner: QueryVertexVariable | QueryVertexUnavailable
    attribute: QueryVertexVariable | QueryVertexUnavailable,
}


export interface QueryConstraintLinks {
    tag: "links",
    textSpan: QueryConstraintSpan,

    relation: QueryVertexVariable | QueryVertexUnavailable,
    player: QueryVertexVariable | QueryVertexUnavailable,
    role: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
}

// Type
export interface QueryConstraintSub {
    tag: "sub",
    textSpan: QueryConstraintSpan,

    subtype: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
    supertype: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
}

export interface QueryConstraintSubExact {
    tag: "sub!",
    textSpan: QueryConstraintSpan,

    subtype: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
    supertype: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
}

export interface QueryConstraintOwns {
    tag: "owns",
    textSpan: QueryConstraintSpan,

    owner: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
    attribute: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
}

export interface QueryConstraintRelates {
    tag: "relates",
    textSpan: QueryConstraintSpan,

    relation: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
    role: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
}

export interface QueryConstraintPlays {
    tag: "plays",
    textSpan: QueryConstraintSpan,

    player: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
    role: QueryVertexVariable | QueryVertexLabel | QueryVertexUnavailable,
}

// Function
export interface QueryConstraintExpression {
    tag: "expression",
    textSpan: QueryConstraintSpan,

    text: string,
    arguments: (QueryVertexVariable | QueryVertexUnavailable)[],
    assigned: (QueryVertexVariable | QueryVertexUnavailable)[],
}

export interface QueryConstraintFunction {
    tag: "functionCall",
    textSpan: QueryConstraintSpan,

    name: string,
    arguments: (QueryVertexVariable | QueryVertexUnavailable)[],
    assigned: (QueryVertexVariable | QueryVertexUnavailable)[],
}
