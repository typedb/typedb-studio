import AnswerProto from "grakn-protocol/protobuf/answer_pb";
import { Concept, Numeric } from "../../dependencies_internal";
export declare class NumericGroup {
    private readonly _owner;
    private readonly _numeric;
    constructor(owner: Concept, numeric: Numeric);
    static of(res: AnswerProto.NumericGroup): NumericGroup;
    owner(): Concept;
    numeric(): Numeric;
}
