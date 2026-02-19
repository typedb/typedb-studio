/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject, Injectable } from "@angular/core";
import { FormBuilder, FormControl } from "@angular/forms";
import { BehaviorSubject, Subject, switchMap } from "rxjs";
import { isOkResponse, isApiErrorResponse, ApiResponse, QueryResponse } from "@typedb/driver-http";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { ChatMessage as CloudChatMessage, CloudService } from "./cloud.service";
import { DriverState } from "./driver-state.service";
import { AppData, PersistedChatMessage, PersistedConversation, RowLimit } from "./app-data.service";
import { ROW_LIMIT_OPTIONS, RunOutputState, createRunOutputState } from "./query-page-state.service";

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
    runs: RunOutputState[];
    selectedRunIndex: number;
    runCounter: number;
    outputTypeControl: FormControl<OutputType>;
}

function currentChatRun(state: OutputState): RunOutputState | null {
    if (state.selectedRunIndex < 0 || state.selectedRunIndex >= state.runs.length) return null;
    return state.runs[state.selectedRunIndex];
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

export interface ConversationSummary {
    id: string;
    title: string;
    messageCount: number;
    createdAt: Date;
    updatedAt: Date;
}

class StreamingMarkdownParser {
    private buffer = '';
    private insideCode = false;
    private currentLang = '';
    private completedParts: MessagePart[] = [];

    constructor(private formBuilder: FormBuilder) {}

    newFormControl(value: string): FormControl<string> {
        return this.formBuilder.nonNullable.control(value);
    }

    parseChunk(chunk: string): MessagePart[] {
        this.buffer += chunk;

        while (true) {
            if (!this.insideCode) {
                const codeStart = this.buffer.indexOf('```');
                if (codeStart === -1) break;

                const endOfLine = this.buffer.indexOf('\n', codeStart);
                if (endOfLine === -1) {
                    // Fence found but no newline yet — flush preceding text and wait
                    if (codeStart > 0) {
                        this.completedParts.push({ type: 'text', content: this.buffer.slice(0, codeStart) });
                        this.buffer = this.buffer.slice(codeStart);
                    }
                    break;
                }

                if (codeStart > 0) {
                    this.completedParts.push({ type: 'text', content: this.buffer.slice(0, codeStart) });
                }

                this.currentLang = this.buffer.slice(codeStart + 3, endOfLine).trim();
                this.buffer = this.buffer.slice(endOfLine + 1);
                this.insideCode = true;
            } else {
                const codeEnd = this.buffer.indexOf('```');
                if (codeEnd === -1) break;

                this.completedParts.push({ type: 'code', language: this.currentLang || 'typeql', formControl: this.newFormControl(this.buffer.slice(0, codeEnd)) });
                this.buffer = this.buffer.slice(codeEnd + 3);
                this.insideCode = false;
                this.currentLang = '';
            }
        }

        if (this.insideCode) {
            return [...this.completedParts, { type: 'code', language: this.currentLang || 'typeql', formControl: this.newFormControl(this.buffer) }];
        }
        return [...this.completedParts, { type: 'text', content: this.buffer }];
    }

    flush(): MessagePart[] {
        if (this.buffer.length === 0) return [...this.completedParts];
        if (this.insideCode) {
            this.completedParts.push({ type: 'code', language: this.currentLang || 'typeql', formControl: this.newFormControl(this.buffer) });
        } else {
            this.completedParts.push({ type: 'text', content: this.buffer });
        }
        this.buffer = '';
        return [...this.completedParts];
    }
}

function createOutputState(formBuilder: FormBuilder): OutputState {
    return {
        runs: [],
        selectedRunIndex: -1,
        runCounter: 0,
        outputTypeControl: formBuilder.nonNullable.control("log" as OutputType),
    };
}

function truncateTitle(text: string, maxLen = 50): string {
    const collapsed = text.replace(/\s+/g, ' ').trim();
    if (collapsed.length <= maxLen) return collapsed || 'New conversation';
    return collapsed.substring(0, maxLen - 3) + '...';
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
    aiResponseStarted$ = new Subject<void>();
    conversations$ = new BehaviorSubject<ConversationSummary[]>([]);
    selectedConversationId$ = new BehaviorSubject<string | null>(null);
    promptControl = new FormControl("", { nonNullable: true });
    rowLimitControl = new FormControl(this.appData.preferences.queryRowLimit(), { nonNullable: true });
    rowLimitOptions = ROW_LIMIT_OPTIONS;
    pendingMessage: string | null = null;
    pendingTitle: string | null = null;

    private messageIdCounter = 0;
    private currentDatabaseName: string | null = null;

    constructor() {
        // Subscribe to database changes to load/save conversations
        this.driver.database$.subscribe((database) => {
            // Save current conversation before switching
            if (this.currentDatabaseName && this.selectedConversationId$.value) {
                this.persistConversation();
            }

            this.currentDatabaseName = database?.name || null;

            if (this.currentDatabaseName) {
                this.refreshConversationList();
                const selectedId = this.appData.chatConversations.getSelectedConversationId(this.currentDatabaseName);
                if (selectedId) {
                    this.selectConversation(selectedId);
                } else {
                    this.selectedConversationId$.next(null);
                    this.messages$.next([]);
                }
            } else {
                this.conversations$.next([]);
                this.selectedConversationId$.next(null);
                this.messages$.next([]);
            }
        });
    }

    private generateMessageId(): string {
        return `msg_${Date.now()}_${++this.messageIdCounter}`;
    }

    private refreshConversationList(): void {
        if (!this.currentDatabaseName) return;
        const persisted = this.appData.chatConversations.getConversationList(this.currentDatabaseName);
        this.conversations$.next(persisted.map(c => ({
            id: c.id,
            title: c.title,
            messageCount: c.messages.length,
            createdAt: new Date(c.createdAt),
            updatedAt: new Date(c.updatedAt),
        })));
    }

    newConversation(): void {
        if (!this.currentDatabaseName) return;

        // Persist current conversation first (only if it has messages)
        if (this.selectedConversationId$.value && this.messages$.value.length > 0) {
            this.persistConversation();
        }

        this.destroyGraphVisualizers();

        // Don't persist the new conversation yet — it will be persisted
        // when the first message is sent (in submitPrompt/persistConversation)
        const newId = crypto.randomUUID();
        this.appData.chatConversations.setSelectedConversationId(this.currentDatabaseName, newId);
        this.selectedConversationId$.next(newId);
        this.messages$.next([]);
    }

    selectConversation(conversationId: string): void {
        if (!this.currentDatabaseName) return;

        // Persist current conversation first
        if (this.selectedConversationId$.value && this.selectedConversationId$.value !== conversationId) {
            this.persistConversation();
        }

        this.destroyGraphVisualizers();

        this.selectedConversationId$.next(conversationId);
        this.appData.chatConversations.setSelectedConversationId(this.currentDatabaseName, conversationId);
        this.restoreFromPersistence();
    }

    deleteConversation(conversationId: string): void {
        if (!this.currentDatabaseName) return;

        if (this.selectedConversationId$.value === conversationId) {
            this.destroyGraphVisualizers();
        }

        this.appData.chatConversations.deleteConversation(this.currentDatabaseName, conversationId);
        this.refreshConversationList();

        if (this.selectedConversationId$.value === conversationId) {
            const remaining = this.conversations$.value;
            if (remaining.length > 0) {
                this.selectConversation(remaining[0].id);
            } else {
                this.selectedConversationId$.next(null);
                this.messages$.next([]);
            }
        }
    }

    renameConversation(conversationId: string, title: string): void {
        if (!this.currentDatabaseName) return;
        this.appData.chatConversations.renameConversation(this.currentDatabaseName, conversationId, title);
        this.refreshConversationList();
    }

    private destroyGraphVisualizers(): void {
        for (const msg of this.messages$.value) {
            for (const part of msg.content) {
                if (part.type === 'output') {
                    for (const run of part.outputState.runs) {
                        run.graph.destroy();
                    }
                }
            }
        }
    }

    submitPrompt(): void {
        const prompt = this.promptControl.value;
        if (!prompt?.length) return;
        if (this.isProcessing$.value) throw new Error(INTERNAL_ERROR);

        // Auto-create a conversation if none exists
        if (!this.selectedConversationId$.value && this.currentDatabaseName) {
            this.newConversation();
        }

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
                next: (chunk) => {
                    const messages = this.messages$.value;
                    const currentAiMsg = messages[messages.length - 1];
                    if (currentAiMsg.sender !== "ai") throw new Error(INTERNAL_ERROR);
                    const isFirstChunk = currentAiMsg.isProcessing;
                    currentAiMsg.content = parser.parseChunk(chunk);
                    if (isFirstChunk) {
                        currentAiMsg.isProcessing = false;
                        this.aiResponseStarted$.next();
                    }
                    this.messages$.next([...messages]);
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
                    const messages = this.messages$.value;
                    const currentAiMsg = messages[messages.length - 1];
                    currentAiMsg.content = parser.flush();
                    currentAiMsg.timestamp = new Date();
                    this.messages$.next([...messages]);
                    this.persistConversation();
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
            outputState = nextPart.outputState;
        } else {
            // Create new output state container
            outputState = createOutputState(this.formBuilder);

            // Insert output block after code block
            const outputPart: MessagePartOutput = {
                type: 'output',
                outputState,
            };
            message.content.splice(codeBlockIndex + 1, 0, outputPart);
        }

        // Detach current run's graph before creating new run
        const oldRun = currentChatRun(outputState);
        if (oldRun) oldRun.graph.detach();

        // Create new run
        outputState.runCounter++;
        const newRun = createRunOutputState(`Run ${outputState.runCounter}`, query);
        outputState.runs.push(newRun);
        outputState.selectedRunIndex = outputState.runs.length - 1;

        // Initialize the new run's outputs
        newRun.log.appendLines(`## Running> `, query, ``, `## Timestamp> ${new Date().toISOString()}`);
        newRun.table.status = "running";
        newRun.graph.status = "running";
        newRun.graph.query = query;
        newRun.graph.database = this.driver.requireDatabase().name;

        this.messages$.next([...messages]);

        // Execute query
        const rowLimit = this.rowLimitControl.value;
        const queryOptions = rowLimit !== "none" ? { answerCountLimit: rowLimit } : undefined;

        this.driver.query(query, queryOptions).subscribe({
            next: (res) => {
                this.outputQueryResponseToRun(newRun, res);
                this.messages$.next([...this.messages$.value]);
            },
            error: (err) => {
                let errorMsg = ``;
                if (isApiErrorResponse(err)) {
                    errorMsg = err.err.message;
                } else {
                    errorMsg = err?.message ?? err?.toString() ?? `Unknown error`;
                }
                newRun.log.appendLines(``, `## Result> Error`, ``, errorMsg);
                newRun.table.status = "error";
                newRun.graph.status = "error";
                this.messages$.next([...this.messages$.value]);
            },
        });
    }

    private outputQueryResponseToRun(run: RunOutputState, res: ApiResponse<QueryResponse>): void {
        const autoCommitted = this.driver.autoTransactionEnabled$.value && !isApiErrorResponse(res) && res.ok.queryType !== "read";
        run.log.appendBlankLine();
        run.log.appendQueryResult(res, autoCommitted);
        run.table.push(res);
        run.graph.push(res);
        run.raw.push(JSON.stringify(res, null, 2));
    }

    compactConversation(): void {
        const messages = this.messages$.value;
        if (messages.length === 0) return;

        // Set processing state
        this.isProcessing$.next(true);

        // Convert to cloud message format (excluding output blocks)
        const conversation: CloudChatMessage[] = messages.map((msg) => ({
            role: msg.sender === "user" ? "user" : "assistant",
            content: msg.content
                .filter((chunk): chunk is MessagePartText | MessagePartCode => chunk.type !== 'output')
                .map((chunk) => chunk.type === 'text' ? chunk.content : chunk.formControl.value)
                .join("\n\n"),
        }));

        // Call backend to compact conversation using OpenAI Responses API
        this.driver.getDatabaseSchemaText().pipe(
            switchMap((res) => {
                if (isOkResponse(res)) {
                    return this.cloud.aiCompact({
                        schema: res.ok,
                        conversation
                    });
                }
                else throw res;
            }),
        ).subscribe({
            next: (response) => {
                // Parse the compacted conversation back into ChatMessageData format
                const compactedMessages: ChatMessageData[] = response.compactedConversation.map((cloudMsg) => {
                    const parser = new StreamingMarkdownParser(this.formBuilder);
                    const content = parser.parseChunk(cloudMsg.content);

                    return {
                        id: this.generateMessageId(),
                        content,
                        sender: cloudMsg.role === "user" ? "user" : "ai",
                        timestamp: new Date(),
                    };
                });

                this.messages$.next(compactedMessages);
                this.persistConversation();
                this.isProcessing$.next(false);
            },
            error: (err) => {
                console.error('Compaction failed:', err);
                // Fallback to simple output removal if backend fails
                const compacted = messages.map((msg) => ({
                    ...msg,
                    content: msg.content.filter(part => part.type !== 'output')
                }));
                this.messages$.next(compacted);
                this.persistConversation();
                this.isProcessing$.next(false);
            }
        });
    }

    clearConversation(): void {
        this.destroyGraphVisualizers();
        this.messages$.next([]);
        this.persistConversation();
    }

    private persistConversation(): void {
        if (!this.currentDatabaseName || !this.selectedConversationId$.value) return;

        const existing = this.appData.chatConversations.getConversation(
            this.currentDatabaseName, this.selectedConversationId$.value
        );

        const serialized = this.serializeMessages(this.messages$.value);

        // Auto-generate title from first user message if still default
        let title = existing?.title || 'New conversation';
        if (title === 'New conversation') {
            if (this.pendingTitle) {
                title = truncateTitle(this.pendingTitle);
                this.pendingTitle = null;
            } else if (serialized.length > 0) {
                const firstUserMsg = serialized.find(m => m.sender === 'user');
                if (firstUserMsg) {
                    const textContent = firstUserMsg.content.find(p => p.type === 'text')?.content || '';
                    title = truncateTitle(textContent);
                }
            }
        }

        const conv: PersistedConversation = {
            id: this.selectedConversationId$.value,
            title,
            createdAt: existing?.createdAt || new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            messages: serialized,
        };
        this.appData.chatConversations.setConversation(this.currentDatabaseName, conv);
        this.refreshConversationList();
    }

    private restoreFromPersistence(): void {
        if (!this.currentDatabaseName || !this.selectedConversationId$.value) return;

        const conv = this.appData.chatConversations.getConversation(
            this.currentDatabaseName, this.selectedConversationId$.value
        );
        if (conv && conv.messages.length > 0) {
            this.messages$.next(this.deserializeMessages(conv.messages));
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
