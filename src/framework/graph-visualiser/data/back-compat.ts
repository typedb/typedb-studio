import {
    AnalyzedConjunction, AnalyzedPipeline,
    ConstraintAny, ConstraintExpression, ConstraintVertexVariable,
    ConstraintExpressionLegacy, ConstraintLinksLegacy,
    QueryStructureLegacy, QueryConjunctionLegacy, ConceptRowsQueryResponseLegacy,
    ConceptRowsQueryResponse,
} from "@typedb/driver-http";

export type ConstraintBackCompat = ConstraintAny | ConstraintLinksLegacy | ConstraintExpressionLegacy;
export type ConceptRowsQueryResponseBackCompat = ConceptRowsQueryResponse | ConceptRowsQueryResponseLegacy;
export type AnalyzedPipelineBackCompat = AnalyzedPipeline | QueryStructureLegacy;

export function backCompat_pipelineBlocks(pipeline : AnalyzedPipelineBackCompat): AnalyzedConjunction[] | QueryConjunctionLegacy[] {
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
