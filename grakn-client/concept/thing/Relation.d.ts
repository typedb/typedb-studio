import { Thing, RemoteThing, GraknClient, RelationType, RoleType, Stream } from "../../dependencies_internal";
import Transaction = GraknClient.Transaction;
export interface Relation extends Thing {
    getType(): RelationType;
    asRemote(transaction: Transaction): RemoteRelation;
}
export interface RemoteRelation extends RemoteThing {
    getType(): RelationType;
    asRemote(transaction: Transaction): RemoteRelation;
    addPlayer(roleType: RoleType, player: Thing): Promise<void>;
    removePlayer(roleType: RoleType, player: Thing): Promise<void>;
    getPlayers(): Stream<Thing>;
    getPlayers(roleTypes: RoleType[]): Stream<Thing>;
    getPlayersByRoleType(): Promise<Map<RoleType, Thing[]>>;
}
