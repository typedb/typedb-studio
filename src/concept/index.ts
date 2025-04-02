/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

export type SidebarState = "expanded" | "collapsed";

export interface Connection {
    name: string;
    uri: string;
    params: ConnectionParams | ConnectionParamsTranslated;
    lastInteractedTimestamp: number;
    connectOnAppStartup?: boolean;
}

export interface ConnectionParams {
    username: string;
    password: string;
    addresses: string[];
    tlsEnabled: boolean;
}

export interface ConnectionParamsTranslated {
    username: string;
    password: string;
    addresses: TranslatedAddress[];
    tlsEnabled: boolean;
}

export interface TranslatedAddress {
    external: string;
    internal: string;
}
