import AnswerProto from "grakn-protocol/protobuf/answer_pb";
import { Concept, ConceptMap } from "../../dependencies_internal";
export declare class ConceptMapGroup {
    private readonly _owner;
    private readonly _conceptMaps;
    constructor(owner: Concept, conceptMaps: ConceptMap[]);
    static of(res: AnswerProto.ConceptMapGroup): ConceptMapGroup;
    owner(): Concept;
    conceptMaps(): ConceptMap[];
}
