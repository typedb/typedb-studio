/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

export interface DriverParamsBasic {
    username: string;
    password: string;
    addresses: string[];
}

export interface DriverParamsTranslated {
    username: string;
    password: string;
    translatedAddresses: TranslatedAddress[];
}

export interface TranslatedAddress {
    external: string;
    internal: string;
}

export type DriverParams = DriverParamsBasic | DriverParamsTranslated;

export function isBasicParams(params: DriverParams): params is DriverParamsBasic {
    return `addresses` in params;
}

export function isTranslatedParams(params: DriverParams): params is DriverParamsTranslated {
    return `translatedAddresses` in params;
}

export function remoteOrigin(params: DriverParams) {
    if (isBasicParams(params)) return `${params.addresses[0]}`;
    else return `${params.translatedAddresses[0].external}`;
}
