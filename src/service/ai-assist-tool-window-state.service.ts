/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { FormControl } from "@angular/forms";
import { BehaviorSubject, switchMap } from "rxjs";
import { isOkResponse } from "typedb-driver-http";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { ChatMessage, CloudService } from "./cloud.service";
import { DriverState } from "./driver-state.service";

/**
 * Represents a message in the AI assistant conversation
 */
interface Message {
    content: MessageChunk[];
    sender: 'user' | 'ai';
    timestamp: Date;
    isProcessing?: boolean;
    error?: string;
}

interface MessageChunk {
    type: 'text' | 'code';
    content: string;
    language?: string;
}

class StreamingMarkdownParser {
    private buffer = '';
    private insideCode = false;
    private currentLang = '';

    parseChunk(chunk: string): MessageChunk[] {
        this.buffer += chunk;
        const segments: MessageChunk[] = [];

        while (true) {
            if (!this.insideCode) {
                const codeStart = this.buffer.indexOf('```');
                if (codeStart === -1) break;

                if (codeStart > 0) {
                    segments.push({ type: 'text', content: this.buffer.slice(0, codeStart) });
                }

                const endOfLine = this.buffer.indexOf('\n', codeStart);
                this.currentLang = endOfLine !== -1 ? this.buffer.slice(codeStart + 3, endOfLine).trim() : '';
                this.buffer = endOfLine !== -1 ? this.buffer.slice(endOfLine + 1) : '';
                this.insideCode = true;
            } else {
                const codeEnd = this.buffer.indexOf('```');
                if (codeEnd === -1) break;

                segments.push({ type: 'code', language: this.currentLang || 'plaintext', content: this.buffer.slice(0, codeEnd) });
                this.buffer = this.buffer.slice(codeEnd + 3);
                this.insideCode = false;
                this.currentLang = '';
            }
        }

        return segments;
    }

    flush(): MessageChunk[] {
        if (this.buffer.length === 0) return [];
        const segs: MessageChunk[] = [];
        if (this.insideCode) {
            segs.push({ type: 'code', language: this.currentLang || 'plaintext', content: this.buffer });
        } else {
            segs.push({ type: 'text', content: this.buffer });
        }
        this.buffer = '';
        return segs;
    }
}

@Injectable({
    providedIn: "root",
})
export class AIAssistToolWindowState {

    messages$ = new BehaviorSubject<Message[]>([]);
    isProcessing$ = new BehaviorSubject<boolean>(false);
    promptControl = new FormControl("", {nonNullable: true});

    constructor(private cloud: CloudService, private driver: DriverState) {
    }

    submitPrompt(): void {
        const prompt = this.promptControl.value;
        if (!prompt?.length) return;
        if (this.isProcessing$.value) throw new Error(INTERNAL_ERROR);
        this.isProcessing$.next(true);

        const conversation = [
            ...this.messages$.value.map((msg) => ({
                role: msg.sender === "user" ? "user" : "assistant",
                content: msg.content,
            })),
            { role: "user", content: prompt },
        ] as ChatMessage[];

        const userMsg: Message = {
            content: [{ type: "text", content: prompt }],
            sender: "user",
            timestamp: new Date()
        };
        const aiMsg: Message = {
            content: [],
            sender: "ai",
            timestamp: new Date(),
            isProcessing: true
        };
        this.messages$.value.push(userMsg, aiMsg);
        this.messages$.next(...[this.messages$.value]);
        this.promptControl.patchValue("");

        const parser = new StreamingMarkdownParser();

        try {
            this.driver.getDatabaseSchemaText().pipe(
                switchMap((res) => {
                    if (isOkResponse(res)) return this.cloud.vibeQuery(res.ok, conversation);
                    else throw res;
                }),
            ).subscribe({
                next: (res) => {
                    const aiMsg = this.messages$.value[this.messages$.value.length - 1];
                    if (aiMsg.sender !== "ai") throw new Error(INTERNAL_ERROR);
                    aiMsg.content = parser.parseChunk(res.response);
                    aiMsg.isProcessing = false;
                    aiMsg.timestamp = new Date();
                    this.messages$.next(...[this.messages$.value]);
                },
                error: (err) => {
                    console.error(err);
                    aiMsg.error = err?.err?.message ?? err?.message ?? err?.toString() ?? INTERNAL_ERROR;
                    aiMsg.isProcessing = false;
                    aiMsg.timestamp = new Date();
                    this.messages$.next(...[this.messages$.value]);
                    this.isProcessing$.next(false);
                },
                complete: () => {
                    this.isProcessing$.next(false);
                },
            });
        } catch (err: any) {
            console.error(err);
            aiMsg.error = err?.err?.message ?? err?.message ?? err?.toString() ?? INTERNAL_ERROR;
            aiMsg.isProcessing = false;
            aiMsg.timestamp = new Date();
            this.messages$.next(...[this.messages$.value]);
            this.isProcessing$.next(false);
        }
    }
}
