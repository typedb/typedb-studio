/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Pipe, PipeTransform } from "@angular/core";
import { formatDate } from "@angular/common";

@Pipe({
    name: "datetime",
    standalone: true,
})
export class DatetimePipe implements PipeTransform {
    transform(value: Date): string {
        return formatDate(value, "d MMM yyyy, HH:mm:ss", "en-US");
    }
}
