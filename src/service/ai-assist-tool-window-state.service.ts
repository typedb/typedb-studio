/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { FormControl } from "@angular/forms";
import { BehaviorSubject, catchError, finalize, switchMap } from "rxjs";
import { isOkResponse } from "typedb-driver-http";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { ChatMessage, CloudService } from "./cloud.service";
import { DriverState } from "./driver-state.service";

/**
 * Represents a message in the AI assistant conversation
 */
interface Message {
    content: string;
    sender: 'user' | 'ai';
    timestamp: Date;
    isProcessing?: boolean;
    error?: string;
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
            content: prompt,
            sender: "user",
            timestamp: new Date()
        };
        const aiMsg: Message = {
            content: "",
            sender: "ai",
            timestamp: new Date(),
            isProcessing: true
        };
        this.messages$.value.push(userMsg, aiMsg);
        this.messages$.next(...[this.messages$.value]);
        this.promptControl.patchValue("");

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
                    aiMsg.content = res.response;
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
