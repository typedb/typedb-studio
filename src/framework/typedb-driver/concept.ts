/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

export type TypeKind = "entityType" | "relationType" | "attributeType" | "roleType";

export type ThingKind = "entity" | "relation" | "attribute";

export type ValueKind = "value";

export type ValueType = "boolean" | "integer" | "double" | "decimal" | "date" | "datetime" | "datetime-tz" | "duration" | "string" | "struct";

export interface EntityType {
    kind: "entityType";
    label: string;
}

export interface RelationType {
    kind: "relationType";
    label: string;
}

export interface RoleType {
    kind: "roleType";
    label: string;
}

export type AttributeType = {
    label: string,
    kind: "attributeType";
    valueType: ValueType;
}

export type Type = EntityType | RelationType | RoleType | AttributeType;

export interface Entity {
    kind: "entity";
    iid: string,
    type: EntityType;
}

export interface Relation {
    kind: "relation";
    iid: string;
    type: RelationType;
}

export interface Attribute {
    kind: "attribute";
    iid: string;
    value: any;
    valueType: ValueType;
    type: AttributeType;
}

export interface Value {
    kind: ValueKind;
    value: any;
    valueType: ValueType;
}

export type Concept = Type | Entity | Relation | Attribute | Value;
