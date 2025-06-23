/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Concept } from "./concept";
import { Database } from "./database";
import { QueryStructure } from "./query-structure";

export interface SignInResponse {
    token: string;
}

export type Distribution = `TypeDB Cluster` | `TypeDB CE`;

export interface VersionResponse {
    distribution: Distribution;
    version: string;
}

export interface DatabasesListResponse {
    databases: Database[];
}

export interface TransactionOpenResponse {
    transactionId: string;
}

export type QueryType = "read" | "write" | "schema";

export type AnswerType = "ok" | "conceptRows" | "conceptDocuments";

export interface ConceptRow {
    [varName: string]: Concept | undefined;
}

export interface ConceptRowAnswer {
    involvedBlocks: number[];
    data: ConceptRow;
}

export type ConceptDocument = Object;

export type Answer = ConceptRowAnswer | ConceptDocument;

export interface QueryResponseBase {
    answerType: AnswerType;
    queryType: QueryType;
    comment: string | null;
    query: QueryStructure | null;
}

export interface OkQueryResponse extends QueryResponseBase {
    answerType: "ok";
}

export interface ConceptRowsQueryResponse extends QueryResponseBase {
    answerType: "conceptRows";
    answers: ConceptRowAnswer[];
}

export interface ConceptDocumentsQueryResponse extends QueryResponseBase {
    answerType: "conceptDocuments";
    answers: ConceptDocument[];
}

export type QueryResponse = OkQueryResponse | ConceptRowsQueryResponse | ConceptDocumentsQueryResponse;

export type ApiOkResponse<OK_RES = {}> = { ok: OK_RES };

export type ApiError = { code: string; message: string };

export interface ApiErrorResponse {
    err: ApiError;
    status: number;
}

export function isApiError(err: any): err is ApiError {
    return typeof err.code === "string" && typeof err.message === "string";
}

export type ApiResponse<OK_RES = {} | null> = ApiOkResponse<OK_RES> | ApiErrorResponse;

export function isOkResponse<OK_RES>(res: ApiResponse<OK_RES>): res is ApiOkResponse<OK_RES> {
    return "ok" in res;
}

export function isApiErrorResponse(res: ApiResponse): res is ApiErrorResponse {
    return "err" in res;
}
