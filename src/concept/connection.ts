/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import {
    Database, DriverParams, DriverParamsBasic, DriverParamsTranslated, isBasicParams, TranslatedAddress
} from "@typedb/driver-http";

export class ConnectionConfig {

    readonly name: string;
    readonly params: ConnectionParams;
    readonly url: string;
    readonly preferences: ConnectionPreferences;

    constructor(props: { name?: string, params: ConnectionParams, preferences: ConnectionPreferences }) {
        this.name = props.name ?? ConnectionConfig.autoName(props.params);
        this.params = props.params;
        this.url = connectionUrl(props.params);
        this.preferences = props.preferences;
    }

    static autoName(params: ConnectionParams) {
        if (params.name?.length) return params.name;
        const address = isBasicParams(params) ? params.addresses[0] : params.translatedAddresses[0].external;
        return `${address.split("/").at(-1)}`;
    }

    withDatabase(database: Database | null): ConnectionConfig {
        return new ConnectionConfig({
            name: this.name,
            params: Object.assign<{}, ConnectionParams, Partial<ConnectionParams>>({}, this.params, { database: database?.name }),
            preferences: this.preferences,
        });
    }

    toJSON(): ConnectionJson {
        return { name: this.name, url: this.url, preferences: this.preferences };
    }

    static fromJSONOrNull(json: Partial<ConnectionJson>): ConnectionConfig | null {
        if (!json.name || !json.url || !json.preferences) return null;
        const params = parseConnectionUrlOrNull(json.url) || null;
        if (!params) return null;
        return new ConnectionConfig({
            name: json.name,
            params: params,
            preferences: json.preferences,
        });
    }
}

export interface ConnectionPreferences {
    isStartupConnection: boolean;
}

export type ConnectionParamsBasic = DriverParamsBasic & { database?: string; name?: string };

export type ConnectionParamsTranslated = DriverParamsTranslated & { database?: string; name?: string };

export type ConnectionParams = ConnectionParamsBasic | ConnectionParamsTranslated;

const SCHEME = "typedb://";
export const CONNECTION_STRING_PLACEHOLDER = connectionUrlBasic({ username: "username", password: "password", addresses: ["http://address"] });

export function connectionUrl(props: ConnectionParams) {
    if (`translatedAddresses` in props) return connectionUrlTranslated(props);
    else return connectionUrlBasic(props);
}

function connectionUrlBasic(props: ConnectionParamsBasic) {
    const { username, password, addresses, database } = props;
    return `${SCHEME}${username}:${password}@${addresses.join(",")}/${database ?? ''}`;
}

function connectionUrlTranslated(props: ConnectionParamsTranslated) {
    const { username, password, translatedAddresses, database } = props;
    const translatedAddressStrings = translatedAddresses.map((x) => `${x.external};${x.internal}`);
    return connectionUrlBasic({ username, password, addresses: translatedAddressStrings, database });
}

export function parseConnectionUrlOrNull(rawValue: string): (DriverParams & { database?: string }) | null {
    if (rawValue.startsWith(SCHEME)) return parseConnectionHostAndPathOrNull(rawValue.substring(SCHEME.length));
    else return null;
}

function parseConnectionHostAndPathOrNull(rawValue: string): ConnectionParams | null {
    const [auth, connection] = rawValue.split(`@`, 2) as [string, string?];
    if (!connection?.length) return null;

    const [username, passwordRaw] = auth.split(`:`, 2) as [string, string?];
    if (!passwordRaw?.length) return null;
    const password = decodeURIComponent(passwordRaw);

    // Safari-compatible: find first "/" not part of "://" or "//"
    const protocolMatch = connection.match(/:\/\/|\/\//);
    const searchStart = protocolMatch ? (protocolMatch.index! + protocolMatch[0].length) : 0;
    const slashIndex = connection.indexOf('/', searchStart);
    const [addressesRaw, path] = slashIndex === -1
        ? [connection, undefined] as [string, undefined]
        : [connection.substring(0, slashIndex), connection.substring(slashIndex + 1)] as [string, string];
    if (!addressesRaw?.length) return null;
    const addresses = addressesRaw.split(`,`);
    if (!addresses.length) return null;

    const [pathname, query] = path?.length ? path.split(`?`, 2) : [undefined, undefined];
    const database = pathname?.length ? pathname : undefined;
    const searchParams = query?.length ? new URLSearchParams(query) : undefined;
    const name = searchParams?.get(`name`) ?? undefined;

    if (addresses[0].includes(`;`)) {
        const translatedAddressesRaw = addresses.map(x => x.split(`;`, 2));
        if (translatedAddressesRaw.some(x => !x[1])) return null;
        const translatedAddresses: TranslatedAddress[] = translatedAddressesRaw.map(([a, b]) => ({ external: a, internal: b }));
        return { username, password, translatedAddresses, database, name };
    } else {
        return { username, password, addresses, database, name };
    }
}

export interface ConnectionJson {
    name: string;
    url: string;
    preferences: ConnectionPreferences;
}

export const DEFAULT_DATABASE_NAME = "default";

export function databasesSortedByName(databases: Database[]) {
    return [...databases].sort((a, b) => a.name.localeCompare(b.name));
}
