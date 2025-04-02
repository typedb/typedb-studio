/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
