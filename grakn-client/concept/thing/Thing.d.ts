import { Attribute, BooleanAttribute, DateTimeAttribute, DoubleAttribute, LongAttribute, StringAttribute, Concept, RemoteConcept, ThingType, AttributeType, BooleanAttributeType, DateTimeAttributeType, DoubleAttributeType, LongAttributeType, StringAttributeType, RoleType, GraknClient, Stream, Relation } from "../../dependencies_internal";
import Transaction = GraknClient.Transaction;
import ValueClass = AttributeType.ValueClass;
export interface Thing extends Concept {
    getIID(): string;
    getType(): ThingType;
    asRemote(transaction: Transaction): RemoteThing;
}
export interface RemoteThing extends RemoteConcept {
    getIID(): string;
    getType(): ThingType;
    asRemote(transaction: Transaction): RemoteThing;
    isInferred(): Promise<boolean>;
    setHas(attribute: Attribute<ValueClass>): Promise<void>;
    unsetHas(attribute: Attribute<ValueClass>): Promise<void>;
    getHas(onlyKey: boolean): Stream<Attribute<ValueClass>>;
    getHas(attributeType: BooleanAttributeType): Stream<BooleanAttribute>;
    getHas(attributeType: LongAttributeType): Stream<LongAttribute>;
    getHas(attributeType: DoubleAttributeType): Stream<DoubleAttribute>;
    getHas(attributeType: StringAttributeType): Stream<StringAttribute>;
    getHas(attributeType: DateTimeAttributeType): Stream<DateTimeAttribute>;
    getHas(): Stream<Attribute<ValueClass>>;
    getHas(attributeTypes: AttributeType[]): Stream<Attribute<ValueClass>>;
    getPlays(): Stream<RoleType>;
    getRelations(): Stream<Relation>;
    getRelations(roleTypes: RoleType[]): Stream<Relation>;
}
