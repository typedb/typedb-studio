import { ClientClusterRPC, ClientRPC, GraknClient, GraknClusterOptions, ServerAddress, SessionRPC, SessionType, TransactionRPC, TransactionType } from "../../dependencies_internal";
export declare class SessionClusterRPC implements GraknClient.Session {
    private readonly _clusterClient;
    private _coreClient;
    private _coreSession;
    private _options;
    constructor(clusterClient: ClientClusterRPC, serverAddress: ServerAddress);
    open(serverAddress: ServerAddress, database: string, type: SessionType, options: GraknClusterOptions): Promise<SessionClusterRPC>;
    transaction(type: TransactionType, options?: GraknClusterOptions): Promise<TransactionRPC>;
    private transactionPrimaryReplica;
    private transactionAnyReplica;
    type(): SessionType;
    isOpen(): boolean;
    options(): GraknClusterOptions;
    close(): Promise<void>;
    database(): GraknClient.Database;
    get clusterClient(): ClientClusterRPC;
    get coreClient(): ClientRPC;
    set coreClient(client: ClientRPC);
    get coreSession(): SessionRPC;
    set coreSession(session: SessionRPC);
}
