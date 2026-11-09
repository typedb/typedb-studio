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
                greeting: "What would you like to know about TypeDB?",
                prompts: [
                    { label: "Connect to a server", prompt: "How do I connect TypeDB Studio to my TypeDB server?" },
                    {
                        label: "Write a query",
                        prompt: "Help me write a TypeQL query. I'll provide the relevant schema and details of the query I want you to write.",
                    },
                    {
                        label: "Design a schema",
                        prompt: "Help me design a TypeDB schema. I'll describe the domain in a sentence or two. Make sensible assumptions and give me a first draft we can refine.",
                    },
                    {
                        label: "What's new",
                        prompt: "What's the latest news from TypeDB? Use recent news-like blog posts and recent releases in the main TypeDB repos as content sources (for example: typedb, typeql, typedb-studio, typedb-driver, typedb-tools, typedb-examples). In your summary, include only 'major' / interesting news and present news broken down by theme, not by content source.",
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
