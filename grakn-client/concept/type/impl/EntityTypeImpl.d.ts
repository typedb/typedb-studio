import { ThingTypeImpl, RemoteThingTypeImpl, RemoteEntityType, GraknClient, EntityType, EntityImpl, Stream } from "../../../dependencies_internal";
import Transaction = GraknClient.Transaction;
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
export declare class EntityTypeImpl extends ThingTypeImpl implements EntityType {
    protected constructor(label: string, isRoot: boolean);
    static of(typeProto: ConceptProto.Type): EntityTypeImpl;
    asRemote(transaction: Transaction): RemoteEntityType;
    isEntityType(): boolean;
}
export declare class RemoteEntityTypeImpl extends RemoteThingTypeImpl implements RemoteEntityType {
    constructor(transaction: Transaction, label: string, isRoot: boolean);
    isEntityType(): boolean;
    create(): Promise<EntityImpl>;
    getSubtypes(): Stream<EntityTypeImpl>;
    getInstances(): Stream<EntityImpl>;
    setSupertype(type: EntityType): Promise<void>;
    asRemote(transaction: Transaction): RemoteEntityTypeImpl;
}
