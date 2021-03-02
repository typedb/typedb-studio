import { TransactionRPC, GraknOptions, Stream, ConceptMap, Numeric, ConceptMapGroup, NumericGroup } from "../dependencies_internal";
export declare class QueryManager {
    private readonly _transactionRPC;
    constructor(transaction: TransactionRPC);
    match(query: string, options?: GraknOptions): Stream<ConceptMap>;
    matchAggregate(query: string, options?: GraknOptions): Promise<Numeric>;
    matchGroup(query: string, options?: GraknOptions): Stream<ConceptMapGroup>;
    matchGroupAggregate(query: string, options?: GraknOptions): Stream<NumericGroup>;
    insert(query: string, options?: GraknOptions): Stream<ConceptMap>;
    delete(query: string, options?: GraknOptions): Promise<void>;
    update(query: string, options?: GraknOptions): Stream<ConceptMap>;
    define(query: string, options?: GraknOptions): Promise<void>;
    undefine(query: string, options?: GraknOptions): Promise<void>;
    private iterateQuery;
    private runQuery;
}
