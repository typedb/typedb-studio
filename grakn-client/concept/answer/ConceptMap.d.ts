import AnswerProto from "grakn-protocol/protobuf/answer_pb";
import { Concept } from "../../dependencies_internal";
export declare class ConceptMap {
    private readonly _map;
    constructor(map: Map<string, Concept>);
    static of(res: AnswerProto.ConceptMap): ConceptMap;
    map(): Map<string, Concept>;
    concepts(): IterableIterator<Concept>;
    get(variable: string): Concept;
    toString(): string;
}
