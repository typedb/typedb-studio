import { GraknClient, GraknOptions, TransactionRPC, ClientRPC, DatabaseRPC, SessionType, TransactionType } from "../dependencies_internal";
export declare class SessionRPC implements GraknClient.Session {
    private readonly _client;
    private readonly _grpcClient;
    private readonly _database;
    private readonly _type;
    private _options;
    private _id;
    private _isOpen;
    private _pulse;
    constructor(client: ClientRPC, database: string, type: SessionType);
    open(options?: GraknOptions): Promise<SessionRPC>;
    transaction(type: TransactionType, options?: GraknOptions): Promise<TransactionRPC>;
    type(): SessionType;
    options(): GraknOptions;
    isOpen(): boolean;
    close(): Promise<void>;
    database(): DatabaseRPC;
    id(): string;
    private pulse;
}
