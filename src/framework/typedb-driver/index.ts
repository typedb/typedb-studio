/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { TransactionType } from "./transaction";
import { DriverParams, remoteOrigin } from "./params";
import { ApiResponse, DatabasesListResponse, isApiError, QueryResponse, SignInResponse, TransactionOpenResponse } from "./response";

const HTTP_UNAUTHORIZED = 401;

export class TypeDBHttpDriver {

    private token?: string;

    constructor(private params: DriverParams) {}

    listDatabases(): Promise<ApiResponse<DatabasesListResponse>> {
        return this.apiGet<DatabasesListResponse>(`/v1/databases`);
    }

    createDatabase(name: string): Promise<ApiResponse> {
        return this.apiPost(`/v1/databases/${name}`, {});
    }

    openTransaction(databaseName: string, type: TransactionType): Promise<ApiResponse<TransactionOpenResponse>> {
        return this.apiPost<TransactionOpenResponse>(`/v1/transactions/open`, { databaseName, transactionType: type });
    }

    commitTransaction(transactionId: string): Promise<ApiResponse> {
        return this.apiPost(`/v1/transactions/${transactionId}/commit`, {});
    }

    closeTransaction(transactionId: string): Promise<ApiResponse> {
        return this.apiPost(`/v1/transactions/${transactionId}/close`, {});
    }

    query(transactionId: string, query: string): Promise<ApiResponse<QueryResponse>> {
        return this.apiPost<QueryResponse>(`/v1/transactions/${transactionId}/query`, { query });
    }

    health(): Promise<ApiResponse> {
        return this.apiGet(`/v1/health`);
    }

    private async apiGet<RES = Object>(path: string, options?: { headers?: Record<string, string> }): Promise<ApiResponse<RES>> {
        const url = `${remoteOrigin(this.params)}${path}`;
        let tokenResp = await this.getToken();
        if ("err" in tokenResp) return tokenResp;
        let headers = Object.assign({ "Authorization": `Bearer ${tokenResp.ok.token}`, "Content-Type": "application/json" }, options?.headers || {});
        let resp = await fetch(url, { method: "GET", headers: headers });
        if (resp.status === HTTP_UNAUTHORIZED) {
            tokenResp = await this.refreshToken();
            if ("err" in tokenResp) return tokenResp;
            headers = Object.assign({ "Authorization": `Bearer ${tokenResp.ok.token}`, "Content-Type": "application/json" }, options?.headers || {});
            resp = await fetch(url, { method: "GET", headers: headers });
        }
        const json = await this.jsonOrNull(resp);
        if (resp.ok) return { ok: json as RES };
        else if (isApiError(json)) return { err: json, status: resp.status };
        else throw resp;
    }

    private async apiPost<RES = Object, BODY = Object>(path: string, body: BODY, options?: { headers?: Record<string, string> }) {
        const url = `${remoteOrigin(this.params)}${path}`;
        let tokenResp = await this.getToken();
        if ("err" in tokenResp) return tokenResp;
        let headers = Object.assign({ "Authorization": `Bearer ${tokenResp.ok.token}`, "Content-Type": "application/json" }, options?.headers || {});
        let resp = await fetch(url, { method: "POST", body: JSON.stringify(body), headers: headers });
        if (resp.status === HTTP_UNAUTHORIZED) {
            tokenResp = await this.refreshToken();
            if ("err" in tokenResp) return tokenResp;
            headers = Object.assign({ "Authorization": `Bearer ${tokenResp.ok.token}`, "Content-Type": "application/json" }, options?.headers || {});
            resp = await fetch(url, { method: "POST", body: JSON.stringify(body), headers: headers });
        }
        const json = await this.jsonOrNull(resp);
        if (resp.ok) return { ok: json as RES };
        else if (isApiError(json)) return { err: json, status: resp.status };
        else throw resp;
    }

    private getToken(): Promise<ApiResponse<SignInResponse>> {
        if (this.token) {
            const resp: ApiResponse<SignInResponse> ={ ok: { token: this.token } };
            return Promise.resolve(resp);
        } else return this.refreshToken();
    }

    private async refreshToken(): Promise<ApiResponse<SignInResponse>> {
        const url = `${remoteOrigin(this.params)}/v1/signin`;
        const body = { username: this.params.username, password: this.params.password };
        const resp = await fetch(url, { method: "POST", body: JSON.stringify(body), headers: { "Content-Type": "application/json" } });
        const json = await this.jsonOrNull(resp);
        if (resp.ok) {
            this.token = (json as SignInResponse).token;
            return { ok: json };
        } else if (isApiError(json)) {
            return { err: json, status: resp.status };
        } else throw resp;
    }

    private async jsonOrNull(resp: Response) {
        const contentLengthRaw = resp.headers.get("Content-Length");
        const contentLength = parseInt(contentLengthRaw || "");
        if (isNaN(contentLength)) throw `Received invalid Content-Length header: ${contentLengthRaw}`;
        return contentLength > 0 ? await resp.json() : null;
    }
}
