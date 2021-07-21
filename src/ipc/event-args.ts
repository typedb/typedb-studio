import { TypeDBVisualiserData } from "../typedb-visualiser";

export interface IPCResponse {
    success: boolean;
    error?: string;
}

export interface ConnectRequest {
    address: string;
}

export interface ConnectResponse extends IPCResponse {}

export interface LoadDatabasesResponse extends IPCResponse {
    databases?: string[];
}

export interface MatchQueryRequest {
    db: string;
    query: string;
}

export interface ConceptData {
    encoding: TypeDBVisualiserData.VertexEncoding;
    iid?: string;
    type?: string;
    value?: boolean | number | string | Date;
    label?: string;
}

export type ConceptMapData = {[varName: string]: ConceptData};

export interface MatchQueryResponse extends IPCResponse {
    answers?: ConceptMapData[]; // TODO: Implement streaming behaviour
}
