import { Thing, RemoteThing, Attribute, AttributeType, BooleanAttributeType, DateTimeAttributeType, DoubleAttributeType, LongAttributeType, StringAttributeType, RoleType, GraknClient, ThingTypeImpl, RoleTypeImpl, Stream, RelationImpl, TypeImpl, AttributeImpl, BooleanAttributeImpl, DateTimeAttributeImpl, DoubleAttributeImpl, LongAttributeImpl, StringAttributeImpl, ConceptImpl, RemoteConceptImpl, Concept } from "../../../dependencies_internal";
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
import Transaction = GraknClient.Transaction;
import ValueClass = AttributeType.ValueClass;
export declare abstract class ThingImpl extends ConceptImpl implements Thing {
    private readonly _iid;
    protected constructor(iid: string);
    getIID(): string;
    abstract getType(): ThingTypeImpl;
    isRemote(): boolean;
    isThing(): boolean;
    toString(): string;
    equals(concept: Concept): boolean;
    abstract asRemote(transaction: Transaction): RemoteThing;
}
export declare abstract class RemoteThingImpl extends RemoteConceptImpl implements RemoteThing {
    private readonly _iid;
    private readonly _transactionRPC;
    protected constructor(transaction: Transaction, iid: string);
    getIID(): string;
    abstract getType(): ThingTypeImpl;
    isInferred(): Promise<boolean>;
    isRemote(): boolean;
    isThing(): boolean;
    equals(concept: Concept): boolean;
    getHas(onlyKey: boolean): Stream<AttributeImpl<ValueClass>>;
    getHas(attributeType: BooleanAttributeType): Stream<BooleanAttributeImpl>;
    getHas(attributeType: LongAttributeType): Stream<LongAttributeImpl>;
    getHas(attributeType: DoubleAttributeType): Stream<DoubleAttributeImpl>;
    getHas(attributeType: StringAttributeType): Stream<StringAttributeImpl>;
    getHas(attributeType: DateTimeAttributeType): Stream<DateTimeAttributeImpl>;
    getHas(attributeTypes: AttributeType[]): Stream<AttributeImpl<ValueClass>>;
    getHas(): Stream<AttributeImpl<ValueClass>>;
    getPlays(): Stream<RoleTypeImpl>;
    getRelations(roleTypes?: RoleType[]): Stream<RelationImpl>;
    setHas(attribute: Attribute<AttributeType.ValueClass>): Promise<void>;
    unsetHas(attribute: Attribute<AttributeType.ValueClass>): Promise<void>;
    delete(): Promise<void>;
    isDeleted(): Promise<boolean>;
    protected get transaction(): Transaction;
    protected typeStream(method: ConceptProto.Thing.Req, typeGetter: (res: ConceptProto.Thing.Res) => ConceptProto.Type[]): Stream<TypeImpl>;
    protected thingStream(method: ConceptProto.Thing.Req, thingGetter: (res: ConceptProto.Thing.Res) => ConceptProto.Thing[]): Stream<ThingImpl>;
    protected execute(method: ConceptProto.Thing.Req): Promise<ConceptProto.Thing.Res>;
    toString(): string;
    abstract asRemote(transaction: Transaction): RemoteThing;
}
export declare namespace ThingImpl {
    function of(thingProto: ConceptProto.Thing): ThingImpl;
}
