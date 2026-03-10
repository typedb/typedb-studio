import {
    AnalyzedConjunction, AnalyzedPipeline,
    ConstraintAny, ConstraintExpression, ConstraintVertexVariable,
    ConstraintExpressionLegacy, ConstraintLinksLegacy,
    QueryStructureLegacy, QueryConjunctionLegacy, ConceptRowsQueryResponseLegacy,
    ConceptRowsQueryResponse,
    Attribute, AttributeType, Concept, ConceptRow,
    getVariableName, ConstraintVertexAny,
    Entity, EntityType, InstantiableType,
    Relation, RelationType, RoleType, Type, Value
} from "@typedb/driver-http";
import {
    DataConstraintAny, StructuredAnswer, DataVertex,
    VertexUnavailable, QueryCoordinates,
} from "./structured-answers";

// Back-compat types & helpers

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

// Structured answers builder

export function buildStructuredAnswers(rowsResult: ConceptRowsQueryResponseBackCompat): StructuredAnswer[] {
    return new StructuredAnswersBuilder().build(rowsResult);
}

class StructuredAnswersBuilder {
    constructor() {
    }

    build(rowsResult: ConceptRowsQueryResponseBackCompat): StructuredAnswer[] {
        let answers: StructuredAnswer[] = [];
        rowsResult.answers.forEach((row, answerIndex) => {
            let constraints = row.involvedBlocks!.flatMap(branchIndex => {
                return backCompat_pipelineBlocks(rowsResult.query!)[branchIndex].constraints.map((constraint, constraintIndex) => {
                    return this.toDataConstraint(rowsResult.query!, answerIndex, constraint, row.data, {
                        branch: branchIndex,
                        constraint: constraintIndex
                    });
                }).filter(x => x != null);
            });
            answers.push({ constraints });
        });
        return answers;
    }

    translateVertex(structure: AnalyzedPipelineBackCompat, structureVertex: ConstraintVertexAny, answerIndex: number, data: ConceptRow): DataVertex {
        switch (structureVertex.tag) {
            case "variable": {
                let name = getVariableName(structure, structureVertex);
                if (name != null && data[name] != null ) {
                    return data[name]!;
                } else {
                    let nameOrId = name ?? `$_${structureVertex.id}`;
                    let key = `unavailable[${nameOrId}][${answerIndex}]`;
                    return {
                        kind: "unavailable",
                        vertexMapKey: key,
                        answerIndex: answerIndex,
                        variable: nameOrId,
                    } as VertexUnavailable;
                }
            }
            case "label": {
                let vertex = structureVertex.type;
                return {kind: vertex.kind, label: vertex.label} as Type;
            }
            case "value": {
                return structureVertex.value;
            }
            case "namedRole": {
                return { kind: "roleType", label: structureVertex.name };
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

                    instance: this.translateVertex(structure, constraint.instance, answerIndex, data) as (Entity | Relation | Attribute | VertexUnavailable),
                    type: this.translateVertex(structure, constraint.type, answerIndex, data) as (InstantiableType | VertexUnavailable),
                }
            }
            case "isa!": {
                return {
                    tag: "isa!",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    instance: this.translateVertex(structure, constraint.instance, answerIndex, data) as (Entity | Relation | Attribute | VertexUnavailable),
                    type: this.translateVertex(structure, constraint.type, answerIndex, data) as (InstantiableType | VertexUnavailable),
                }
            }
            case "has": {
                return {
                    tag: "has",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    owner: this.translateVertex(structure, constraint.owner, answerIndex, data) as (Entity | Relation | VertexUnavailable),
                    attribute: this.translateVertex(structure, constraint.attribute, answerIndex, data) as (Attribute | VertexUnavailable),
                }
            }
            case "links": {
                return {
                    tag: "links",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    relation: this.translateVertex(structure, constraint.relation, answerIndex, data) as (Relation | VertexUnavailable),
                    player: this.translateVertex(structure, constraint.player, answerIndex, data) as (Entity | Relation | VertexUnavailable),
                    role: this.translateVertex(structure, constraint.role, answerIndex, data) as (RoleType | VertexUnavailable),
                }
            }
            case "sub": {
                return {
                    tag: "sub",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    subtype: this.translateVertex(structure, constraint.subtype, answerIndex, data) as (Type | VertexUnavailable),
                    supertype: this.translateVertex(structure, constraint.supertype, answerIndex, data) as (Type | VertexUnavailable),
                }
            }
            case "sub!": {
                return {
                    tag: "sub!",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    subtype: this.translateVertex(structure, constraint.subtype, answerIndex, data) as (Type | VertexUnavailable),
                    supertype: this.translateVertex(structure, constraint.supertype, answerIndex, data) as (Type | VertexUnavailable),
                }
            }
            case "owns": {
                return {
                    tag: "owns",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    owner: this.translateVertex(structure, constraint.owner, answerIndex, data) as (EntityType | RelationType | VertexUnavailable),
                    attribute: this.translateVertex(structure, constraint.attribute, answerIndex, data) as (AttributeType | VertexUnavailable),
                }
            }
            case "relates": {
                return {
                    tag: "relates",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    relation: this.translateVertex(structure, constraint.relation, answerIndex, data) as (RelationType | VertexUnavailable),
                    role: this.translateVertex(structure, constraint.role, answerIndex, data) as (RoleType | VertexUnavailable),
                }
            }
            case "plays": {
                return {
                    tag: "plays",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    player: this.translateVertex(structure, constraint.player, answerIndex, data) as (EntityType | RelationType | VertexUnavailable),
                    role: this.translateVertex(structure, constraint.role, answerIndex, data) as (RoleType | VertexUnavailable),
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
                    arguments: constraint.arguments.map(vertex => this.translateVertex(structure, vertex, answerIndex, data) as (Entity | Relation | Attribute | Value | VertexUnavailable)),
                    assigned: this.translateVertex(structure, queryAssigned, answerIndex, data) as (Entity | Relation | Attribute | Value | VertexUnavailable),
                }
            }
            case "functionCall": {
                return {
                    tag: "function",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    name: constraint.name,
                    arguments: constraint.arguments.map(vertex => this.translateVertex(structure, vertex, answerIndex, data) as (Entity | Relation | Attribute | Value | VertexUnavailable)),
                    assigned: constraint.assigned.map(vertex => this.translateVertex(structure, vertex, answerIndex, data) as (Entity | Relation | Attribute | Value | VertexUnavailable)),
                }
            }
            case "comparison" : {
                return  {
                    tag: "comparison",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    lhs: this.translateVertex(structure, constraint.lhs, answerIndex, data) as (Value | Attribute | VertexUnavailable),
                    rhs: this.translateVertex(structure, constraint.lhs, answerIndex, data) as (Value | Attribute | VertexUnavailable),
                    comparator: constraint.comparator,
                }
            }
            case "is" : {
                return {
                    tag: "is",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    lhs: this.translateVertex(structure, constraint.lhs, answerIndex, data) as (Concept | VertexUnavailable),
                    rhs: this.translateVertex(structure, constraint.lhs, answerIndex, data) as (Concept | VertexUnavailable),
                }
            }
            case "iid" : {
                return {
                    tag: "iid",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    concept: this.translateVertex(structure, constraint.concept, answerIndex, data) as (Concept | VertexUnavailable),
                    iid: constraint.iid,
                }
            }
            case "label" : {
                return {
                    tag: "label",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    type: this.translateVertex(structure, constraint.type, answerIndex, data) as (Type | VertexUnavailable),
                    label: constraint.label,
                }
            }
            case "value": {
                return {
                    tag: "value",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    attributeType: this.translateVertex(structure, constraint.attributeType, answerIndex, data) as (AttributeType| VertexUnavailable),
                    valueType: constraint.valueType,
                }
            }
            case "kind" : {
                return {
                    tag: "kind",
                    textSpan: constraint.textSpan,
                    queryCoordinates: coordinates,
                    queryConstraint: constraint,

                    type: this.translateVertex(structure, constraint.type, answerIndex, data) as (Type | VertexUnavailable),
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
