import { RemoteType, Type, GraknClient, Stream, ThingImpl, ConceptImpl, RemoteConceptImpl, Concept } from "../../../dependencies_internal";
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
import Transaction = GraknClient.Transaction;
export declare abstract class TypeImpl extends ConceptImpl implements Type {
    private readonly _label;
    private readonly _root;
    protected constructor(label: string, root: boolean);
    getLabel(): string;
    isRoot(): boolean;
    isType(): boolean;
    isRemote(): boolean;
    toString(): string;
    equals(concept: Concept): boolean;
    abstract asRemote(transaction: Transaction): RemoteType;
}
export declare abstract class RemoteTypeImpl extends RemoteConceptImpl implements RemoteType {
    private readonly _rpcTransaction;
    private _label;
    private readonly _isRoot;
    protected constructor(transaction: Transaction, label: string, isRoot: boolean);
    getLabel(): string;
    isRoot(): boolean;
    isType(): boolean;
    isRemote(): boolean;
    equals(concept: Concept): boolean;
    setLabel(label: string): Promise<void>;
    isAbstract(): Promise<boolean>;
    protected setSupertype(type: Type): Promise<void>;
    getSupertype(): Promise<TypeImpl>;
    getSupertypes(): Stream<TypeImpl>;
    getSubtypes(): Stream<TypeImpl>;
    delete(): Promise<void>;
    isDeleted(): Promise<boolean>;
    protected get transaction(): Transaction;
    protected typeStream(method: ConceptProto.Type.Req, typeGetter: (res: ConceptProto.Type.Res) => ConceptProto.Type[]): Stream<TypeImpl>;
    protected thingStream(method: ConceptProto.Type.Req, thingGetter: (res: ConceptProto.Type.Res) => ConceptProto.Thing[]): Stream<ThingImpl>;
    protected execute(method: ConceptProto.Type.Req): Promise<ConceptProto.Type.Res>;
    toString(): string;
    abstract asRemote(transaction: Transaction): RemoteTypeImpl;
}
export declare namespace TypeImpl {
    function of(typeProto: ConceptProto.Type): TypeImpl;
}
