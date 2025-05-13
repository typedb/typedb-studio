/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { EdgeKind, TypeKind, Value } from "./concept";

export type QueryVertexKind = "variable" | "label" | "value" | "unavailableVariable" | "expression" | "functionCall";

export interface QueryVertexVariable {
    kind: "variable";
    value: { variable: string };
}

export interface QueryVertexLabel {
    kind: "label";
    value: { kind: TypeKind, label: string };
}

export interface QueryVertexValue {
    kind: "value";
    value: Value;
}

export interface QueryVertexExpression {
    kind: "expression";
    value: { repr: string };
}

export interface QueryVertexFunction {
    kind: "functionCall";
    value: { repr: string };
}

export interface QueryVertexUnavailable {
    kind: "unavailableVariable";
    value: { variable: string };
}

export type QueryVertex = QueryVertexVariable | QueryVertexLabel | QueryVertexValue | QueryVertexExpression | QueryVertexFunction | QueryVertexUnavailable;
// TODO:
// export enum VertexKindOther = { }

export type QueryEdge = {
    type: QueryEdgeType,
    to: QueryVertex,
    from: QueryVertex,
    span: { begin: number, end: number }
};

export type QueryEdgeType = { kind: EdgeKind, param: QueryVertex | null | string };

export type QueryStructure = { branches: { edges: QueryEdge[] }[] };
