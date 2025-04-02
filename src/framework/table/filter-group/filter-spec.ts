/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Property } from "../../../concept/base";
import { FilterOperator, RawFilterValue } from "../../../concept/common";

export interface FilterSpec<NATIVE_TYPE extends RawFilterValue = RawFilterValue> {
    property: Property;
    operators: FilterOperator[];
    valueType: "boolean" | "long" | "double" | "string" | "date";
    validValues?: NATIVE_TYPE[];
}

export function booleanFilterSpec(property: Property): FilterSpec<boolean> {
    return {
        property: property,
        operators: ["eq"],
        valueType: "boolean",
        validValues: [false, true],
    };
}

export function longFilterSpec(property: Property): FilterSpec<number> {
    return {
        property: property,
        operators: ["eq", "neq", "lt", "lte", "gt", "gte"],
        valueType: "long",
    };
}

export function doubleFilterSpec(property: Property): FilterSpec<number> {
    return {
        property: property,
        operators: ["eq", "neq", "lt", "lte", "gt", "gte"],
        valueType: "double",
    };
}

export function stringFilterSpec(property: Property, validValues?: string[]): FilterSpec<string> {
    return {
        property: property,
        operators: ["eq", "neq", "lt", "lte", "gt", "gte", "contains", "regex"],
        valueType: "string",
        validValues: validValues,
    };
}

export function dateFilterSpec(property: Property): FilterSpec<Date> {
    return {
        property: property,
        operators: ["eq", "neq", "lt", "lte", "gt", "gte"],
        valueType: "date",
    };
}
