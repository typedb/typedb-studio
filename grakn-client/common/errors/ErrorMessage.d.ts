import { Stringable } from "../../dependencies_internal";
export declare abstract class ErrorMessage {
    private readonly _codePrefix;
    private readonly _codeNumber;
    private readonly _messagePrefix;
    private readonly _messageBody;
    private _code;
    private static knownErrors;
    private static maxCodeNumber;
    private static maxCodeDigits;
    protected constructor(codePrefix: string, codeNumber: number, messagePrefix: string, messageBody: (args: Stringable[]) => string);
    code(): string;
    message(...args: Stringable[]): string;
    toString(): string;
}
export declare namespace ErrorMessage {
    class Client extends ErrorMessage {
        constructor(code: number, message: (args?: Stringable[]) => string);
    }
    namespace Client {
        const TRANSACTION_CLOSED: Client;
        const UNABLE_TO_CONNECT: Client;
        const NEGATIVE_VALUE_NOT_ALLOWED: Client;
        const MISSING_DB_NAME: Client;
        const DB_DOES_NOT_EXIST: Client;
        const MISSING_RESPONSE: Client;
        const UNKNOWN_REQUEST_ID: Client;
        const CLUSTER_NO_PRIMARY_REPLICA_YET: Client;
        const CLUSTER_UNABLE_TO_CONNECT: Client;
        const CLUSTER_REPLICA_NOT_PRIMARY: Client;
        const CLUSTER_ALL_NODES_FAILED: Client;
        const UNRECOGNISED_SESSION_TYPE: Client;
    }
    class Concept extends ErrorMessage {
        constructor(code: number, message: (args: Stringable[]) => string);
    }
    namespace Concept {
        const INVALID_CONCEPT_CASTING: Concept;
        const MISSING_TRANSACTION: Concept;
        const MISSING_IID: Concept;
        const MISSING_LABEL: Concept;
        const BAD_ENCODING: Concept;
        const BAD_VALUE_TYPE: Concept;
        const BAD_ATTRIBUTE_VALUE: Concept;
    }
    class Query extends ErrorMessage {
        constructor(code: number, message: (args: Stringable[]) => string);
    }
    namespace Query {
        const VARIABLE_DOES_NOT_EXIST: Query;
        const NO_EXPLANATION: Query;
        const BAD_ANSWER_TYPE: Query;
        const MISSING_ANSWER: Query;
    }
    class Internal extends ErrorMessage {
        constructor(code: number, message: (args: Stringable[]) => string);
    }
    namespace Internal {
        const ILLEGAL_CAST: Internal;
        const ILLEGAL_ARGUMENT: Internal;
    }
}
