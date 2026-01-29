/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Pipe, PipeTransform } from "@angular/core";
import { DriverAction } from "../../concept/action";

@Pipe({
    name: "actionDuration",
    standalone: true,
})
export class ActionDurationPipe implements PipeTransform {
    transform(action: DriverAction): string {
        if (action.completedAtTimestamp == undefined) return ``;
        return `${action.completedAtTimestamp - action.startedAtTimestamp}ms`;
    }
}
