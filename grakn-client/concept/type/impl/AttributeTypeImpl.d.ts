import { AttributeImpl, AttributeType, BooleanAttributeImpl, BooleanAttributeType, DateTimeAttributeImpl, DateTimeAttributeType, DoubleAttributeImpl, DoubleAttributeType, GraknClient, LongAttributeImpl, LongAttributeType, RemoteAttributeType, RemoteBooleanAttributeType, RemoteDateTimeAttributeType, RemoteDoubleAttributeType, RemoteLongAttributeType, RemoteStringAttributeType, RemoteThingTypeImpl, Stream, StringAttributeImpl, StringAttributeType, ThingTypeImpl } from "../../../dependencies_internal";
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
import Transaction = GraknClient.Transaction;
import ValueType = AttributeType.ValueType;
import ValueClass = AttributeType.ValueClass;
export declare class AttributeTypeImpl extends ThingTypeImpl implements AttributeType {
    private static ROOT_LABEL;
    constructor(label: string, isRoot: boolean);
    getValueType(): ValueType;
    isKeyable(): boolean;
    asRemote(transaction: Transaction): RemoteAttributeTypeImpl;
    isAttributeType(): boolean;
    isBoolean(): boolean;
    isString(): boolean;
    isDouble(): boolean;
    isLong(): boolean;
    isDateTime(): boolean;
    asBoolean(): BooleanAttributeType;
    asLong(): LongAttributeType;
    asDouble(): DoubleAttributeType;
    asString(): StringAttributeType;
    asDateTime(): DateTimeAttributeType;
}
export declare class RemoteAttributeTypeImpl extends RemoteThingTypeImpl implements RemoteAttributeType {
    private static ROOT_LABEL;
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    isAttributeType(): boolean;
    isBoolean(): boolean;
    isString(): boolean;
    isDouble(): boolean;
    isLong(): boolean;
    isDateTime(): boolean;
    asBoolean(): RemoteBooleanAttributeType;
    asLong(): RemoteLongAttributeType;
    asDouble(): RemoteDoubleAttributeType;
    asString(): RemoteStringAttributeType;
    asDateTime(): RemoteDateTimeAttributeType;
    getValueType(): ValueType;
    isKeyable(): boolean;
    setSupertype(attributeType: AttributeType): Promise<void>;
    getSubtypes(): Stream<AttributeTypeImpl>;
    getInstances(): Stream<AttributeImpl<ValueClass>>;
    getOwners(onlyKey?: boolean): Stream<ThingTypeImpl>;
    protected putInternal(valueProto: ConceptProto.Attribute.Value): Promise<AttributeImpl<ValueClass>>;
    protected getInternal(valueProto: ConceptProto.Attribute.Value): Promise<AttributeImpl<ValueClass>>;
    asRemote(transaction: Transaction): RemoteAttributeTypeImpl;
}
export declare class BooleanAttributeTypeImpl extends AttributeTypeImpl implements BooleanAttributeType {
    constructor(label: string, isRoot: boolean);
    isBoolean(): boolean;
    asBoolean(): BooleanAttributeType;
    static of(typeProto: ConceptProto.Type): BooleanAttributeTypeImpl;
    getValueType(): ValueType;
    asRemote(transaction: Transaction): RemoteBooleanAttributeTypeImpl;
}
export declare class RemoteBooleanAttributeTypeImpl extends RemoteAttributeTypeImpl implements RemoteBooleanAttributeType {
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    isBoolean(): boolean;
    asBoolean(): RemoteBooleanAttributeType;
    getValueType(): ValueType;
    asRemote(transaction: Transaction): RemoteBooleanAttributeTypeImpl;
    getSubtypes(): Stream<BooleanAttributeTypeImpl>;
    getInstances(): Stream<BooleanAttributeImpl>;
    setSupertype(type: BooleanAttributeType): Promise<void>;
    put(value: boolean): Promise<BooleanAttributeImpl>;
    get(value: boolean): Promise<BooleanAttributeImpl>;
}
export declare class LongAttributeTypeImpl extends AttributeTypeImpl implements LongAttributeType {
    constructor(label: string, isRoot: boolean);
    static of(typeProto: ConceptProto.Type): LongAttributeTypeImpl;
    getValueType(): ValueType;
    isLong(): boolean;
    asLong(): LongAttributeType;
    asRemote(transaction: Transaction): RemoteLongAttributeTypeImpl;
}
export declare class RemoteLongAttributeTypeImpl extends RemoteAttributeTypeImpl implements RemoteLongAttributeType {
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    getValueType(): ValueType;
    isLong(): boolean;
    asLong(): RemoteLongAttributeType;
    asRemote(transaction: Transaction): RemoteLongAttributeTypeImpl;
    getSubtypes(): Stream<LongAttributeTypeImpl>;
    getInstances(): Stream<LongAttributeImpl>;
    setSupertype(type: LongAttributeType): Promise<void>;
    put(value: number): Promise<LongAttributeImpl>;
    get(value: number): Promise<LongAttributeImpl>;
}
export declare class DoubleAttributeTypeImpl extends AttributeTypeImpl implements DoubleAttributeType {
    constructor(label: string, isRoot: boolean);
    static of(typeProto: ConceptProto.Type): DoubleAttributeTypeImpl;
    getValueType(): ValueType;
    isDouble(): boolean;
    asDouble(): DoubleAttributeType;
    asRemote(transaction: Transaction): RemoteDoubleAttributeTypeImpl;
}
export declare class RemoteDoubleAttributeTypeImpl extends RemoteAttributeTypeImpl implements RemoteDoubleAttributeType {
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    getValueType(): ValueType;
    asRemote(transaction: Transaction): RemoteDoubleAttributeTypeImpl;
    isDouble(): boolean;
    asDouble(): RemoteDoubleAttributeType;
    getSubtypes(): Stream<DoubleAttributeTypeImpl>;
    getInstances(): Stream<DoubleAttributeImpl>;
    setSupertype(type: DoubleAttributeType): Promise<void>;
    put(value: number): Promise<DoubleAttributeImpl>;
    get(value: number): Promise<DoubleAttributeImpl>;
}
export declare class StringAttributeTypeImpl extends AttributeTypeImpl implements StringAttributeType {
    constructor(label: string, isRoot: boolean);
    static of(typeProto: ConceptProto.Type): StringAttributeTypeImpl;
    getValueType(): ValueType;
    isString(): boolean;
    asString(): StringAttributeType;
    asRemote(transaction: Transaction): RemoteStringAttributeTypeImpl;
}
export declare class RemoteStringAttributeTypeImpl extends RemoteAttributeTypeImpl implements RemoteStringAttributeType {
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    getValueType(): ValueType;
    asRemote(transaction: Transaction): RemoteStringAttributeTypeImpl;
    isString(): boolean;
    asString(): RemoteStringAttributeType;
    getSubtypes(): Stream<StringAttributeTypeImpl>;
    getInstances(): Stream<StringAttributeImpl>;
    setSupertype(type: StringAttributeType): Promise<void>;
    put(value: string): Promise<StringAttributeImpl>;
    get(value: string): Promise<StringAttributeImpl>;
    getRegex(): Promise<string>;
    setRegex(regex: string): Promise<void>;
}
export declare class DateTimeAttributeTypeImpl extends AttributeTypeImpl implements DateTimeAttributeType {
    constructor(label: string, isRoot: boolean);
    static of(typeProto: ConceptProto.Type): DateTimeAttributeTypeImpl;
    getValueType(): ValueType;
    isDateTime(): boolean;
    asDateTime(): DateTimeAttributeType;
    asRemote(transaction: Transaction): RemoteDateTimeAttributeTypeImpl;
}
export declare class RemoteDateTimeAttributeTypeImpl extends RemoteAttributeTypeImpl implements RemoteDateTimeAttributeType {
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    getValueType(): ValueType;
    isDateTime(): boolean;
    asDateTime(): RemoteDateTimeAttributeType;
    asRemote(transaction: Transaction): RemoteDateTimeAttributeTypeImpl;
    getSubtypes(): Stream<DateTimeAttributeTypeImpl>;
    getInstances(): Stream<DateTimeAttributeImpl>;
    setSupertype(type: DateTimeAttributeType): Promise<void>;
    put(value: Date): Promise<DateTimeAttributeImpl>;
    get(value: Date): Promise<DateTimeAttributeImpl>;
}
export declare namespace AttributeTypeImpl {
    function of(typeProto: ConceptProto.Type): AttributeTypeImpl;
}
