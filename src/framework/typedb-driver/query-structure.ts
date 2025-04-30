/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { TypeKind, Value } from "./concept";

export type EdgeKind = "isa" | "has" | "links" | "sub" | "owns" | "relates" | "plays" | "isaExact" | "subExact" | "assigned" | "argument";

export type QueryStructure = { branches: Array<{ edges: Array<Edge> }> };

export type Edge = {
    type: EdgeType,
    to: Vertex,
    from: Vertex,
    span: { begin: number, end: number }
};
export type EdgeType = { kind: EdgeKind, param: Vertex | null | string };

export type VertexKind = "variable" | "label" | "value" | "unavailableVariable" | "expression" | "functionCall";

export interface VertexVariable {
    kind: "variable";
    value: { variable: string };
}

export interface VertexLabel {
    kind: "label";
    value: { kind: TypeKind, label: string };
}

export interface VertexValue {
    kind: "value";
    value: Value;
}

export interface VertexExpression {
    kind: "expression";
    value: { repr: string };
}

export interface VertexFunction {
    kind: "functionCall";
    value: { repr: string };
}

export interface VertexUnavailable {
    kind: "unavailableVariable";
    value: { variable: string };
}

export type Vertex = VertexVariable | VertexLabel | VertexValue | VertexExpression | VertexFunction | VertexUnavailable;
// TODO:
// export enum VertexKindOther = { }

