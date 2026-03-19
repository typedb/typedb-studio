import { EdgeKind } from "@typedb/driver-http";
import { MultiGraph } from "graphology";
import { DataConstraintAny } from "@typedb/graph-utils";
import type { StudioDataVertex } from "./types";

export interface VertexMetadata {
    defaultLabel: string;
    hoverLabel: string;
    concept: StudioDataVertex;
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

export type Graph = MultiGraph<VertexAttributes, EdgeAttributes, GraphAttributes>;

export const newGraph: () => Graph = () => new MultiGraph<VertexAttributes, EdgeAttributes, GraphAttributes>();

export interface GraphBuilderStructureParams {
    ignoreEdgesInvolvingLabels: Array<EdgeKind>,
}

export const defaultStructureParams: GraphBuilderStructureParams = {
    ignoreEdgesInvolvingLabels: ["isa", "sub", "relates", "plays"],
};
