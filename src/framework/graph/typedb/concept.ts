////////////////////////////
// Native TypeDB concepts //
////////////////////////////
export enum TypeKind {
    entityType = "entityType",
    relationType = "relationType",
    attributeType = "attributeType",
    roleType = "roleType",
}

export enum ThingKind {
    entity = "entity",
    relation = "relation",
    attribute = "attribute",
}

export type ValueKind = "value";

export enum ValueType {
    boolean = "boolean",
    integer = "integer",
    double = "double",
    decimal = "decimal",
    date = "date",
    dateTime = "datetime",
    dateTimeTZ = "datetime-tz",
    duration = "duration",
    string = "string",
    struct = "struct",
}

//////////////
// Concepts //
//////////////
export type ObjectType = {
    // iid: string,
    kind: TypeKind.entityType | TypeKind.relationType;
    label: string,
}
export type RelationType = ObjectType;
export type EntityType = ObjectType;

export type RoleType = {
    kind: TypeKind.roleType;
    label: string,
}

export type AttributeType = {
    // iid: string,
    label: string,
    kind: TypeKind.attributeType,
    value_type: ValueType,
}

export type TypeAny = ObjectType | RoleType | AttributeType;

export type ObjectAny = {
    kind: ThingKind.entity | ThingKind.relation,
    iid: string,
    type: ObjectType,
}

export type Relation = ObjectAny;
export type Entity = ObjectAny;

export type Attribute = {
    kind: ThingKind.attribute,
    iid: string,
    value: any,
    valueType: ValueType,
    type: AttributeType,
}

export type TypeDBValue = {
    kind: ValueKind,
    value: any,
    valueType: ValueType,
}

export type ConceptAny = TypeAny | ObjectAny | Attribute | TypeDBValue;

//////////////
//   Edges  //
//////////////

export enum EdgeKind {
    isa = "isa",
    has = "has",
    links = "links",

    sub = "sub",
    owns = "owns",
    relates = "relates",
    plays = "plays",

    isaExact = "isaExact",
    subExact = "subExact",

    // Functional
    assigned = "assigned",
    argument = "argument",
}
