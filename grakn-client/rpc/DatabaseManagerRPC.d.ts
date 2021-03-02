import { GraknClient } from "../dependencies_internal";
import { GraknClient as GraknGrpc } from "grakn-protocol/protobuf/grakn_grpc_pb";
import { DatabaseRPC } from "./DatabaseRPC";
export declare class DatabaseManagerRPC implements GraknClient.DatabaseManager {
    private readonly _grpcClient;
    constructor(client: GraknGrpc);
    contains(name: string): Promise<boolean>;
    create(name: string): Promise<void>;
    get(name: string): Promise<DatabaseRPC>;
    all(): Promise<DatabaseRPC[]>;
    grpcClient(): GraknGrpc;
}
