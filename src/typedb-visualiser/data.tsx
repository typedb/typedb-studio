export declare namespace TypeDBVisualiserData {
    export interface Graph {
        simulationID?: string;
        vertices: Vertex[];
        edges: Edge[];
    }

    export interface Vertex {
        id: number;
        label: string;
        encoding: VertexEncoding;
        x?: number;
        y?: number;
        width: number;
        height: number;
        color?: number;
    }

    export type VertexEncoding = "entityType" | "relationType" | "attributeType" | "thingType" | "entity" | "relation" | "attribute";

    export interface Edge {
        id?: number;
        source: number;
        target: number;
        label: string;
        highlight?: EdgeHighlight;
        color?: number;
    }

    export type EdgeHighlight = "inferred" | "error";
}
