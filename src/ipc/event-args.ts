import { TypeDBVisualiserData } from "../typedb-visualiser";

export interface IPCResponse {
    success: boolean;
    error?: string;
}

export interface ConnectRequest {
    address: string;
}

export interface LoadDatabasesResponse extends IPCResponse {
    databases?: string[];
}

export interface MatchQueryRequest {
    db: string;
    query: string;
}

export interface RolePlayerInstanceData {
    iid: string;
    role: string;
}

export interface RoleTypeData {
    relation: string;
    role: string;
}

export interface ConceptData {
    encoding: TypeDBVisualiserData.VertexEncoding;
    iid?: string;
    type?: string;
    value?: boolean | number | string | Date;
    label?: string;
    playerInstances?: RolePlayerInstanceData[];
    ownerIIDs?: string[];
    playsTypes?: RoleTypeData[];
    ownsLabels?: string[];
}

export type ConceptMapData = {[varName: string]: ConceptData};

export interface MatchQueryResponse extends IPCResponse {
    answers?: ConceptMapData[]; // TODO: Implement streaming behaviour
}
