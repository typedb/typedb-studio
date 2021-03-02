import { GraknClient } from "../dependencies_internal";
import { GraknClient as GraknGrpc } from "grakn-protocol/protobuf/grakn_grpc_pb";
export declare class DatabaseRPC implements GraknClient.Database {
    private readonly _name;
    private readonly _grpcClient;
    constructor(grpcClient: GraknGrpc, name: string);
    name(): string;
    delete(): Promise<void>;
    toString(): string;
}
