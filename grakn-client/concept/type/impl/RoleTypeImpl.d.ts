import { ThingTypeImpl, RoleType, RemoteRoleType, GraknClient, RelationTypeImpl, Stream, TypeImpl, RemoteTypeImpl } from "../../../dependencies_internal";
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
import Transaction = GraknClient.Transaction;
export declare class RoleTypeImpl extends TypeImpl implements RoleType {
    private readonly _scope;
    protected constructor(label: string, scope: string, isRoot: boolean);
    static of(typeProto: ConceptProto.Type): RoleTypeImpl;
    getScope(): string;
    getScopedLabel(): string;
    asRemote(transaction: Transaction): RemoteRoleTypeImpl;
    toString(): string;
    isRoleType(): boolean;
}
export declare class RemoteRoleTypeImpl extends RemoteTypeImpl implements RemoteRoleType {
    private readonly _scope;
    constructor(transaction: Transaction, label: string, scope: string, isRoot: boolean);
    getScope(): string;
    getScopedLabel(): string;
    getSupertype(): Promise<RoleTypeImpl>;
    getSupertypes(): Stream<RoleTypeImpl>;
    getSubtypes(): Stream<RoleTypeImpl>;
    asRemote(transaction: Transaction): RemoteRoleTypeImpl;
    getRelationType(): Promise<RelationTypeImpl>;
    getRelationTypes(): Stream<RelationTypeImpl>;
    getPlayers(): Stream<ThingTypeImpl>;
    protected typeStream(method: ConceptProto.Type.Req, typeGetter: (res: ConceptProto.Type.Res) => ConceptProto.Type[]): Stream<TypeImpl>;
    protected execute(method: ConceptProto.Type.Req): Promise<ConceptProto.Type.Res>;
    toString(): string;
    isRoleType(): boolean;
}
