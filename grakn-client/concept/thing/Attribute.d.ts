import { Thing, RemoteThing, AttributeType, GraknClient, Merge, Stream, ThingType, BooleanAttributeType, LongAttributeType, DoubleAttributeType, StringAttributeType, DateTimeAttributeType } from "../../dependencies_internal";
import ValueClass = AttributeType.ValueClass;
import Transaction = GraknClient.Transaction;
export interface Attribute<T extends ValueClass> extends Thing {
    getType(): AttributeType;
    getValue(): T;
    asRemote(transaction: Transaction): RemoteAttribute<T>;
    isBoolean(): boolean;
    isLong(): boolean;
    isDouble(): boolean;
    isString(): boolean;
    isDateTime(): boolean;
}
export interface RemoteAttribute<T extends ValueClass> extends RemoteThing {
    getType(): AttributeType;
    getValue(): T;
    asRemote(transaction: Transaction): RemoteAttribute<T>;
    isBoolean(): boolean;
    isLong(): boolean;
    isDouble(): boolean;
    isString(): boolean;
    isDateTime(): boolean;
    getOwners(): Stream<Thing>;
    getOwners(ownerType: ThingType): Stream<Thing>;
}
export interface BooleanAttribute extends Attribute<boolean> {
    getType(): BooleanAttributeType;
    asRemote(transaction: Transaction): RemoteBooleanAttribute;
}
export interface RemoteBooleanAttribute extends Merge<RemoteAttribute<boolean>, BooleanAttribute> {
    asRemote(transaction: Transaction): RemoteBooleanAttribute;
}
export interface LongAttribute extends Attribute<number> {
    getType(): LongAttributeType;
    asRemote(transaction: Transaction): RemoteLongAttribute;
}
export interface RemoteLongAttribute extends Merge<RemoteAttribute<number>, LongAttribute> {
    asRemote(transaction: Transaction): RemoteLongAttribute;
}
export interface DoubleAttribute extends Attribute<number> {
    getType(): DoubleAttributeType;
    asRemote(transaction: Transaction): RemoteDoubleAttribute;
}
export interface RemoteDoubleAttribute extends Merge<RemoteAttribute<number>, LongAttribute> {
    asRemote(transaction: Transaction): RemoteDoubleAttribute;
}
export interface StringAttribute extends Attribute<string> {
    getType(): StringAttributeType;
    asRemote(transaction: Transaction): RemoteStringAttribute;
}
export interface RemoteStringAttribute extends Merge<RemoteAttribute<string>, StringAttribute> {
    asRemote(transaction: Transaction): RemoteStringAttribute;
}
export interface DateTimeAttribute extends Attribute<Date> {
    getType(): DateTimeAttributeType;
    asRemote(transaction: Transaction): RemoteDateTimeAttribute;
}
export interface RemoteDateTimeAttribute extends Merge<RemoteAttribute<Date>, DateTimeAttribute> {
    asRemote(transaction: Transaction): RemoteDateTimeAttribute;
}
