export declare class GraknOptions {
    infer?: boolean;
    traceInference?: boolean;
    explain?: boolean;
    parallel?: boolean;
    batchSize?: number;
    prefetch?: boolean;
    sessionIdleTimeoutMillis?: number;
    schemaLockAcquireTimeoutMillis?: number;
    constructor(obj?: {
        [K in keyof GraknOptions]: GraknOptions[K];
    });
}
export declare class GraknClusterOptions extends GraknOptions {
    readAnyReplica?: boolean;
    constructor(obj?: {
        [K in keyof GraknClusterOptions]: GraknClusterOptions[K];
    });
}
export declare namespace GraknOptions {
    function core(options?: {
        [K in keyof GraknOptions]: GraknOptions[K];
    }): GraknOptions;
    function cluster(options?: {
        [K in keyof GraknClusterOptions]: GraknClusterOptions[K];
    }): GraknClusterOptions;
}
