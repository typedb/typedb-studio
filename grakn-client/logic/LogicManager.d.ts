import { TransactionRPC, Rule, RuleImpl, Stream } from "../dependencies_internal";
export declare class LogicManager {
    private readonly _transactionRPC;
    constructor(transactionRPC: TransactionRPC);
    putRule(label: string, when: string, then: string): Promise<Rule>;
    getRule(label: string): Promise<Rule>;
    getRules(): Stream<RuleImpl>;
    private execute;
    private ruleStream;
}
