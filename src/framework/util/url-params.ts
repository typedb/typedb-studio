/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { ParamMap } from "@angular/router";

export const USERNAME = "username";
export const ADDRESS = "address";
export const NAME = "name";

/** Server addresses from the page URL's `address` query param(s).
 *  Supports both repeated params (`?address=a&address=b`) and comma-separated
 *  values (`?address=a,b`) — including combinations — for multi-node clusters. */
export function addressesFromParams(params: ParamMap): string[] {
    return params.getAll(ADDRESS)
        .flatMap(value => value.split(","))
        .map(address => address.trim())
        .filter(address => address.length > 0);
}
