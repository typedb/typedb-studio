/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { parser } from "../codemirror-lang-typeql/generated/typeql.grammar.generated";
import { QueryFileEntry } from "../codemirror-lang-typeql/generated/typeql.grammar.generated.terms";

/**
 * Split a TypeQL document into individual queries using the Lezer grammar.
 *
 * Splits on `end;` separators. String literals (e.g. `"Friend;"`) are handled
 * by the grammar and will never cause a false split.
 *
 * Returns an array of query strings (without the `end;` delimiters).
 * For a single query with no `end;`, returns a single-element array.
 */
export function splitTypeQLQueries(text: string): string[] {
    const tree = parser.parse(text);
    const queries: string[] = [];

    let cursor = tree.topNode.firstChild;
    while (cursor) {
        if (cursor.type.id === QueryFileEntry) {
            const queryText = text.slice(cursor.from, cursor.to).trim();
            if (queryText.length > 0) {
                queries.push(queryText);
            }
        }
        cursor = cursor.nextSibling;
    }

    // Fallback: if parsing produced no entries, treat entire text as one query
    if (queries.length === 0) {
        const trimmed = text.trim();
        if (trimmed.length > 0) return [trimmed];
    }

    return queries;
}
