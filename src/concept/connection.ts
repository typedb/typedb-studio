/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
        return `${params.username}@${address}`;
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

export interface ConnectionParamsBasic {
    username: string;
    password: string;
    addresses: string[];
    tlsEnabled: boolean;
}

export interface ConnectionParamsTranslated {
    username: string;
    password: string;
    translatedAddresses: TranslatedAddress[];
    tlsEnabled: boolean;
}

export interface TranslatedAddress {
    external: string;
    internal: string;
}

export type ConnectionParams = ConnectionParamsBasic | ConnectionParamsTranslated;

export function isBasicParams(params: ConnectionParams): params is ConnectionParamsBasic {
    return `addresses` in params;
}

export function isTranslatedParams(params: ConnectionParams): params is ConnectionParamsTranslated {
    return `translatedAddresses` in params;
}

export function remoteOrigin(params: ConnectionParams) {
    if (isBasicParams(params)) return `http://${params.addresses[0]}`;
    else return `http://${params.translatedAddresses[0].external}`;
}

export interface ConnectionPreferences {
    autoReconnectOnAppStartup: boolean;
}

const SCHEME = "typedb://";
const TLS_DISABLED = "tlsDisabled";
export const CONNECTION_URL_PLACEHOLDER = connectionUrlBasic({ username: "username", password: "password", addresses: ["address"], tlsEnabled: true });

function connectionUrl(props: ConnectionParamsBasic | ConnectionParamsTranslated) {
    if (`translatedAddresses` in props) return connectionUrlTranslated(props);
    else return connectionUrlBasic(props);
}

function connectionUrlBasic(props: ConnectionParamsBasic) {
    const { username, password, addresses, tlsEnabled } = props;
    return `${SCHEME}${username}:${password}@${addresses.join(",")}${tlsEnabled ? `` : `/?${TLS_DISABLED}=true`}`;
}

function connectionUrlTranslated(props: ConnectionParamsTranslated) {
    const { username, password, translatedAddresses, tlsEnabled } = props;
    const translatedAddressStrings = translatedAddresses.map((x) => `${x.external};${x.internal}`);
    return connectionUrlBasic({ username, password, addresses: translatedAddressStrings, tlsEnabled });
}

export function parseConnectionUrlOrNull(rawValue: string): ConnectionParams | null {
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

    const queryParamsPairs = path?.split(`?`)?.at(-1)?.split(`&`).map(x => x.split(`=`, 2) as [string, string?]);
    if (queryParamsPairs?.some(([_, x]) => x == undefined)) return null;
    const queryParams = queryParamsPairs ? Object.fromEntries(queryParamsPairs) as { [key: string]: string | undefined } : undefined;
    const tlsEnabled = queryParams && queryParams[TLS_DISABLED] ? !Boolean(queryParams[TLS_DISABLED]) : true;

    if (addresses[0].includes(`;`)) {
        const translatedAddressesRaw = addresses.map(x => x.split(`;`, 2));
        if (translatedAddressesRaw.some(x => !x[1])) return null;
        const translatedAddresses: TranslatedAddress[] = translatedAddressesRaw.map(([a, b]) => ({ external: a, internal: b }));
        return { username, password, translatedAddresses, tlsEnabled };
    } else {
        return { username, password, addresses, tlsEnabled };
    }
}

export interface ConnectionJson {
    name: string;
    url: string;
    preferences: ConnectionPreferences;
}

export type ConnectionStatus = "connected" | "connecting" | "disconnected";
