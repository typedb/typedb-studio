/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { BehaviorSubject } from "rxjs";
import { INTERNAL_ERROR } from "./strings";

export function requireValue<T>(behaviorSubject: BehaviorSubject<T | null>, stack: string): T {
    const value = behaviorSubject.value;
    if (!value) throw `${INTERNAL_ERROR}: ${stack}`;
    return value;
}
