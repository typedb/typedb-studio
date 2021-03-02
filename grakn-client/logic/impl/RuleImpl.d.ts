import { RemoteRule, Rule, GraknClient, TransactionRPC } from "../../dependencies_internal";
import LogicProto from "grakn-protocol/protobuf/logic_pb";
import Transaction = GraknClient.Transaction;
export declare class RuleImpl implements Rule {
    private readonly _label;
    private readonly _when;
    private readonly _then;
    protected constructor(label: string, when: string, then: string);
    static of(ruleProto: LogicProto.Rule): RuleImpl;
    getLabel(): string;
    getThen(): string;
    getWhen(): string;
    asRemote(transaction: Transaction): RemoteRule;
    isRemote(): boolean;
    toString(): string;
}
export declare class RemoteRuleImpl implements RemoteRule {
    private _label;
    private readonly _when;
    private readonly _then;
    private readonly _rpcTransaction;
    constructor(transaction: Transaction, label: string, when: string, then: string);
    getLabel(): string;
    getThen(): string;
    getWhen(): string;
    setLabel(label: string): Promise<void>;
    delete(): Promise<void>;
    isDeleted(): Promise<boolean>;
    asRemote(transaction: Transaction): RemoteRule;
    isRemote(): boolean;
    protected execute(method: LogicProto.Rule.Req): Promise<LogicProto.Rule.Res>;
    protected get rpcTransaction(): TransactionRPC;
}
