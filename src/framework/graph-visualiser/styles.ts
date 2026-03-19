import {
    RoleType,
    ConstraintAny, ConstraintExpression, ConstraintSpan, ConstraintVertexVariable,
    ConstraintExpressionLegacy, ConstraintLinksLegacy,
    AnalyzedPipeline,
} from "@typedb/driver-http";
import { VertexKind, QueryCoordinates, VertexUnavailable } from "@typedb/graph-utils";
import { AnalyzedPipelineBackCompat, backCompat_pipelineBlocks, backCompat_expressionAssigned } from "./types";
import type { StudioDataVertex } from "./types";
import { Color } from "chroma-js";
import chroma from "chroma-js";
import { vertexMapKey, shouldCreateEdge, shouldCreateNode } from "./graph-builder";
import { Graph } from "./graph";

export interface GraphStyles {
    vertexColors: Record<VertexKind, string>,
    vertexBorderColors: Record<VertexKind, string>,
    vertexShapes: Record<VertexKind, string>,
    vertexWidths: Record<VertexKind, number>,
    vertexHeights: Record<VertexKind, number>,
    vertexHeight: number,

    // Per-type overrides (keyed by type label, e.g. "person", "employment")
    vertexTypeColors?: Record<string, string>,
    vertexTypeBorderColors?: Record<string, string>,
    vertexTypeShapes?: Record<string, string>,
    vertexTypeWidths?: Record<string, number>,
    vertexTypeHeights?: Record<string, number>,

    edgeColor: Color,
    edgeLabelColors?: Record<string, string>,
    edgeHighlightColor: Color;
    edgeSize: number

    vertexDefaultLabel: (vertex: StudioDataVertex) => string;
    vertexHoverLabel: (vertex: StudioDataVertex) => string;
    linksEdgeLabel: (role: RoleType | VertexUnavailable) => string;
}

export const darkPalette = {
    black:    "#09022F",
    blue1:    "#7BA0FF",
    green:    "#02DAC9",
    orange:   "#B0740C",
    yellow:   "#F6C94C",
    pink:     "#FF87DC",
    purple1:  "#0E0D17",
    purple2:  "#14121F",
    purple3:  "#151322",
    purple4:  "#1A182A",
    purple5:  "#232135",
    purple6:  "#2D2A46",
    red1:     "#CF4A55",
    red2:     "#FF8080",
    white:    "#FFFFFF",
    white2:   "#D5CCFF"
};

export const defaultEdgeLabelColors: Record<string, string> = {};

export const defaultQueryStyleParams: GraphStyles = {
    vertexColors: {
        entity: "#402237",
        relation: "#3e3213",
        attribute: "#1f2840",
        entityType: "#402237",
        relationType: "#3e3213",
        attributeType: "#1f2840",
        roleType: "#2c1d03",
        value: "#262626",
        unavailable: "#1a1a1a",
    },
    vertexBorderColors: {
        entity: darkPalette.pink,
        relation: darkPalette.yellow,
        attribute: darkPalette.blue1,
        entityType: darkPalette.pink,
        relationType: darkPalette.yellow,
        attributeType: darkPalette.blue1,
        roleType: darkPalette.orange,
        value: "#999",
        unavailable: "#666",
    },
    vertexShapes: {
        entity: "rounded-rect",
        relation: "diamond",
        attribute: "ellipse",
        entityType: "rounded-rect",
        relationType: "diamond",
        attributeType: "ellipse",
        roleType: "ellipse",
        value: "ellipse",
        unavailable: "ellipse",
    },
    vertexWidths: {
        entity: 56,
        relation: 52,
        attribute: 70,
        entityType: 56,
        relationType: 52,
        attributeType: 70,
        roleType: 56,
        value: 56,
        unavailable: 56,
    },
    vertexHeights: {
        entity: 24,
        relation: 26,
        attribute: 40,
        entityType: 24,
        relationType: 26,
        attributeType: 40,
        roleType: 24,
        value: 24,
        unavailable: 24,
    },
    vertexHeight: 24,

    edgeColor: chroma("#5a5670"),
    edgeHighlightColor: chroma("cyan"),
    edgeSize: 2,

    vertexDefaultLabel(vertex: StudioDataVertex): string {
        switch (vertex.kind) {
            case "entityType":
            case "relationType":
            case "roleType":
            case "attributeType": {
                return vertex.label;
            }

            case "entity":
            case "relation":{
                return vertex.type.label;
            }
            case "attribute": {
                return `${vertex.type.label}\n${vertex.value}`;
            }
            case "value": {
                return vertex.value;
            }
            case "unavailable": {
                return `?${vertex.variable}?`;
            }
            case "functionCall": {
                let argStart = vertex.repr.indexOf("(");
                return vertex.repr.substring(0, argStart) + "(...)";
            }
            case "expression": {
                let parts = vertex.repr.split("=");
                return `${parts[0]}=(...)`
            }
        }
    },

    vertexHoverLabel(vertex: StudioDataVertex): string {
        switch (vertex.kind) {
            case "entityType":
            case "relationType":
            case "roleType":
            case "attributeType": {
                return vertex.label;
            }
            case "entity":
            case "relation": {
                return `${vertex.type.label}:${vertex.iid}`;
            }
            case "attribute": {
                return `${vertex.type.label}:${vertex.value}`;
            }
            case "value": {
                return `${vertex.valueType}:${vertex.value}`;
            }
            case "unavailable": {
                return vertexMapKey(vertex);
            }
            case "functionCall":
            case "expression": {
                return vertex.repr;
            }
        }
    },

    linksEdgeLabel(role: RoleType | VertexUnavailable): string {
        return this.vertexDefaultLabel(role);
    }
};

export const defaultExplorationQueryStyleParams: GraphStyles = {
    vertexColors: defaultQueryStyleParams.vertexColors,
    vertexBorderColors: defaultQueryStyleParams.vertexBorderColors,
    vertexShapes: defaultQueryStyleParams.vertexShapes,
    vertexWidths: defaultQueryStyleParams.vertexWidths,
    vertexHeights: defaultQueryStyleParams.vertexHeights,
    vertexHeight: defaultQueryStyleParams.vertexHeight,

    // We only change this one:
    edgeColor: chroma("darkblue"),
    //^We only change this one:

    edgeHighlightColor: defaultQueryStyleParams.edgeHighlightColor,
    edgeSize: defaultQueryStyleParams.edgeSize,

    vertexDefaultLabel: defaultQueryStyleParams.vertexDefaultLabel,
    vertexHoverLabel: defaultQueryStyleParams.vertexHoverLabel,
    linksEdgeLabel: defaultQueryStyleParams.linksEdgeLabel,
};

// Constraint colouring

export function colorEdgesByConstraintIndex(
    graph: Graph,
    styleParams: GraphStyles,
    reset: boolean,
): void {
    graph.edges().forEach(edgeKey => {
        if (reset) {
            const tag = graph.getEdgeAttributes(edgeKey).metadata.dataEdge.tag;
            const color = styleParams.edgeLabelColors?.[tag] ?? styleParams.edgeColor.hex();
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

    backCompat_pipelineBlocks(queryStructure).forEach((branch: { constraints: any[] }, branchIndex: number) => {
        branch.constraints.forEach((constraint: ConstraintAny | ConstraintExpressionLegacy | ConstraintLinksLegacy, constraintIndex: number) => {
            if (shouldColourConstraint(constraint)) {
                let span = "textSpan" in constraint ? constraint["textSpan"] : null;
                if (span != null) {
                    spans.push({span, coordinates: { branch: branchIndex, constraint: constraintIndex}});
                }
            }
        })
    });
    // Add one to end-offset so we're AFTER the last character
    let startsEndsSeparate = spans.flatMap(span => [
        { offset: span.span.begin, coordinatesIfStartElseNull: span.coordinates },
        { offset: span.span.end + 1, coordinatesIfStartElseNull: null }
    ]);
    startsEndsSeparate.sort((a,b) => a.offset - b.offset);
    let seIndex = 0;
    let highlighted = "";
    for(let i= 0; i<queryString.length; i++) {
        while (seIndex < startsEndsSeparate.length && startsEndsSeparate[seIndex].offset == i) {
            let coordinatesOrNullIfEnd = startsEndsSeparate[seIndex].coordinatesIfStartElseNull;
            if (coordinatesOrNullIfEnd == null) {
                highlighted += "</span>"
            } else {
                let color = getColorForConstraintIndex(coordinatesOrNullIfEnd.branch, coordinatesOrNullIfEnd.constraint)
                highlighted += "<span style=\"color: " + color.hex() + "\">";
            }
            seIndex += 1;
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
