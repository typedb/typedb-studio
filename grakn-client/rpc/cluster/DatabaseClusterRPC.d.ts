import { GraknClient, ServerAddress, DatabaseManagerClusterRPC } from "../../dependencies_internal";
import DatabaseProto from "grakn-protocol/protobuf/cluster/database_pb";
export declare class DatabaseClusterRPC implements GraknClient.DatabaseCluster {
    private readonly _name;
    private readonly _databases;
    private readonly _databaseManagerCluster;
    private readonly _replicas;
    private constructor();
    static of(protoDB: DatabaseProto.Database, databaseManagerCluster: DatabaseManagerClusterRPC): DatabaseClusterRPC;
    primaryReplica(): DatabaseReplicaRPC;
    preferredSecondaryReplica(): DatabaseReplicaRPC;
    name(): string;
    delete(): Promise<void>;
    replicas(): DatabaseReplicaRPC[];
    toString(): string;
}
export declare class DatabaseReplicaRPC implements GraknClient.DatabaseReplica {
    private readonly _id;
    private readonly _database;
    private readonly _isPrimary;
    private readonly _isPreferredSecondary;
    private readonly _term;
    private constructor();
    static of(replica: DatabaseProto.Database.Replica, database: DatabaseClusterRPC): DatabaseReplicaRPC;
    id(): ReplicaId;
    database(): DatabaseClusterRPC;
    term(): number;
    isPrimary(): boolean;
    isPreferredSecondary(): boolean;
    address(): ServerAddress;
    toString(): string;
}
declare class ReplicaId {
    private readonly _address;
    private readonly _databaseName;
    constructor(address: ServerAddress, databaseName: string);
    address(): ServerAddress;
    toString(): string;
}
export {};
