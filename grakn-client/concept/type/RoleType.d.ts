import { RelationType, ThingType, GraknClient, Stream, Type, RemoteType } from "../../dependencies_internal";
import Transaction = GraknClient.Transaction;
export interface RoleType extends Type {
    getScope(): string;
    getScopedLabel(): string;
    asRemote(transaction: Transaction): RemoteRoleType;
}
export interface RemoteRoleType extends RemoteType {
    getScope(): string;
    getScopedLabel(): string;
    asRemote(transaction: Transaction): RemoteRoleType;
    getSupertype(): Promise<RoleType>;
    getSupertypes(): Stream<RoleType>;
    getSubtypes(): Stream<RoleType>;
    getRelationType(): Promise<RelationType>;
    getRelationTypes(): Stream<RelationType>;
    getPlayers(): Stream<ThingType>;
}
