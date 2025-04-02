/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Pipe, PipeTransform } from "@angular/core";
import { RawFilterValue } from "../../../concept/common";
import { DatetimePipe } from "../../util";

@Pipe({
    name: "filterValue",
    standalone: true,
})
export class FilterValuePipe implements PipeTransform {
    transform(value: RawFilterValue): string {
        if (typeof value === "boolean") return value ? "True" : "False";
        else if (typeof value === "number") return value.toString();
        else if (typeof value === "string") return value;
        else return new DatetimePipe().transform(value);
    }
}
