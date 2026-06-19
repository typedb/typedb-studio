/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject, Injectable } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { BehaviorSubject, firstValueFrom } from "rxjs";
import { AppData } from "./app-data.service";
import { SnackbarService } from "./snackbar.service";

/**
 * Central gate for AI Agent features (chat / vibe query / "send to AI"), all of
 * which share the database schema and chat data with the AI backend (OpenAI).
 *
 * Consent is a single global, persisted opt-in flag. It does NOT gate the
 * Intercom-based "AI Support" button, which is unrelated. Enforcement lives at
 * the network chokepoint (`CloudService`) so no path can leak data; this
 * service drives the consent UX (lazy grant modal on first use, withdrawal).
 */
@Injectable({
    providedIn: "root",
})
export class AiConsentState {
    private appData = inject(AppData);
    private dialog = inject(MatDialog);
    private snackbar = inject(SnackbarService);

    /** Emits the current consent state; components subscribe to render the
     *  footer / withdraw affordance reactively. */
    readonly granted$ = new BehaviorSubject<boolean>(this.appData.aiConsent.isGranted());

    isGranted(): boolean {
        return this.granted$.value;
    }

    grantedAt(): number | null {
        return this.appData.aiConsent.grantedAt();
    }

    /**
     * Gate an AI action. Resolves `true` if the user may proceed (consent
     * already granted, or just granted via the modal), `false` if they declined
     * or dismissed it. Callers MUST await this and only proceed on `true`.
     */
    async requireConsent(): Promise<boolean> {
        if (this.isGranted()) return true;
        // Imported lazily to avoid a service<->component import cycle and to keep
        // the dialog out of the initial bundle for users who never touch AI.
        const { AiConsentGrantDialogComponent } = await import(
            "../module/ai/consent/ai-consent-grant-dialog.component"
        );
        const ref = this.dialog.open(AiConsentGrantDialogComponent, {
            width: "560px",
            disableClose: false,
            autoFocus: false,
        });
        const result = await firstValueFrom(ref.afterClosed());
        if (result === true) {
            this.grant();
            return true;
        }
        return false;
    }

    grant(): void {
        this.appData.aiConsent.grant();
        this.granted$.next(true);
    }

    /**
     * Open the withdrawal confirmation modal. If confirmed, revokes consent and
     * (when the user opted in) clears all locally-stored chat history.
     */
    async withdraw(): Promise<void> {
        const { AiConsentWithdrawDialogComponent } = await import(
            "../module/ai/consent/ai-consent-withdraw-dialog.component"
        );
        const ref = this.dialog.open(AiConsentWithdrawDialogComponent, {
            width: "560px",
            autoFocus: false,
        });
        const result = await firstValueFrom(ref.afterClosed()) as
            { confirmed: boolean; clearHistory: boolean } | undefined;
        if (!result?.confirmed) return;
        this.appData.aiConsent.withdraw();
        this.granted$.next(false);
        if (result.clearHistory) {
            this.appData.chatConversations.clearAll();
            this.snackbar.info("AI features are now disabled. All chat history has been deleted locally.");
        } else {
            this.snackbar.info("AI features are now disabled.");
        }
    }
}
