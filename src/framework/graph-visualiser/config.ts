import { EdgeKind, RoleType } from "@typedb/driver-http";
import {DataVertex, DataVertexKind, VertexUnavailable} from "./graph";
import {Color} from "chroma-js";

export interface StudioConverterStyleParameters {
    vertex_colors: Record<DataVertexKind, string>,
    vertex_border_colors: Record<DataVertexKind, string>,
    vertex_shapes: Record<DataVertexKind, string>,
    vertex_sizes: Record<DataVertexKind, number>,
    vertex_size: number,

    // Per-type overrides (keyed by type label, e.g. "person", "employment")
    vertex_type_colors?: Record<string, string>,
    vertex_type_border_colors?: Record<string, string>,
    vertex_type_shapes?: Record<string, string>,
    vertex_type_sizes?: Record<string, number>,

    edge_color: Color,
    edge_highlight_color: Color;
    edge_size: number

    vertex_default_label: (vertex: DataVertex) => string;
    vertex_hover_label: (vertex: DataVertex) => string;
    links_edge_label: (role: RoleType | VertexUnavailable) => string;
}

export interface StudioConverterStructureParameters {
    ignoreEdgesInvolvingLabels: Array<EdgeKind>,
}
