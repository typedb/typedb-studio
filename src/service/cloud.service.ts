/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../environments/environment";
import { HttpClient } from "@angular/common/http";

export interface ChatMessage {
    role: ChatRole;
    content: string;
}

export type ChatRole = "user" | "assistant" | "system";

export interface AIChatResponse {
    response: string;
}

export interface AICompactRequest {
    schema: string;
    conversation: ChatMessage[];
}

export interface AICompactResponse {
    compactedConversation: ChatMessage[];
}

@Injectable({
    providedIn: "root",
})
export class CloudService {
    constructor(private http: HttpClient) {}

    aiChat(schema: string, conversation: ChatMessage[]): Observable<AIChatResponse> {
        return this.http.post<AIChatResponse>(`${environment.cloudUrl}/ai/chat`, { schema, conversation });
    }

    aiCompact(request: AICompactRequest): Observable<AICompactResponse> {
        return this.http.post<AICompactResponse>(`${environment.cloudUrl}/ai/compact`, request);
    }
}
