import { GraknClient } from "../dependencies_internal";
import Transaction = GraknClient.Transaction;
export interface Concept {
    asRemote(transaction: Transaction): RemoteConcept;
    isRemote(): boolean;
    isType(): boolean;
    isRoleType(): boolean;
    isThingType(): boolean;
    isEntityType(): boolean;
    isAttributeType(): boolean;
    isRelationType(): boolean;
    isThing(): boolean;
    isEntity(): boolean;
    isAttribute(): boolean;
    isRelation(): boolean;
    equals(concept: Concept): boolean;
}
export interface RemoteConcept extends Concept {
    delete(): Promise<void>;
    isDeleted(): Promise<boolean>;
}
