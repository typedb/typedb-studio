import { TypeImpl, RemoteTypeImpl, ThingType, RemoteThingType, AttributeType, RoleType, GraknClient, Stream, ThingImpl, RoleTypeImpl, AttributeTypeImpl } from "../../../dependencies_internal";
import Transaction = GraknClient.Transaction;
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
export declare class ThingTypeImpl extends TypeImpl implements ThingType {
    constructor(label: string, isRoot: boolean);
    asRemote(transaction: Transaction): RemoteThingType;
    isThingType(): boolean;
}
export declare class RemoteThingTypeImpl extends RemoteTypeImpl implements RemoteThingType {
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    isThingType(): boolean;
    protected setSupertype(thingType: ThingType): Promise<void>;
    getSupertype(): Promise<ThingTypeImpl>;
    getSupertypes(): Stream<ThingTypeImpl>;
    getSubtypes(): Stream<ThingTypeImpl>;
    getInstances(): Stream<ThingImpl>;
    setAbstract(): Promise<void>;
    unsetAbstract(): Promise<void>;
    setPlays(role: RoleType, overriddenType?: RoleType): Promise<void>;
    setOwns(attributeType: AttributeType): Promise<void>;
    setOwns(attributeType: AttributeType, isKey: boolean): Promise<void>;
    setOwns(attributeType: AttributeType, overriddenType: AttributeType): Promise<void>;
    setOwns(attributeType: AttributeType, overriddenType: AttributeType, isKey: boolean): Promise<void>;
    getPlays(): Stream<RoleTypeImpl>;
    getOwns(): Stream<AttributeTypeImpl>;
    getOwns(valueType: AttributeType.ValueType): Stream<AttributeTypeImpl>;
    getOwns(keysOnly: boolean): Stream<AttributeTypeImpl>;
    getOwns(valueType: AttributeType.ValueType, keysOnly: boolean): Stream<AttributeTypeImpl>;
    unsetPlays(role: RoleType): Promise<void>;
    unsetOwns(attributeType: AttributeType): Promise<void>;
    asRemote(transaction: Transaction): RemoteThingTypeImpl;
}
export declare namespace ThingTypeImpl {
    function of(typeProto: ConceptProto.Type): ThingTypeImpl;
}
