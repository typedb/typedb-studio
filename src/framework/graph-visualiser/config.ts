import { EdgeKind, RoleType } from "../typedb-driver/concept";
import {DataVertex, DataVertexKind, VertexUnavailable} from "./graph";
import {Color} from "chroma-js";

export interface StudioConverterStyleParameters {
    vertex_colors: Record<DataVertexKind, string>,
    vertex_shapes: Record<DataVertexKind, string>,
    vertex_size: number,

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
