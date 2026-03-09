import { RoleType } from "@typedb/driver-http";
import { DataVertex, DataVertexKind, VertexUnavailable } from "../data/types";
import { Color } from "chroma-js";
import chroma from "chroma-js";
import { vertexMapKey } from "../visual/converter";

export interface StudioConverterStyleParameters {
    vertex_colors: Record<DataVertexKind, string>,
    vertex_border_colors: Record<DataVertexKind, string>,
    vertex_shapes: Record<DataVertexKind, string>,
    vertex_widths: Record<DataVertexKind, number>,
    vertex_heights: Record<DataVertexKind, number>,
    vertex_height: number,

    // Per-type overrides (keyed by type label, e.g. "person", "employment")
    vertex_type_colors?: Record<string, string>,
    vertex_type_border_colors?: Record<string, string>,
    vertex_type_shapes?: Record<string, string>,
    vertex_type_widths?: Record<string, number>,
    vertex_type_heights?: Record<string, number>,

    edge_color: Color,
    edge_label_colors?: Record<string, string>,
    edge_highlight_color: Color;
    edge_size: number

    vertex_default_label: (vertex: DataVertex) => string;
    vertex_hover_label: (vertex: DataVertex) => string;
    links_edge_label: (role: RoleType | VertexUnavailable) => string;
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
