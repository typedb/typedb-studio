import Transaction = GraknClient.Transaction;
import { Attribute, BooleanAttribute, DateTimeAttribute, DoubleAttribute, LongAttribute, StringAttribute, GraknClient, RemoteThingType, ThingType, Merge, Stream } from "../../dependencies_internal";
import ValueType = AttributeType.ValueType;
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
import ValueClass = AttributeType.ValueClass;
export interface AttributeType extends ThingType {
    getValueType(): ValueType;
    isKeyable(): boolean;
    isBoolean(): boolean;
    isLong(): boolean;
    isDouble(): boolean;
    isString(): boolean;
    isDateTime(): boolean;
    asBoolean(): BooleanAttributeType;
    asLong(): LongAttributeType;
    asDouble(): DoubleAttributeType;
    asString(): StringAttributeType;
    asDateTime(): DateTimeAttributeType;
    asRemote(transaction: Transaction): RemoteAttributeType;
}
export interface RemoteAttributeType extends Merge<RemoteThingType, AttributeType> {
    setSupertype(type: AttributeType): Promise<void>;
    getSubtypes(): Stream<AttributeType>;
    getInstances(): Stream<Attribute<ValueClass>>;
    getOwners(): Stream<ThingType>;
    getOwners(onlyKey: boolean): Stream<ThingType>;
    asBoolean(): RemoteBooleanAttributeType;
    asLong(): RemoteLongAttributeType;
    asDouble(): RemoteDoubleAttributeType;
    asString(): RemoteStringAttributeType;
    asDateTime(): RemoteDateTimeAttributeType;
    asRemote(transaction: Transaction): RemoteAttributeType;
}
export interface BooleanAttributeType extends AttributeType {
    asRemote(transaction: Transaction): RemoteBooleanAttributeType;
}
export interface RemoteBooleanAttributeType extends Merge<RemoteAttributeType, BooleanAttributeType> {
    asRemote(transaction: Transaction): RemoteBooleanAttributeType;
    setSupertype(type: BooleanAttributeType): Promise<void>;
    getSubtypes(): Stream<BooleanAttributeType>;
    getInstances(): Stream<BooleanAttribute>;
    put(value: boolean): Promise<BooleanAttribute>;
    get(value: boolean): Promise<BooleanAttribute>;
}
export interface LongAttributeType extends AttributeType {
    asRemote(transaction: Transaction): RemoteLongAttributeType;
}
export interface RemoteLongAttributeType extends Merge<RemoteAttributeType, LongAttributeType> {
    asRemote(transaction: Transaction): RemoteLongAttributeType;
    setSupertype(type: LongAttributeType): Promise<void>;
    getSubtypes(): Stream<LongAttributeType>;
    getInstances(): Stream<LongAttribute>;
    put(value: number): Promise<LongAttribute>;
    get(value: number): Promise<LongAttribute>;
}
export interface DoubleAttributeType extends AttributeType {
    asRemote(transaction: Transaction): RemoteDoubleAttributeType;
}
export interface RemoteDoubleAttributeType extends Merge<RemoteAttributeType, DoubleAttributeType> {
    asRemote(transaction: Transaction): RemoteDoubleAttributeType;
    setSupertype(type: DoubleAttributeType): Promise<void>;
    getSubtypes(): Stream<DoubleAttributeType>;
    getInstances(): Stream<DoubleAttribute>;
    put(value: number): Promise<DoubleAttribute>;
    get(value: number): Promise<DoubleAttribute>;
}
export interface StringAttributeType extends AttributeType {
    asRemote(transaction: Transaction): RemoteStringAttributeType;
}
export interface RemoteStringAttributeType extends Merge<RemoteAttributeType, StringAttributeType> {
    asRemote(transaction: Transaction): RemoteStringAttributeType;
    setSupertype(type: StringAttributeType): Promise<void>;
    getSubtypes(): Stream<StringAttributeType>;
    getInstances(): Stream<StringAttribute>;
    put(value: string): Promise<StringAttribute>;
    get(value: string): Promise<StringAttribute>;
    getRegex(): Promise<string>;
    setRegex(regex: string): Promise<void>;
}
export interface DateTimeAttributeType extends AttributeType {
    asRemote(transaction: Transaction): RemoteDateTimeAttributeType;
}
export interface RemoteDateTimeAttributeType extends Merge<RemoteAttributeType, DateTimeAttributeType> {
    asRemote(transaction: Transaction): RemoteDateTimeAttributeType;
    setSupertype(type: DateTimeAttributeType): Promise<void>;
    getSubtypes(): Stream<DateTimeAttributeType>;
    getInstances(): Stream<DateTimeAttribute>;
    put(value: Date): Promise<DateTimeAttribute>;
    get(value: Date): Promise<DateTimeAttribute>;
}
export declare namespace AttributeType {
    enum ValueType {
        OBJECT = "OBJECT",
        BOOLEAN = "BOOLEAN",
        LONG = "LONG",
        DOUBLE = "DOUBLE",
        STRING = "STRING",
        DATETIME = "DATETIME"
    }
    namespace ValueType {
        function of(valueType: ConceptProto.AttributeType.ValueType): ValueType;
        function isKeyable(valueType: ValueType): boolean;
        function isWritable(valueType: ValueType): boolean;
    }
    type ValueClass = number | string | boolean | Date;
}
