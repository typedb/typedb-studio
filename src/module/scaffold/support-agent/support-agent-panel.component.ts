/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { AfterViewInit, Component, ElementRef, HostListener, OnDestroy, ViewChild } from "@angular/core";
import type { ChatKitOptions, OpenAIChatKit } from "@openai/chatkit";
import { Subscription } from "rxjs";
import { SupportAgentService } from "../../../service/support-agent.service";
import { EffectiveTheme, ThemeService } from "../../../service/theme.service";

/** Slide-over panel hosting the "AI Support" ChatKit widget. Mounted once on first open and
 *  then hidden/shown, so the conversation survives closing the panel. */
@Component({
    selector: "ts-support-agent-panel",
    templateUrl: "./support-agent-panel.component.html",
    styleUrls: ["./support-agent-panel.component.scss"],
    imports: [AsyncPipe],
})
export class SupportAgentPanelComponent implements AfterViewInit, OnDestroy {
    @ViewChild("host") host!: ElementRef<HTMLElement>;
    error: string | null = null;

    private chatkit: OpenAIChatKit | null = null;
    private domainKey: string | null = null;
    private colorScheme: EffectiveTheme = "dark";
    private mounting = false;
    private subscriptions: Subscription[] = [];

    constructor(readonly supportAgent: SupportAgentService, private theme: ThemeService) {}

    ngAfterViewInit() {
        this.subscriptions.push(
            this.theme.effectiveTheme$.subscribe((theme) => {
                this.colorScheme = theme;
                this.applyOptions();
            }),
            this.supportAgent.open$.subscribe((open) => {
                if (open) this.ensureMounted();
            }),
        );
    }

    ngOnDestroy() {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    @HostListener("document:keydown.escape")
    onEscape() {
        this.supportAgent.close();
    }

    private async ensureMounted() {
        if (this.chatkit) {
            this.chatkit.focusComposer();
            return;
        }
        if (this.mounting) return;
        this.mounting = true;
        this.error = null;
        try {
            const [domainKey] = await Promise.all([this.supportAgent.fetchDomainKey(), this.supportAgent.loadChatKit()]);
            this.domainKey = domainKey;
            const element = document.createElement("openai-chatkit") as OpenAIChatKit;
            element.addEventListener("chatkit.error", (event) => console.error("AI Support:", event.detail?.error));
            this.host.nativeElement.appendChild(element);
            this.chatkit = element;
            this.applyOptions();
            element.focusComposer();
        } catch (e) {
            console.error("AI Support failed to load:", e);
            this.error = "AI Support is unavailable right now. Please try again later.";
        } finally {
            this.mounting = false;
        }
    }

    private applyOptions() {
        if (!this.chatkit || !this.domainKey) return;
        this.chatkit.setOptions(this.options(this.domainKey));
    }

    private options(domainKey: string): ChatKitOptions {
        return {
            api: { url: this.supportAgent.apiUrl, domainKey, fetch: this.supportAgent.apiFetch },
            theme: {
                colorScheme: this.colorScheme,
                radius: "soft",
                density: "normal",
                color: {
                    accent: { primary: "#02dac9", level: 1 },
                    grayscale: { hue: 250, tint: 6, shade: 2 },
                },
            },
            header: {
                title: { text: "AI Support" },
                rightAction: { icon: "close", onClick: () => this.supportAgent.close() },
            },
            history: { enabled: true, showDelete: true, showRename: false },
            startScreen: {
                greeting: "How can I help you today?",
                prompts: [
                    { label: "Get started", prompt: "I'm in TypeDB Studio. How do I get started with it? Also briefly include info about and links to TypeDB crash courses." },
                    {
                        label: "TypeDB Studio features",
                        prompt: "I'm in TypeDB Studio. Walk me through its features.",
                    },
                    {
                        label: "Visualise data",
                        prompt: "I'm in TypeDB Studio. Describe how to visualise data in it. Mention Agent Mode and how to access it for AI-assisted visualisation.",
                    },
                    {
                        label: "Design schema / query data",
                        prompt: "I'm in TypeDB Studio. Explain what Agent Mode is and how to access it for AI-assisted schema design and query writing.",
                    },
                ],
            },
            composer: {
                placeholder: "Ask a question about TypeDB",
                attachments: { enabled: false },
            },
            threadItemActions: { feedback: true, retry: true },
            disclaimer: { text: "Answers are generated by AI and may be inaccurate. Check the docs before relying on them." },
        };
    }
}
