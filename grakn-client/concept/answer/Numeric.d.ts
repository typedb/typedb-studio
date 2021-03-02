import * as answer_pb from "grakn-protocol/protobuf/answer_pb";
export declare class Numeric {
    private readonly _numberValue?;
    private constructor();
    static of(answer: answer_pb.Numeric): Numeric;
    isNumber(): boolean;
    isNaN(): boolean;
    asNumber(): number;
    private static ofNumber;
    private static ofNaN;
    toString: () => string;
}
