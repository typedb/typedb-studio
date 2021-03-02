import { Concept, RemoteConcept, GraknClient, Stream } from "../../dependencies_internal";
import Transaction = GraknClient.Transaction;
export interface Type extends Concept {
    getLabel(): string;
    isRoot(): boolean;
    asRemote(transaction: Transaction): RemoteType;
}
export interface RemoteType extends RemoteConcept {
    getLabel(): string;
    isRoot(): boolean;
    asRemote(transaction: Transaction): RemoteType;
    setLabel(label: string): Promise<void>;
    isAbstract(): Promise<boolean>;
    getSupertype(): Promise<Type>;
    getSupertypes(): Stream<Type>;
    getSubtypes(): Stream<Type>;
}
