/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import {
    Database, DriverParams, DriverParamsBasic, DriverParamsTranslated, isBasicParams, TranslatedAddress
} from "@samuel-butcher-typedb/typedb-http-driver";

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
        const address = isBasicParams(params) ? params.addresses[0] : params.translatedAddresses[0].external;
        const host = address.split("/").at(-1);
        return `${params.username}@${host}`;
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

export type ConnectionParamsBasic = DriverParamsBasic & { database?: string };

export type ConnectionParamsTranslated = DriverParamsTranslated & { database?: string };

export type ConnectionParams = ConnectionParamsBasic | ConnectionParamsTranslated;

const SCHEME = "typedb://";
export const CONNECTION_URL_PLACEHOLDER = connectionUrlBasic({ username: "username", password: "password", addresses: ["address"] });

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

    const [addressesRaw, path] = connection.split(/(?<![:/])\//, 2) as [string?, string?] ?? undefined;
    if (!addressesRaw?.length) return null;
    const addresses = addressesRaw.split(`,`);
    if (!addresses.length) return null;

    const database = path?.length ? path : undefined;

    if (addresses[0].includes(`;`)) {
        const translatedAddressesRaw = addresses.map(x => x.split(`;`, 2));
        if (translatedAddressesRaw.some(x => !x[1])) return null;
        const translatedAddresses: TranslatedAddress[] = translatedAddressesRaw.map(([a, b]) => ({ external: a, internal: b }));
        return { username, password, translatedAddresses, database };
    } else {
        return { username, password, addresses, database };
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
