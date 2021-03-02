import { ClientRPC, GraknClientCluster, SessionType, GraknClusterOptions, DatabaseManagerClusterRPC, DatabaseClusterRPC, SessionClusterRPC } from "../../dependencies_internal";
import { GraknClusterClient as GraknClusterGrpc } from "grakn-protocol/protobuf/cluster/grakn_cluster_grpc_pb";
export declare class ClientClusterRPC implements GraknClientCluster {
    private _coreClients;
    private _graknClusterRPCs;
    private _databaseManagers;
    private _clusterDatabases;
    private _isOpen;
    open(addresses: string[]): Promise<this>;
    session(database: string, type: SessionType, options?: GraknClusterOptions): Promise<SessionClusterRPC>;
    private sessionPrimaryReplica;
    private sessionAnyReplica;
    databases(): DatabaseManagerClusterRPC;
    isOpen(): boolean;
    close(): void;
    isCluster(): boolean;
    clusterDatabases(): {
        [db: string]: DatabaseClusterRPC;
    };
    clusterMembers(): string[];
    coreClient(address: string): ClientRPC;
    graknClusterRPC(address: string): GraknClusterGrpc;
    private fetchClusterServers;
}
