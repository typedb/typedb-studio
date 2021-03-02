import { ThingType, RemoteThingType, Relation, RoleType, GraknClient, Stream } from "../../dependencies_internal";
import Transaction = GraknClient.Transaction;
export interface RelationType extends ThingType {
    asRemote(transaction: Transaction): RemoteRelationType;
}
export interface RemoteRelationType extends RemoteThingType {
    asRemote(transaction: Transaction): RemoteRelationType;
    create(): Promise<Relation>;
    getRelates(roleLabel: string): Promise<RoleType>;
    getRelates(): Stream<RoleType>;
    setRelates(roleLabel: string): Promise<void>;
    setRelates(roleLabel: string, overriddenLabel: string): Promise<void>;
    unsetRelates(roleLabel: string): Promise<void>;
    setSupertype(relationType: RelationType): Promise<void>;
    getSubtypes(): Stream<RelationType>;
    getInstances(): Stream<Relation>;
}
