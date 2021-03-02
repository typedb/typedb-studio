import { ErrorMessage } from "../../dependencies_internal";
import { ServiceError } from "@grpc/grpc-js";
export declare class GraknClientError extends Error {
    private readonly _errorMessage;
    constructor(error: string | Error | ServiceError | ErrorMessage);
    errorMessage(): ErrorMessage;
}
