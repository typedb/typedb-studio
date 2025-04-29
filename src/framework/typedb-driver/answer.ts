/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Concept } from "./concept";

export interface ConceptRow {
    [varName: string]: Concept | undefined;
}

export interface ConceptRowAnswer {
    data: ConceptRow;
}

export type ConceptDocument = Object;

export type Answer = ConceptRowAnswer | ConceptDocument;
