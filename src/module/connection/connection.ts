/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { ConnectionParams, ConnectionParamsTranslated, TranslatedAddress } from "../../concept";

const SCHEME = "typedb://";
const TLS_ENABLED = "tlsEnabled";
export const CONNECTION_URI_PLACEHOLDER = connectionUriBasic({ username: "username", password: "password", addresses: ["address"], tlsEnabled: true });

function connectionUriBasic(props: ConnectionParams) {
    const { username, password, addresses, tlsEnabled } = props;
    return `${SCHEME}${username}:${password}@${addresses.join(",")}/?${TLS_ENABLED}=${tlsEnabled}`;
}

function connectionUriTranslated(props: ConnectionParamsTranslated) {
    const { username, password, addresses, tlsEnabled } = props;
    const translatedAddressStrings = addresses.map((x) => `${x.external};${x.internal}`);
    return connectionUriBasic({ username, password, addresses: translatedAddressStrings, tlsEnabled });
}

interface ConnectionUriParsedBase {
    username?: string;
    password?: string;
    tlsEnabled?: boolean;
}

interface ConnectionUriBasicParsed extends ConnectionUriParsedBase {
    addresses: string[];
}

interface ConnectionUriTranslatedParsed extends ConnectionUriParsedBase {
    addresses: TranslatedAddress[];
}

type ConnectionUriParsed = ConnectionUriBasicParsed | ConnectionUriTranslatedParsed;

export function parseConnectionUriOrNull(rawValue: string): ConnectionUriParsed | null {
    if (rawValue.startsWith(SCHEME)) return parseConnectionHostAndPath(rawValue.substring(SCHEME.length));
    else return null;
}

function parseConnectionHostAndPath(rawValue: string): ConnectionUriBasicParsed | ConnectionUriTranslatedParsed | null {
    const [auth, connection] = rawValue.split(`@`, 2) as [string, string?];
    const [username, passwordRaw] = auth.split(`:`, 2) as [string, string?];
    const password = passwordRaw ? decodeURIComponent(passwordRaw) : undefined;
    const [addressesRaw, path] = connection?.split(/(?<![:/])\//, 2) as [string?, string?] ?? undefined;
    const addresses = addressesRaw?.split(`,`) || [];
    const queryParamsPairs = path?.split(`?`)?.at(-1)?.split(`&`).map(x => x.split(`=`, 2) as [string, string?]);
    const queryParams = queryParamsPairs ? Object.fromEntries(queryParamsPairs) as { [key: string]: string | undefined } : undefined;
    const tlsEnabled = queryParams && queryParams[TLS_ENABLED] ? Boolean(queryParams[TLS_ENABLED]) : undefined;

    if (addresses[0]?.includes(`;`)) {
        const translatedAddresses: TranslatedAddress[] = addresses
            .map(x => x.split(`;`, 2).filter(([_, b]) => b != null))
            .map(([a, b]) => ({ external: a, internal: b }));
        return { username, password, addresses: translatedAddresses, tlsEnabled };
    } else {
        return { username, password, addresses, tlsEnabled };
    }
}
