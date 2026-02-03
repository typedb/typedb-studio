/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject, Injectable } from "@angular/core";
import { FormBuilder, FormControl } from "@angular/forms";
import { BehaviorSubject, switchMap } from "rxjs";
import { isOkResponse, isApiErrorResponse, ApiResponse, QueryResponse } from "@typedb/driver-http";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { ChatMessage as CloudChatMessage, CloudService } from "./cloud.service";
import { DriverState } from "./driver-state.service";
import { AppData, RowLimit } from "./app-data.service";
import { LogOutputState, TableOutputState, GraphOutputState, RawOutputState, ROW_LIMIT_OPTIONS } from "./query-page-state.service";

interface MessagePartText {
    type: 'text';
    content: string;
}

interface MessagePartCode {
    type: 'code';
    language: string;
    formControl: FormControl<string>;
}

interface MessagePartOutput {
    type: 'output';
    outputState: OutputState;
}

type MessagePart = MessagePartText | MessagePartCode | MessagePartOutput;

export interface OutputState {
    log: LogOutputState;
    table: TableOutputState;
    graph: GraphOutputState;
    raw: RawOutputState;
    outputTypeControl: FormControl<OutputType>;
}

export type OutputType = "log" | "table" | "graph" | "raw";

export interface ChatMessageData {
    id: string;
    content: MessagePart[];
    sender: 'user' | 'ai';
    timestamp: Date;
    isProcessing?: boolean;
    error?: string;
}

export interface PersistedChatMessage {
    id: string;
    sender: 'user' | 'ai';
    timestamp: string;
    content: Array<{
        type: 'text' | 'code';
        content?: string;
        language?: string;
    }>;
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

function createOutputState(formBuilder: FormBuilder): OutputState {
    return {
        log: new LogOutputState(),
        table: new TableOutputState(),
        graph: new GraphOutputState(),
        raw: new RawOutputState(),
        outputTypeControl: formBuilder.nonNullable.control("log" as OutputType),
    };
}

@Injectable({
    providedIn: "root",
})
export class ChatState {

    private cloud = inject(CloudService);
    private driver = inject(DriverState);
    private formBuilder = inject(FormBuilder);
    private appData = inject(AppData);

    messages$ = new BehaviorSubject<ChatMessageData[]>([]);
    isProcessing$ = new BehaviorSubject<boolean>(false);
    promptControl = new FormControl("", { nonNullable: true });
    rowLimitControl = new FormControl(this.appData.preferences.queryRowLimit(), { nonNullable: true });
    rowLimitOptions = ROW_LIMIT_OPTIONS;

    private messageIdCounter = 0;
    private currentDatabaseName: string | null = null;

    constructor() {
        // Subscribe to database changes to load/save conversations
        this.driver.database$.subscribe((database) => {
            // Save current conversation before switching
            if (this.currentDatabaseName) {
                this.persistConversation();
            }

            this.currentDatabaseName = database?.name || null;

            // Load conversation for new database
            if (this.currentDatabaseName) {
                this.restoreFromPersistence();
            } else {
                this.messages$.next([]);
            }
        });
    }

    private generateMessageId(): string {
        return `msg_${Date.now()}_${++this.messageIdCounter}`;
    }

    submitPrompt(): void {
        const prompt = this.promptControl.value;
        if (!prompt?.length) return;
        if (this.isProcessing$.value) throw new Error(INTERNAL_ERROR);
        this.isProcessing$.next(true);

        const conversation = [
            ...this.messages$.value.map((msg) => ({
                role: msg.sender === "user" ? "user" : "assistant",
                content: msg.content
                    .filter((chunk): chunk is MessagePartText | MessagePartCode => chunk.type !== 'output')
                    .map((chunk) => chunk.type === 'text' ? chunk.content : chunk.formControl.value)
                    .join("\n\n"),
            })),
            { role: "user", content: prompt },
        ] as CloudChatMessage[];

        const userMsg: ChatMessageData = {
            id: this.generateMessageId(),
            content: [{ type: "text", content: prompt }],
            sender: "user",
            timestamp: new Date()
        };
        const aiMsg: ChatMessageData = {
            id: this.generateMessageId(),
            content: [],
            sender: "ai",
            timestamp: new Date(),
            isProcessing: true
        };
        this.messages$.value.push(userMsg, aiMsg);
        this.messages$.next([...this.messages$.value]);
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
                    const messages = this.messages$.value;
                    const currentAiMsg = messages[messages.length - 1];
                    if (currentAiMsg.sender !== "ai") throw new Error(INTERNAL_ERROR);
                    currentAiMsg.content = parser.parseChunk(res.response);
                    currentAiMsg.isProcessing = false;
                    currentAiMsg.timestamp = new Date();
                    this.messages$.next([...messages]);
                    this.persistConversation();
                },
                error: (err) => {
                    console.error(err);
                    aiMsg.error = err?.err?.message ?? err?.error?.message ?? err?.message ?? err?.toString() ?? INTERNAL_ERROR;
                    aiMsg.isProcessing = false;
                    aiMsg.timestamp = new Date();
                    this.messages$.next([...this.messages$.value]);
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
            this.messages$.next([...this.messages$.value]);
            this.isProcessing$.next(false);
        }
    }

    executeQueryInMessage(messageId: string, codeBlockIndex: number, query: string): void {
        const messages = this.messages$.value;
        const message = messages.find(m => m.id === messageId);
        if (!message) return;

        const codeBlock = message.content[codeBlockIndex];
        if (codeBlock.type !== 'code') return;

        // Check if there's already an output block after this code block
        const nextPart = message.content[codeBlockIndex + 1];
        let outputState: OutputState;

        if (nextPart && nextPart.type === 'output') {
            // Reuse existing output state
            outputState = nextPart.outputState;
            outputState.log.clear();
            outputState.table.clear();
            outputState.graph.destroy();
            outputState.raw.clear();
        } else {
            // Create new output state
            outputState = createOutputState(this.formBuilder);

            // Insert output block after code block
            const outputPart: MessagePartOutput = {
                type: 'output',
                outputState,
            };
            message.content.splice(codeBlockIndex + 1, 0, outputPart);
        }

        // Initialize output
        outputState.log.appendLines(`## Running> `, query, ``, `## Timestamp> ${new Date().toISOString()}`);
        outputState.table.status = "running";
        outputState.graph.status = "running";
        outputState.graph.query = query;
        outputState.graph.database = this.driver.requireDatabase().name;

        this.messages$.next([...messages]);

        // Execute query
        const rowLimit = this.rowLimitControl.value;
        const queryOptions = rowLimit !== "none" ? { answerCountLimit: rowLimit } : undefined;

        this.driver.query(query, queryOptions).subscribe({
            next: (res) => {
                this.outputQueryResponse(outputState, res);
                this.messages$.next([...this.messages$.value]);
            },
            error: (err) => {
                let errorMsg = ``;
                if (isApiErrorResponse(err)) {
                    errorMsg = err.err.message;
                } else {
                    errorMsg = err?.message ?? err?.toString() ?? `Unknown error`;
                }
                outputState.log.appendLines(``, `## Result> Error`, ``, errorMsg);
                outputState.table.status = "error";
                outputState.graph.status = "error";
                this.messages$.next([...this.messages$.value]);
            },
        });
    }

    private outputQueryResponse(outputState: OutputState, res: ApiResponse<QueryResponse>): void {
        outputState.log.appendBlankLine();
        outputState.log.appendQueryResult(res);
        outputState.table.push(res);
        outputState.graph.push(res);
        outputState.raw.push(JSON.stringify(res, null, 2));
    }

    compactConversation(): void {
        const messages = this.messages$.value;
        if (messages.length <= 4) return;

        // Remove output blocks from all messages except the last 4
        const compacted = messages.map((msg, idx) => {
            if (idx >= messages.length - 4) return msg;
            return {
                ...msg,
                content: msg.content.filter(part => part.type !== 'output')
            };
        });

        this.messages$.next(compacted);
        this.persistConversation();
    }

    clearConversation(): void {
        // Destroy any graph visualizers
        for (const msg of this.messages$.value) {
            for (const part of msg.content) {
                if (part.type === 'output') {
                    part.outputState.graph.destroy();
                }
            }
        }

        this.messages$.next([]);
        this.persistConversation();
    }

    private persistConversation(): void {
        if (!this.currentDatabaseName) return;

        const serialized = this.serializeMessages(this.messages$.value);
        this.appData.chatConversations.setConversation(this.currentDatabaseName, serialized);
    }

    private restoreFromPersistence(): void {
        if (!this.currentDatabaseName) return;

        const saved = this.appData.chatConversations.getConversation(this.currentDatabaseName);
        if (saved && saved.length > 0) {
            this.messages$.next(this.deserializeMessages(saved));
        } else {
            this.messages$.next([]);
        }
    }

    private serializeMessages(messages: ChatMessageData[]): PersistedChatMessage[] {
        return messages.map(msg => ({
            id: msg.id,
            sender: msg.sender,
            timestamp: msg.timestamp.toISOString(),
            content: msg.content
                .filter((part): part is MessagePartText | MessagePartCode => part.type !== 'output')
                .map(part => ({
                    type: part.type,
                    content: part.type === 'code'
                        ? part.formControl.value
                        : part.content,
                    language: part.type === 'code' ? part.language : undefined,
                })),
        }));
    }

    private deserializeMessages(persisted: PersistedChatMessage[]): ChatMessageData[] {
        return persisted.map(msg => ({
            id: msg.id,
            sender: msg.sender,
            timestamp: new Date(msg.timestamp),
            content: msg.content.map(part => {
                if (part.type === 'code') {
                    return {
                        type: 'code' as const,
                        language: part.language || 'typeql',
                        formControl: this.formBuilder.nonNullable.control(part.content || ''),
                    };
                }
                return {
                    type: 'text' as const,
                    content: part.content || '',
                };
            }),
        }));
    }
}
