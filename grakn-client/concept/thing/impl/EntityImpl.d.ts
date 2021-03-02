import { ThingImpl, RemoteThingImpl, Entity, RemoteEntity, EntityTypeImpl, GraknClient } from "../../../dependencies_internal";
import Transaction = GraknClient.Transaction;
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
export declare class EntityImpl extends ThingImpl implements Entity {
    private readonly _type;
    protected constructor(iid: string, type: EntityTypeImpl);
    static of(protoThing: ConceptProto.Thing): EntityImpl;
    getType(): EntityTypeImpl;
    asRemote(transaction: Transaction): RemoteEntityImpl;
    isEntity(): boolean;
}
export declare class RemoteEntityImpl extends RemoteThingImpl implements RemoteEntity {
    private readonly _type;
    constructor(transaction: Transaction, iid: string, type: EntityTypeImpl);
    asRemote(transaction: Transaction): RemoteEntityImpl;
    getType(): EntityTypeImpl;
    isEntity(): boolean;
}
