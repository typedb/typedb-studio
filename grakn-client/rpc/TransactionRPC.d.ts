import { GraknClient, ConceptManager, GraknOptions, QueryManager, Stream, LogicManager, TransactionType } from "../dependencies_internal";
import TransactionProto from "grakn-protocol/protobuf/transaction_pb";
import GraknProto from "grakn-protocol/protobuf/grakn_grpc_pb";
import GraknGrpc = GraknProto.GraknClient;
export declare class TransactionRPC implements GraknClient.Transaction {
    private readonly _type;
    private readonly _conceptManager;
    private readonly _logicManager;
    private readonly _queryManager;
    private readonly _collectors;
    private readonly _grpcClient;
    private _stream;
    private _options;
    private _isOpen;
    private _networkLatencyMillis;
    constructor(grpcClient: GraknGrpc, type: TransactionType);
    open(sessionId: string, options?: GraknOptions): Promise<TransactionRPC>;
    type(): TransactionType;
    options(): GraknOptions;
    isOpen(): boolean;
    concepts(): ConceptManager;
    logic(): LogicManager;
    query(): QueryManager;
    commit(): Promise<void>;
    rollback(): Promise<void>;
    close(): Promise<void>;
    execute<T>(request: TransactionProto.Transaction.Req, transformResponse?: (res: TransactionProto.Transaction.Res) => T): Promise<T>;
    stream<T>(request: TransactionProto.Transaction.Req, transformResponse: (res: TransactionProto.Transaction.Res) => T[]): Stream<T>;
    private openTransactionStream;
}
export declare class ResponseCollector {
    private _responseBuffer;
    constructor();
    add(response: Response): void;
    take(): Promise<TransactionProto.Transaction.Res>;
}
declare abstract class Response {
    abstract read(): TransactionProto.Transaction.Res;
}
export {};
