import { RoleType } from "@typedb/driver-http";
import chroma from "chroma-js";
import { vertexMapKey } from "./converter";
import {DataVertex, VertexUnavailable} from "./graph";
import {NodeSquareProgram} from "@sigma/node-square";
import EdgeCurveProgram from "@sigma/edge-curve";
import {ForceLayoutSettings} from "graphology-layout-force";
import {Settings as SigmaSettings} from "sigma/settings";
import {StudioConverterStructureParameters, StudioConverterStyleParameters} from "./config";
import { NodeDiamondProgram } from "./node-diamond";

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

export const defaultQueryStyleParameters: StudioConverterStyleParameters = {
    vertex_colors: {
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
        entity: "square",
        relation: "diamond",
        attribute: "circle",
        entityType: "square",
        relationType: "diamond",
        attributeType: "circle",
        roleType: "circle",
        value: "circle",
        unavailable: "circle",
        expression: "circle",
        functionCall: "circle",
    },
    vertex_size: 6,

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
                return vertex.value;
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
    vertex_shapes: defaultQueryStyleParameters.vertex_shapes,
    vertex_size: defaultQueryStyleParameters.vertex_size,

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

export const defaultSigmaSettings: Partial<SigmaSettings> = {
    allowInvalidContainer: true,
    zoomToSizeRatioFunction: (x) => x,
    minCameraRatio: 0.1,
    maxCameraRatio: 10,
    labelColor: {
        color: `#958fa8`,
    },
    renderEdgeLabels: true,
    nodeProgramClasses: {
        square: NodeSquareProgram,
        diamond: NodeDiamondProgram,
    },
    edgeProgramClasses: {
        curved: EdgeCurveProgram,
    },
    cameraPanBoundaries: {
        tolerance: 1,
    },
};

export const defaultForceSupervisorSettings: ForceLayoutSettings = {
    attraction: 0.00005,
    repulsion: 0.5,
    gravity: 0.00000005,
    inertia: 0.2,
    maxMove: 200,
};
