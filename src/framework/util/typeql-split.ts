/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { TransactionType } from "@typedb/driver-http";
import { parser } from "../codemirror-lang-typeql/generated/typeql.grammar.generated";
import {
    QueryFileEntry, QuerySchema, ClauseInsert, ClausePut, ClauseUpdate, ClauseDelete,
} from "../codemirror-lang-typeql/generated/typeql.grammar.generated.terms";

/**
 * Split a TypeQL document into individual queries using the Lezer grammar.
 *
 * Splits on `END SEMICOLON` token pairs in the parsed tree. If the grammar
 * falsely splits (e.g. `end-date` tokenized as `END` + `-date`), the resulting
 * fragment won't end with `;` and is rejoined to the next query.
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

const WRITE_CLAUSE_IDS = new Set([ClauseInsert, ClausePut, ClauseUpdate, ClauseDelete]);

/**
 * Detect the required transaction type for a TypeQL query using the Lezer grammar.
 *
 * - Returns "schema" if the query is a define/undefine/redefine.
 * - Returns "write" if the query contains insert/put/update/delete clauses.
 * - Returns "read" otherwise.
 * - Returns null if parsing fails to produce a recognisable structure.
 */
export function detectTransactionType(query: string): TransactionType | null {
    const tree = parser.parse(query);
    const entry = tree.topNode.firstChild;
    if (!entry || entry.type.id !== QueryFileEntry) return null;

    const child = entry.firstChild;
    if (!child) return null;

    if (child.type.id === QuerySchema) return "schema";

    // Walk descendants to find write clauses
    const cursor = child.cursor();
    do {
        if (WRITE_CLAUSE_IDS.has(cursor.type.id)) return "write";
    } while (cursor.next());

    return "read";
}

