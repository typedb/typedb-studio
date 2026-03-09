import {
    ConstraintAny, ConstraintExpression, ConstraintSpan, ConstraintVertexVariable,
    ConstraintExpressionLegacy, ConstraintLinksLegacy,
    AnalyzedPipeline,
} from "@typedb/driver-http";
import chroma from "chroma-js";
import { QueryCoordinates } from "../data/types";
import { AnalyzedPipelineBackCompat, backCompat_pipelineBlocks, backCompat_expressionAssigned } from "../data/back-compat";
import { shouldCreateEdge, shouldCreateNode } from "../visual/converter";
import { VisualGraph } from "../visual/types";
import { StudioConverterStyleParameters } from "./parameters";

export function colorEdgesByConstraintIndex(
    graph: VisualGraph,
    styleParameters: StudioConverterStyleParameters,
    reset: boolean,
): void {
    graph.edges().forEach(edgeKey => {
        if (reset) {
            const tag = graph.getEdgeAttributes(edgeKey).metadata.dataEdge.tag;
            const color = styleParameters.edge_label_colors?.[tag] ?? styleParameters.edge_color.hex();
            graph.setEdgeAttribute(edgeKey, "color", color);
        } else {
            const attributes = graph.getEdgeAttributes(edgeKey);
            const constraintIndex = attributes.metadata.dataEdge.queryCoordinates.constraint;
            const branchIndex = attributes.metadata.dataEdge.queryCoordinates.branch;
            const color = getColorForConstraintIndex(branchIndex, constraintIndex);
            graph.setEdgeAttribute(edgeKey, "color", color.hex());
        }
    });
}

export function colorQuery(
    queryString: string,
    queryStructure: AnalyzedPipelineBackCompat,
): string {
    function shouldColourConstraint(constraint: ConstraintAny | ConstraintExpressionLegacy | ConstraintLinksLegacy): boolean {
        switch (constraint.tag) {
            case "isa": return shouldCreateEdge(queryStructure, constraint, constraint.instance, constraint.type);
            case "isa!": return shouldCreateEdge(queryStructure, constraint, constraint.instance, constraint.type);
            case "has":  return shouldCreateEdge(queryStructure, constraint, constraint.owner, constraint.attribute);
            case "links":
                return shouldCreateEdge(queryStructure, constraint, constraint.relation, constraint.player);
            case "sub":
                return shouldCreateEdge(queryStructure, constraint, constraint.subtype, constraint.supertype);
            case "sub!":
                return shouldCreateEdge(queryStructure, constraint, constraint.subtype, constraint.supertype);
            case "owns":
                return shouldCreateEdge(queryStructure, constraint, constraint.owner, constraint.attribute);
            case "relates":
                return shouldCreateEdge(queryStructure, constraint, constraint.relation, constraint.role);
            case "plays":
                return shouldCreateEdge(queryStructure, constraint, constraint.player, constraint.role);
            case "expression":

                return (
                    constraint.arguments.map(arg => shouldCreateNode(queryStructure, arg)).reduce((a,b) => a || b, false)
                    || shouldCreateNode(queryStructure, backCompat_expressionAssigned(constraint))
                );
            case "functionCall":
                return (
                    constraint.arguments.map(arg => shouldCreateNode(queryStructure, arg)).reduce((a,b) => a || b, false)
                    || constraint.assigned.map(assigned => shouldCreateNode(queryStructure, assigned)).reduce((a,b) => a || b, false)
                );
            case "comparison": return false;
            case "is": return false;
            case "iid": return false;
            case "kind": return false;
            case "label": return false;
            case "value": return false;
            case "or":  return false;
            case "not": return false;
            case "try": return false;
        }
    }
    let spans: { span: ConstraintSpan, coordinates: QueryCoordinates}[] = [];

    backCompat_pipelineBlocks(queryStructure).forEach((branch, branchIndex) => {
        branch.constraints.forEach((constraint, constraintIndex) => {
            if (shouldColourConstraint(constraint)) {
                let span = "textSpan" in constraint ? constraint["textSpan"] : null;
                if (span != null) {
                    spans.push({span, coordinates: { branch: branchIndex, constraint: constraintIndex}});
                }
            }
        })
    });
    // Add one to end-offset so we're AFTER the last character
    let starts_ends_separate = spans.flatMap(span => [
        { offset: span.span.begin, coordinatesIfStartElseNull: span.coordinates },
        { offset: span.span.end + 1, coordinatesIfStartElseNull: null }
]);
    starts_ends_separate.sort((a,b) => a.offset - b.offset);
    let se_index = 0;
    let highlighted = "";
    for(let i= 0; i<queryString.length; i++) {
        while (se_index < starts_ends_separate.length && starts_ends_separate[se_index].offset == i) {
            let coordinatesOrNullIfEnd = starts_ends_separate[se_index].coordinatesIfStartElseNull;
            if (coordinatesOrNullIfEnd == null) {
                highlighted += "</span>"
            } else {
                let color = getColorForConstraintIndex(coordinatesOrNullIfEnd.branch, coordinatesOrNullIfEnd.constraint)
                highlighted += "<span style=\"color: " + color.hex() + "\">";
            }
            se_index += 1;
        }
        highlighted += (queryString[i] == "\n") ? "<br/>": queryString[i];
    }
    return highlighted;
}

export function getColorForConstraintIndex(branchIndex: number, constraintIndex: number): chroma.Color {
    const OFFSET1 = 153;
    const OFFSET2 = 173;
    const OFFSET3 = 199;
    let r = ((branchIndex + 1) * OFFSET3 + (constraintIndex+1) * OFFSET1) % 256;
    let g = ((branchIndex + 1) * OFFSET2 + (constraintIndex+1) * OFFSET2) % 256;
    let b = ((branchIndex + 1) * OFFSET1 + (constraintIndex+1) * OFFSET3) % 256;
    return chroma([r,g,b]);
}
