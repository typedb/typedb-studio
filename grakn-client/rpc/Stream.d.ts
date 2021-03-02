import { ClientWritableStream } from "@grpc/grpc-js";
import TransactionProto from "grakn-protocol/protobuf/transaction_pb";
import { ResponseCollector } from "../dependencies_internal";
export declare class Stream<T> implements AsyncIterable<T> {
    private readonly _requestId;
    private readonly _writableStream;
    private readonly _responseCollector;
    private readonly _transformResponse;
    private _receivedAnswers;
    constructor(requestId: string, writableStream: ClientWritableStream<TransactionProto.Transaction.Req>, responseCollector: ResponseCollector, transformResponse: (res: TransactionProto.Transaction.Res) => T[]);
    [Symbol.asyncIterator](): AsyncIterator<T, any, undefined>;
    next(): Promise<T>;
    collect(): Promise<T[]>;
    every(callbackFn: (value: T) => unknown): Promise<boolean>;
    filter(callbackFn: (value: T) => unknown): Stream<T>;
    map<TResult>(callbackFn: (value: T) => TResult): Stream<TResult>;
    some(callbackFn: (value: T) => unknown): Promise<boolean>;
}
