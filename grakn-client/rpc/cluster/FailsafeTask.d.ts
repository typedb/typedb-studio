import { ClientClusterRPC, DatabaseReplicaRPC } from "../../dependencies_internal";
export declare abstract class FailsafeTask<TResult> {
    private readonly _client;
    private readonly _database;
    protected constructor(client: ClientClusterRPC, database: string);
    abstract run(replica: DatabaseReplicaRPC): Promise<TResult>;
    rerun(replica: DatabaseReplicaRPC): Promise<TResult>;
    runPrimaryReplica(): Promise<TResult>;
    runAnyReplica(): Promise<TResult>;
    protected get client(): ClientClusterRPC;
    protected get database(): string;
    private seekPrimaryReplica;
    private fetchDatabaseReplicas;
    private waitForPrimaryReplicaSelection;
    private clusterNotAvailableError;
}
