import { GraknOptions, ConceptManager, QueryManager, LogicManager, ServerAddress } from "./dependencies_internal";
export interface GraknClient {
    session(databaseName: string, type: SessionType, options?: GraknOptions): Promise<GraknClient.Session>;
    databases(): GraknClient.DatabaseManager;
    isOpen(): boolean;
    close(): void;
    isCluster(): boolean;
}
export interface GraknClientCluster extends GraknClient {
    databases(): GraknClient.DatabaseManagerCluster;
}
export declare namespace GraknClient {
    const DEFAULT_ADDRESS = "localhost:1729";
    function core(address?: string): GraknClient;
    function cluster(addresses: string[]): Promise<GraknClientCluster>;
    interface DatabaseManager {
        contains(name: string): Promise<boolean>;
        create(name: string): Promise<void>;
        get(name: string): Promise<Database>;
        all(): Promise<Database[]>;
    }
    interface DatabaseManagerCluster extends DatabaseManager {
        get(name: string): Promise<DatabaseCluster>;
        all(): Promise<DatabaseCluster[]>;
    }
    interface Database {
        name(): string;
        delete(): Promise<void>;
    }
    interface DatabaseCluster extends Database {
        replicas(): DatabaseReplica[];
        primaryReplica(): DatabaseReplica;
        preferredSecondaryReplica(): DatabaseReplica;
    }
    interface DatabaseReplica {
        database(): DatabaseCluster;
        term(): number;
        isPrimary(): boolean;
        isPreferredSecondary(): boolean;
        address(): ServerAddress;
    }
    interface Session {
        transaction(type: TransactionType, options?: GraknOptions): Promise<Transaction>;
        type(): SessionType;
        options(): GraknOptions;
        isOpen(): boolean;
        close(): Promise<void>;
        database(): Database;
    }
    interface Transaction {
        type(): TransactionType;
        options(): GraknOptions;
        isOpen(): boolean;
        concepts(): ConceptManager;
        logic(): LogicManager;
        query(): QueryManager;
        commit(): Promise<void>;
        rollback(): Promise<void>;
        close(): Promise<void>;
    }
}
export declare enum SessionType {
    DATA = 0,
    SCHEMA = 1
}
export declare enum TransactionType {
    READ = 0,
    WRITE = 1
}
