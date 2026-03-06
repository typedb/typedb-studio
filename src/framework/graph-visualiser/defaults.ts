import { RoleType } from "@typedb/driver-http";
import chroma from "chroma-js";
import { vertexMapKey } from "./converter";
import {DataVertex, VertexUnavailable} from "./graph";
import { createEdgeCurveProgram, createDrawCurvedEdgeLabel, DEFAULT_EDGE_CURVE_PROGRAM_OPTIONS } from "@sigma/edge-curve";
import {ForceLayoutSettings} from "graphology-layout-force";
import { drawStraightEdgeLabel } from "sigma/rendering";
import {Settings as SigmaSettings} from "sigma/settings";
import {StudioConverterStructureParameters, StudioConverterStyleParameters} from "./config";
import { NodeDiamondProgram } from "./node-diamond";
import { NodeRoundedRectangleProgram } from "./node-rounded-rect";
import { NodeEllipseProgram } from "./node-ellipse";
import { zoomScaledFontSize } from "./label-utils";

const darkPalette = {
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

export const defaultEdgeLabelColors: Record<string, string> = {
    "has": darkPalette.blue1,
    "owns": darkPalette.blue1,
    "links": darkPalette.yellow,
    "relates": darkPalette.yellow,
    "plays": darkPalette.yellow,
};

export const defaultQueryStyleParameters: StudioConverterStyleParameters = {
    vertex_colors: {
        entity: "#402237",
        relation: "#3e3213",
        attribute: "#1f2840",
        entityType: "#402237",
        relationType: "#3e3213",
        attributeType: "#1f2840",
        roleType: "#2c1d03",
        value: "#262626",
        unavailable: "#1a1a1a",
        expression: "#353340",
        functionCall: "#353340",
    },
    vertex_border_colors: {
        entity: darkPalette.pink,
        relation: darkPalette.yellow,
        attribute: darkPalette.blue1,
        entityType: darkPalette.pink,
        relationType: darkPalette.yellow,
        attributeType: darkPalette.blue1,
        roleType: darkPalette.orange,
        value: "#999",
        unavailable: "#666",
        expression: darkPalette.white2,
        functionCall: darkPalette.white2,
    },
    vertex_shapes: {
        entity: "rounded-rect",
        relation: "diamond",
        attribute: "ellipse",
        entityType: "rounded-rect",
        relationType: "diamond",
        attributeType: "ellipse",
        roleType: "ellipse",
        value: "ellipse",
        unavailable: "ellipse",
        expression: "ellipse",
        functionCall: "ellipse",
    },
    vertex_widths: {
        entity: 56,
        relation: 52,
        attribute: 70,
        entityType: 56,
        relationType: 52,
        attributeType: 70,
        roleType: 56,
        value: 56,
        unavailable: 56,
        expression: 56,
        functionCall: 56,
    },
    vertex_heights: {
        entity: 24,
        relation: 26,
        attribute: 40,
        entityType: 24,
        relationType: 26,
        attributeType: 40,
        roleType: 24,
        value: 24,
        unavailable: 24,
        expression: 24,
        functionCall: 24,
    },
    vertex_height: 24,

    edge_color: chroma("grey"),
    edge_highlight_color: chroma("cyan"),
    edge_size: 2,

    vertex_default_label(vertex: DataVertex): string {
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

    vertex_hover_label(vertex: DataVertex): string {
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

    links_edge_label(role: RoleType | VertexUnavailable): string {
        return this.vertex_default_label(role);
    }
};

export const defaultExplorationQueryStyleParameters: StudioConverterStyleParameters = {
    vertex_colors: defaultQueryStyleParameters.vertex_colors,
    vertex_border_colors: defaultQueryStyleParameters.vertex_border_colors,
    vertex_shapes: defaultQueryStyleParameters.vertex_shapes,
    vertex_widths: defaultQueryStyleParameters.vertex_widths,
    vertex_heights: defaultQueryStyleParameters.vertex_heights,
    vertex_height: defaultQueryStyleParameters.vertex_height,

    // We only change this one:
    edge_color: chroma("darkblue"),
    //^We only change this one:

    edge_highlight_color: defaultQueryStyleParameters.edge_highlight_color,
    edge_size: defaultQueryStyleParameters.edge_size,

    vertex_default_label: defaultQueryStyleParameters.vertex_default_label,
    vertex_hover_label: defaultQueryStyleParameters.vertex_hover_label,
    links_edge_label: defaultQueryStyleParameters.links_edge_label,
};


export const defaultStructureParameters: StudioConverterStructureParameters = {
    ignoreEdgesInvolvingLabels: ["isa", "sub", "relates", "plays"],
};

function edgeLabelSize(sourceData: any, targetData: any, maxSize: number): number {
    return Math.max(zoomScaledFontSize(sourceData, maxSize), zoomScaledFontSize(targetData, maxSize));
}

function scaledDrawStraightEdgeLabel(context: CanvasRenderingContext2D, edgeData: any, sourceData: any, targetData: any, settings: any): void {
    const scaledSize = edgeLabelSize(sourceData, targetData, settings.edgeLabelSize);
    if (scaledSize < 3) return;
    drawStraightEdgeLabel(context, edgeData, sourceData, targetData, { ...settings, edgeLabelSize: scaledSize });
}

const defaultDrawCurvedLabel = createDrawCurvedEdgeLabel(DEFAULT_EDGE_CURVE_PROGRAM_OPTIONS as any);
const ScaledEdgeCurveProgram = createEdgeCurveProgram({
    drawLabel(context, edgeData, sourceData, targetData, settings) {
        const scaledSize = edgeLabelSize(sourceData, targetData, settings.edgeLabelSize);
        if (scaledSize < 3) return;
        defaultDrawCurvedLabel(
            context, edgeData, sourceData, targetData,
            { ...settings, edgeLabelSize: scaledSize },
        );
    },
});

export const defaultSigmaSettings: Partial<SigmaSettings> = {
    allowInvalidContainer: true,
    labelFont: '"Darkmode", sans-serif',
    itemSizesReference: "positions",
    autoRescale: false,
    zoomToSizeRatioFunction: (x) => x,
    minCameraRatio: 0.1,
    maxCameraRatio: 10,
    labelColor: {
        color: `#958fa8`,
    },
    labelRenderedSizeThreshold: 0,
    labelDensity: Infinity,
    defaultDrawEdgeLabel: scaledDrawStraightEdgeLabel as any,
    renderEdgeLabels: true,
    nodeProgramClasses: {
        "rounded-rect": NodeRoundedRectangleProgram,
        diamond: NodeDiamondProgram,
        ellipse: NodeEllipseProgram,
    },
    edgeProgramClasses: {
        curved: ScaledEdgeCurveProgram,
    },
};

export const defaultForceSupervisorSettings: ForceLayoutSettings = {
    attraction: 0.00005,
    repulsion: 0.5,
    gravity: 0.00000005,
    inertia: 0.2,
    maxMove: 200,
};
