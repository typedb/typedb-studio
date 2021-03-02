import { ThingImpl, RemoteThingImpl, Relation, RemoteRelation, Thing, RelationTypeImpl, RoleType, GraknClient, Stream } from "../../../dependencies_internal";
import Transaction = GraknClient.Transaction;
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
export declare class RelationImpl extends ThingImpl implements Relation {
    private readonly _type;
    protected constructor(iid: string, type: RelationTypeImpl);
    static of(protoThing: ConceptProto.Thing): RelationImpl;
    asRemote(transaction: Transaction): RemoteRelationImpl;
    getType(): RelationTypeImpl;
    isRelation(): boolean;
}
export declare class RemoteRelationImpl extends RemoteThingImpl implements RemoteRelation {
    private readonly _type;
    constructor(transaction: Transaction, iid: string, type: RelationTypeImpl);
    asRemote(transaction: Transaction): RemoteRelationImpl;
    getType(): RelationTypeImpl;
    getPlayersByRoleType(): Promise<Map<RoleType, Thing[]>>;
    getPlayers(roleTypes?: RoleType[]): Stream<ThingImpl>;
    addPlayer(roleType: RoleType, player: Thing): Promise<void>;
    removePlayer(roleType: RoleType, player: Thing): Promise<void>;
    isRelation(): boolean;
}
