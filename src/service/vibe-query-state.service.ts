/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject, Injectable } from "@angular/core";
import { FormBuilder, FormControl } from "@angular/forms";
import { BehaviorSubject, switchMap } from "rxjs";
import { isOkResponse } from "@typedb/driver-http";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { ChatMessage, CloudService } from "./cloud.service";
import { DriverState } from "./driver-state.service";

interface MessagePartText {
    type: 'text';
    content: string;
}

interface MessagePartCode {
    type: 'code';
    language: string;
    formControl: FormControl<string>;
}

type MessagePart = MessagePartText | MessagePartCode;

interface Message {
    content: MessagePart[];
    sender: 'user' | 'ai';
    timestamp: Date;
    isProcessing?: boolean;
    error?: string;
}

class StreamingMarkdownParser {
    private buffer = '';
    private insideCode = false;
    private currentLang = '';

    constructor(private formBuilder: FormBuilder) {}

    newFormControl(value: string): FormControl<string> {
        return this.formBuilder.nonNullable.control(value);
    }

    parseChunk(chunk: string): MessagePart[] {
        this.buffer += chunk;
        const parts: MessagePart[] = [];

        while (true) {
            if (!this.insideCode) {
                const codeStart = this.buffer.indexOf('```');
                if (codeStart === -1) break;

                if (codeStart > 0) {
                    parts.push({ type: 'text', content: this.buffer.slice(0, codeStart) });
                }

                const endOfLine = this.buffer.indexOf('\n', codeStart);
                this.currentLang = endOfLine !== -1 ? this.buffer.slice(codeStart + 3, endOfLine).trim() : '';
                this.buffer = endOfLine !== -1 ? this.buffer.slice(endOfLine + 1) : '';
                this.insideCode = true;
            } else {
                const codeEnd = this.buffer.indexOf('```');
                if (codeEnd === -1) break;

                parts.push({ type: 'code', language: this.currentLang || 'typeql', formControl: this.newFormControl(this.buffer.slice(0, codeEnd)) });
                this.buffer = this.buffer.slice(codeEnd + 3);
                this.insideCode = false;
                this.currentLang = '';
            }
        }

        parts.push({ type: 'text', content: this.buffer });
        return parts;
    }

    flush(): MessagePart[] {
        if (this.buffer.length === 0) return [];
        const segs: MessagePart[] = [];
        if (this.insideCode) {
            segs.push({ type: 'code', language: this.currentLang || 'typeql', formControl: this.newFormControl(this.buffer) });
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
export class VibeQueryState {

    private cloud = inject(CloudService);
    private driver = inject(DriverState);
    private formBuilder = inject(FormBuilder);

    messages$ = new BehaviorSubject<Message[]>([]);
    isProcessing$ = new BehaviorSubject<boolean>(false);
    promptControl = new FormControl("", {nonNullable: true});

    submitPrompt(): void {
        const prompt = this.promptControl.value;
        if (!prompt?.length) return;
        if (this.isProcessing$.value) throw new Error(INTERNAL_ERROR);
        this.isProcessing$.next(true);

        const conversation = [
            ...this.messages$.value.map((msg) => ({
                role: msg.sender === "user" ? "user" : "assistant",
                content: msg.content.map((chunk) => "content" in chunk ? chunk.content : chunk.formControl.value).join("\n\n"),
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

        const parser = new StreamingMarkdownParser(this.formBuilder);

        try {
            this.driver.getDatabaseSchemaText().pipe(
                switchMap((res) => {
                    if (isOkResponse(res)) return this.cloud.aiChat(res.ok, conversation);
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
                    aiMsg.error = err?.err?.message ?? err?.error?.message ?? err?.message ?? err?.toString() ?? INTERNAL_ERROR;
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
            aiMsg.error = err?.err?.message ?? err?.error?.message ?? err?.message ?? err?.toString() ?? INTERNAL_ERROR;
            aiMsg.isProcessing = false;
            aiMsg.timestamp = new Date();
            this.messages$.next(...[this.messages$.value]);
            this.isProcessing$.next(false);
        }
    }
}
