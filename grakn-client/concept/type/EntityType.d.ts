import { ThingType, RemoteThingType, Entity, GraknClient, Stream } from "../../dependencies_internal";
import Transaction = GraknClient.Transaction;
export interface EntityType extends ThingType {
    asRemote(transaction: Transaction): RemoteEntityType;
}
export interface RemoteEntityType extends RemoteThingType {
    asRemote(transaction: Transaction): RemoteEntityType;
    create(): Promise<Entity>;
    setSupertype(superEntityType: EntityType): Promise<void>;
    getSubtypes(): Stream<EntityType>;
    getInstances(): Stream<Entity>;
}
