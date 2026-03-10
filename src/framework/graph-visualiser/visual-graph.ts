import { EdgeKind } from "@typedb/driver-http";
import { MultiGraph } from "graphology";
import { DataConstraintAny, DataVertex } from "./data/types";

export interface VertexMetadata {
    defaultLabel: string;
    hoverLabel: string;
    concept: DataVertex;
}

export interface VertexAttributes {
    label: string;
    color: string;
    borderColor: string;
    width: number;
    height: number;
    size: number;  // max(width, height) — kept for sigma internals
    type: string;
    x: number;
    y: number;
    metadata: VertexMetadata;
    highlighted: boolean;
}

export interface EdgeMetadata {
    answerIndex: number;
    dataEdge: DataConstraintAny;
}

export interface EdgeAttributes {
    label: string;
    color: string;
    size: number;
    type: string;
    metadata: EdgeMetadata;
}

export interface GraphAttributes {
}

export type VisualGraph = MultiGraph<VertexAttributes, EdgeAttributes, GraphAttributes>;

export const newVisualGraph: () => VisualGraph = () => new MultiGraph<VertexAttributes, EdgeAttributes, GraphAttributes>();

export interface StudioConverterStructureParameters {
    ignoreEdgesInvolvingLabels: Array<EdgeKind>,
}

export const defaultStructureParameters: StudioConverterStructureParameters = {
    ignoreEdgesInvolvingLabels: ["isa", "sub", "relates", "plays"],
};
