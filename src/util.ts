/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Observable, of, switchMap } from "rxjs";

// eslint-disable-next-line @typescript-eslint/ban-types
export type PropsOf<OBJECT> = Pick<OBJECT, { [KEY in keyof OBJECT]: KEY }[keyof OBJECT]>;

export function renderCentsAsUSD(amount: number, minDecimalPlaces?: number) {
    return (amount / 100).toLocaleString("en-US", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: minDecimalPlaces,
    });
}

export function isBlank(str: string | null | undefined) {
    return str == null || /^\s*$/.test(str);
}

export const toSentenceCase = (phrase: string): string => {
    return phrase
        .split(" ")
        .map((word: string) => {
            if (word.length >= 2)
                return (
                    word[0].toUpperCase() +
                    word.substring(1, undefined).toLowerCase()
                );
            else return word[0].toUpperCase();
        })
        .join(" ");
};

export function bytesToString(bytes: Uint8Array): string {
    return new TextDecoder().decode(bytes);
}

export function stringToBytes(str: string): Uint8Array {
    return new TextEncoder().encode(str);
}

export function stringToBytesOrUndefined(str: string | null | undefined): Uint8Array | undefined {
    return str != null ? stringToBytes(str) : undefined;
}

export function stripUndefinedValues(obj: Record<string, unknown>) {
    return Object.keys(obj).reduce((acc, key) => obj[key] === undefined ? {...acc} : {...acc, [key] : obj[key]} , {})
}

export function cleanInputForID(input: string) {
    return input.toLowerCase().replaceAll(" ", "");
}

export function ensureIdUnique(id: string, checkId: (id: string) => Observable<boolean>): Observable<string> {
    return checkId(id).pipe(
        switchMap((exists): Observable<string> => {
            if (exists) return generateUniqueId(id, 0, checkId);
            else return of(id);
        })
    );
}

function generateUniqueId(base: string, count: number, checkId: (id: string) => Observable<boolean>): Observable<string> {
    const generatedNumber = Math.floor(Math.random() * 9);
    const generatedId = `${base}${generatedNumber}`;
    if (count >= 9) return of(generatedId);
    return checkId(generatedId).pipe(
        switchMap((exists): Observable<string> => {
            if (exists) return generateUniqueId(generatedId, count + 1, checkId);
            else return of(generatedId);
        })
    );
}

const alphanumericCharacters = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';

export function randomId(length: number) {
    return cryptoRandomAlphanumeric(length).toLowerCase()
}

export function cryptoRandomAlphanumeric(length: number) {
    return Array.from(crypto.getRandomValues(new Uint32Array(length)))
        .map((x) => alphanumericCharacters[x % alphanumericCharacters.length])
        .join('')
}
