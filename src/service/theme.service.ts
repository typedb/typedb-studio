/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable, OnDestroy } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { StorageService } from "./storage.service";

export type ThemePreference = "dark" | "light" | "system";
export type EffectiveTheme = "dark" | "light";

const STORAGE_KEY = "theme.preference";

@Injectable({
    providedIn: "root",
})
export class ThemeService implements OnDestroy {
    private mediaQuery = window.matchMedia("(prefers-color-scheme: light)");

    readonly preference$ = new BehaviorSubject<ThemePreference>(this.loadPreference());
    readonly effectiveTheme$ = new BehaviorSubject<EffectiveTheme>(this.resolveEffective(this.preference$.value));

    private mediaListener = () => {
        if (this.preference$.value === "system") {
            this.applyTheme(this.resolveEffective("system"));
        }
    };

    constructor(private storage: StorageService) {
        this.mediaQuery.addEventListener("change", this.mediaListener);
        this.applyTheme(this.effectiveTheme$.value);
    }

    ngOnDestroy() {
        this.mediaQuery.removeEventListener("change", this.mediaListener);
    }

    setPreference(pref: ThemePreference) {
        this.preference$.next(pref);
        this.storage.write(STORAGE_KEY, pref);
        this.applyTheme(this.resolveEffective(pref));
    }

    cycleTheme() {
        const order: ThemePreference[] = ["dark", "light", "system"];
        const next = order[(order.indexOf(this.preference$.value) + 1) % order.length];
        this.setPreference(next);
    }

    private resolveEffective(pref: ThemePreference): EffectiveTheme {
        if (pref === "system") {
            return this.mediaQuery.matches ? "light" : "dark";
        }
        return pref;
    }

    private applyTheme(theme: EffectiveTheme) {
        const html = document.documentElement;
        html.classList.remove("theme-dark", "theme-light");
        html.classList.add(theme === "light" ? "theme-light" : "theme-dark");
        this.effectiveTheme$.next(theme);
    }

    private loadPreference(): ThemePreference {
        return this.storage.read<ThemePreference>(STORAGE_KEY, (obj): ThemePreference => {
            const val = obj as string;
            if (val === "dark" || val === "light" || val === "system") return val;
            return "dark";
        });
    }
}
