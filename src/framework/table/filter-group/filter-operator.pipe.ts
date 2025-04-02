/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Pipe, PipeTransform } from "@angular/core";
import { FilterOperator, filterOperatorToString } from "../../../concept/common";

@Pipe({
    name: "filterOperator",
    standalone: true,
})
export class FilterOperatorPipe implements PipeTransform {
    transform(value: FilterOperator): string {
        return filterOperatorToString(value);
    }
}
