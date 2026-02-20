/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable, NgZone } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../environments/environment";


export interface ChatMessage {
    role: ChatRole;
    content: string;
}

export type ChatRole = "user" | "assistant" | "system";

export interface AICompactRequest {
    schema: string;
    conversation: ChatMessage[];
}


@Injectable({
    providedIn: "root",
})
export class CloudService {
    constructor(private zone: NgZone) {}

    aiChat(schema: string, conversation: ChatMessage[]): Observable<string> {
        return new Observable<string>(subscriber => {
            const controller = new AbortController();

            this.zone.runOutsideAngular(() => {
                fetch(`${environment.cloudUrl}/ai/v1/chat`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/event-stream, application/json',
                    },
                    body: JSON.stringify({ schema, conversation }),
                    signal: controller.signal,
                }).then(async response => {
                    if (!response.ok) {
                        const error = new Error(await this.extractErrorBody(response));
                        (error as any).status = response.status;
                        throw error;
                    }

                    const reader = response.body!.getReader();
                    const decoder = new TextDecoder();
                    let buffer = '';

                    while (true) {
                        const { done, value } = await reader.read();
                        if (done) break;

                        buffer += decoder.decode(value, { stream: true });
                        const events = buffer.split('\n\n');
                        buffer = events.pop()!;

                        for (const event of events) {
                            const data = this.parseEventData(event);
                            if (data != null && data !== '[DONE]') {
                                this.zone.run(() => subscriber.next(data));
                            }
                        }
                    }

                    // Process any remaining buffer
                    if (buffer.trim()) {
                        const data = this.parseEventData(buffer);
                        if (data != null && data !== '[DONE]') {
                            this.zone.run(() => subscriber.next(data));
                        }
                    }

                    this.zone.run(() => subscriber.complete());
                }).catch(err => {
                    if (err.name !== 'AbortError') {
                        this.zone.run(() => subscriber.error(err));
                    }
                });
            });

            return () => controller.abort();
        });
    }

    private async extractErrorBody(response: Response): Promise<string> {
        const body = await response.text();
        if (!body) return `HTTP ${response.status}`;
        try {
            const json = JSON.parse(body);
            if (json.message) return json.message;
        } catch {}
        return body;
    }

    private parseEventData(event: string): string | null {
        const parts: string[] = [];
        for (const line of event.split('\n')) {
            if (line.startsWith('data:')) {
                const raw = line[5] === ' ' ? line.slice(6) : line.slice(5);
                if (raw === '[DONE]') return null;
                try {
                    parts.push(JSON.parse(raw));
                } catch {
                    parts.push(raw);
                }
            }
        }
        return parts.length > 0 ? parts.join('') : null;
    }

    aiCompact(request: AICompactRequest): Observable<string> {
        return new Observable<string>(subscriber => {
            const controller = new AbortController();

            this.zone.runOutsideAngular(() => {
                fetch(`${environment.cloudUrl}/ai/v1/chat/compact`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/event-stream, application/json',
                    },
                    body: JSON.stringify(request),
                    signal: controller.signal,
                }).then(async response => {
                    if (!response.ok) {
                        throw new Error(await this.extractErrorBody(response));
                    }

                    const reader = response.body!.getReader();
                    const decoder = new TextDecoder();
                    let buffer = '';

                    while (true) {
                        const { done, value } = await reader.read();
                        if (done) break;

                        buffer += decoder.decode(value, { stream: true });
                        const events = buffer.split('\n\n');
                        buffer = events.pop()!;

                        for (const event of events) {
                            const data = this.parseEventData(event);
                            if (data != null && data !== '[DONE]') {
                                this.zone.run(() => subscriber.next(data));
                            }
                        }
                    }

                    if (buffer.trim()) {
                        const data = this.parseEventData(buffer);
                        if (data != null && data !== '[DONE]') {
                            this.zone.run(() => subscriber.next(data));
                        }
                    }

                    this.zone.run(() => subscriber.complete());
                }).catch(err => {
                    if (err.name !== 'AbortError') {
                        this.zone.run(() => subscriber.error(err));
                    }
                });
            });

            return () => controller.abort();
        });
    }
}
