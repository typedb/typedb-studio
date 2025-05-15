import chroma from "chroma-js";
import { RoleType } from "../typedb-driver/concept";
import { vertexMapKey } from "./converter";
import {DataVertex, SpecialVertexKind, VertexUnavailable} from "./graph";
import {NodeSquareProgram} from "@sigma/node-square";
import EdgeCurveProgram from "@sigma/edge-curve";
import {ForceLayoutSettings} from "graphology-layout-force";
import {Settings as SigmaSettings} from "sigma/settings";
import {StudioConverterStructureParameters, StudioConverterStyleParameters} from "./config";

export const defaultQueryStyleParameters: StudioConverterStyleParameters = {
    vertex_colors: {
        entity: chroma("pink"),
        relation: chroma("yellow"),
        attribute: chroma("green"),
        entityType: chroma("magenta"),
        relationType: chroma("orange"),
        attributeType: chroma("darkgreen"),
        roleType: chroma("darkorange"),
        value: chroma("grey"),
        unavailable: chroma("darkgrey"),
        expression: chroma("white"),
        functionCall: chroma("white")
    },
    vertex_shapes: {
        entity: "circle",
        relation: "square",
        attribute: "circle",
        entityType: "circle",
        relationType: "square",
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
    zoomToSizeRatioFunction: (x) => x,
    minCameraRatio: 0.1,
    maxCameraRatio: 10,
    labelColor: {
        color: `#958fa8`,
    },
    renderEdgeLabels: true,
    nodeProgramClasses: {
        square: NodeSquareProgram,
    },
    edgeProgramClasses: {
        curved: EdgeCurveProgram,
    },
};

export const defaultForceSupervisorSettings: ForceLayoutSettings = {
    attraction: 0.00005,
    repulsion: 0.5,
    gravity: 0.00000005,
    inertia: 0.2,
    maxMove: 200,
};
