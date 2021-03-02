import { GraknClient, GraknOptions, DatabaseManagerRPC, SessionRPC, SessionType } from "../dependencies_internal";
import { GraknClient as GraknGrpc } from "grakn-protocol/protobuf/grakn_grpc_pb";
export declare const DEFAULT_URI = "localhost:1729";
export declare class ClientRPC implements GraknClient {
    private readonly _graknGrpc;
    private readonly _databases;
    private readonly _sessions;
    private _isOpen;
    constructor(address?: string);
    session(database: string, type: SessionType, options?: GraknOptions): Promise<SessionRPC>;
    databases(): DatabaseManagerRPC;
    isOpen(): boolean;
    close(): void;
    isCluster(): boolean;
    removeSession(session: SessionRPC): void;
    grpcClient(): GraknGrpc;
}
