import { GraknClient, ClientClusterRPC, DatabaseManagerRPC, DatabaseClusterRPC } from "../../dependencies_internal";
export declare class DatabaseManagerClusterRPC implements GraknClient.DatabaseManagerCluster {
    private readonly _databaseManagers;
    private readonly _client;
    constructor(client: ClientClusterRPC, databaseManagers: {
        [serverAddress: string]: DatabaseManagerRPC;
    });
    contains(name: string): Promise<boolean>;
    create(name: string): Promise<void>;
    get(name: string): Promise<DatabaseClusterRPC>;
    all(): Promise<DatabaseClusterRPC[]>;
    databaseManagers(): {
        [address: string]: DatabaseManagerRPC;
    };
}
