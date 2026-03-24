/**
 * Back-compat types & helpers.
 * These were previously defined in @typedb/graph-utils but removed.
 * They unify the new AnalyzedPipeline format with the old QueryStructureLegacy format.
 *
 * Also contains Studio-specific vertex types for expressions and functions,
 * which were removed from VertexKind in @typedb/graph-utils.
 */
import {
    ConstraintAny,
    ConstraintLinksLegacy,
    ConstraintExpressionLegacy,
    ConstraintExpression,
    ConstraintVertexVariable,
    ConceptRowsQueryResponse,
    ConceptRowsQueryResponseLegacy,
    AnalyzedPipeline,
    AnalyzedConjunction,
    QueryStructureLegacy,
    QueryConjunctionLegacy,
} from "@typedb/driver-http";
import { DataVertex } from "@typedb/graph-utils";

// -- Back-compat types --

export type ConstraintBackCompat = ConstraintAny | ConstraintLinksLegacy | ConstraintExpressionLegacy;
export type ConceptRowsQueryResponseBackCompat = ConceptRowsQueryResponse | ConceptRowsQueryResponseLegacy;
export type AnalyzedPipelineBackCompat = AnalyzedPipeline | QueryStructureLegacy;

export function backCompat_pipelineBlocks(pipeline: AnalyzedPipelineBackCompat): AnalyzedConjunction[] | QueryConjunctionLegacy[] {
    if ("blocks" in pipeline) {
        return pipeline["blocks"];
    } else if ("conjunctions" in pipeline) {
        return pipeline["conjunctions"];
    } else {
        throw new Error("Unreachable: pipeline neither had blocks nor conjunctions");
    }
}

export function backCompat_expressionAssigned(expr: ConstraintExpression | ConstraintExpressionLegacy): ConstraintVertexVariable {
    return (Array.isArray(expr.assigned) ? expr.assigned[0] : expr.assigned) as ConstraintVertexVariable;
}

// -- Studio-specific vertex types --

export interface VertexExpression {
    tag: "expression";
    kind: "expression";
    answerIndex: number;
    repr: string;
    vertexMapKey: string;
}

export interface VertexFunction {
    tag: "functionCall";
    kind: "functionCall";
    answerIndex: number;
    repr: string;
    vertexMapKey: string;
}

/** DataVertex extended with Studio-specific vertex types for expressions and functions. */
export type StudioDataVertex = DataVertex | VertexExpression | VertexFunction;
