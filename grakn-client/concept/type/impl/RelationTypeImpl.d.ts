import { ThingTypeImpl, RemoteThingTypeImpl, RelationType, RemoteRelationType, GraknClient, Stream, RelationImpl, RoleTypeImpl } from "../../../dependencies_internal";
import Transaction = GraknClient.Transaction;
import { Type as TypeProto } from "grakn-protocol/protobuf/concept_pb";
export declare class RelationTypeImpl extends ThingTypeImpl implements RelationType {
    protected constructor(label: string, isRoot: boolean);
    static of(typeProto: TypeProto): RelationTypeImpl;
    asRemote(transaction: Transaction): RemoteRelationTypeImpl;
    isRelationType(): boolean;
}
export declare class RemoteRelationTypeImpl extends RemoteThingTypeImpl implements RemoteRelationType {
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    asRemote(transaction: Transaction): RemoteRelationTypeImpl;
    isRelationType(): boolean;
    create(): Promise<RelationImpl>;
    getRelates(roleLabel: string): Promise<RoleTypeImpl>;
    getRelates(): Stream<RoleTypeImpl>;
    setRelates(roleLabel: string, overriddenLabel?: string): Promise<void>;
    unsetRelates(roleLabel: string): Promise<void>;
    setSupertype(relationType: RelationType): Promise<void>;
    getSubtypes(): Stream<RelationTypeImpl>;
    getInstances(): Stream<RelationImpl>;
}
