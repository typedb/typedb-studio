export interface TypeQLGraph {
    vertices: TypeQLVertex[];
    edges: TypeQLEdge[];
}

type TypeQLVertexEncoding = "entity" | "relation" | "attribute";
type TypeQLEdgeHighlight = "inferred" | "error";

export interface TypeQLVertex {
    id: number;
    label: string;
    encoding: TypeQLVertexEncoding;
    x: number;
    y: number;
    width: number;
    height: number;
}

export interface TypeQLEdge {
    source: number;
    target: number;
    label: string;
    highlight?: TypeQLEdgeHighlight;
}
