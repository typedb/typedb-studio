/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
