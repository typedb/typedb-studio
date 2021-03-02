import { Concept, RemoteConcept, GraknClient } from "../../dependencies_internal";
export declare abstract class ConceptImpl implements Concept {
    abstract asRemote(transaction: GraknClient.Transaction): RemoteConcept;
    isRemote(): boolean;
    isType(): boolean;
    isThingType(): boolean;
    isEntityType(): boolean;
    isAttributeType(): boolean;
    isRelationType(): boolean;
    isRoleType(): boolean;
    isThing(): boolean;
    isEntity(): boolean;
    isAttribute(): boolean;
    isRelation(): boolean;
    abstract equals(concept: Concept): boolean;
}
export declare abstract class RemoteConceptImpl implements RemoteConcept {
    abstract asRemote(transaction: GraknClient.Transaction): RemoteConcept;
    abstract delete(): Promise<void>;
    abstract isDeleted(): Promise<boolean>;
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
    abstract equals(concept: Concept): boolean;
}
