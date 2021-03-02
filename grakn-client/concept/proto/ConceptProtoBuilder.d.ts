import ConceptProto from "grakn-protocol/protobuf/concept_pb";
import { Type, Thing, AttributeType } from "../../dependencies_internal";
export declare namespace ConceptProtoBuilder {
    function thing(thing: Thing): ConceptProto.Thing;
    function type(type: Type): ConceptProto.Type;
    function types(types: Type[]): ConceptProto.Type[];
    function booleanAttributeValue(value: boolean): ConceptProto.Attribute.Value;
    function longAttributeValue(value: number): ConceptProto.Attribute.Value;
    function doubleAttributeValue(value: number): ConceptProto.Attribute.Value;
    function stringAttributeValue(value: string): ConceptProto.Attribute.Value;
    function dateTimeAttributeValue(value: Date): ConceptProto.Attribute.Value;
    function valueType(valueType: AttributeType.ValueType): ConceptProto.AttributeType.ValueType;
    function encoding(type: Type): ConceptProto.Type.Encoding;
}
