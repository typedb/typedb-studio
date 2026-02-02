/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { FormControl } from "@angular/forms";
import { BehaviorSubject } from "rxjs";
import { AppData, PersistedQueryTab } from "./app-data.service";

export interface QueryTab {
    id: string;
    name: string;
    query: string;
    pinned?: boolean;
}

@Injectable({
    providedIn: "root",
})
export class QueryTabsState {
    openTabs$ = new BehaviorSubject<QueryTab[]>([]);
    selectedTabIndex$ = new BehaviorSubject<number>(0);

    private tabControls = new Map<string, FormControl<string>>();

    constructor(private appData: AppData) {
        this.restoreTabs();

        this.openTabs$.subscribe(() => this.persistTabs());
        this.selectedTabIndex$.subscribe(() => this.persistTabs());
    }

    get currentTab(): QueryTab | null {
        const tabs = this.openTabs$.value;
        const index = this.selectedTabIndex$.value;
        return tabs[index] || null;
    }

    getTabControl(tab: QueryTab): FormControl<string> {
        let control = this.tabControls.get(tab.id);
        if (!control) {
            control = new FormControl(tab.query, { nonNullable: true });
            control.valueChanges.subscribe(value => {
                this.updateTabQuery(tab.id, value);
            });
            this.tabControls.set(tab.id, control);
        }
        return control;
    }

    private persistTabs() {
        const tabs = this.openTabs$.value;
        const persistedTabs: PersistedQueryTab[] = tabs.map(tab => ({
            id: tab.id,
            name: tab.name,
            query: tab.query,
            pinned: tab.pinned,
        }));

        this.appData.queryTabs.setTabs(
            persistedTabs,
            this.selectedTabIndex$.value
        );
    }

    private restoreTabs() {
        const saved = this.appData.queryTabs.getTabs();
        if (saved.tabs.length === 0) {
            this.newTab();
            return;
        }

        const restoredTabs: QueryTab[] = saved.tabs.map(persistedTab => ({
            id: persistedTab.id,
            name: persistedTab.name,
            query: persistedTab.query,
            pinned: persistedTab.pinned,
        }));

        this.openTabs$.next(restoredTabs);
        const selectedIndex = Math.min(saved.selectedTabIndex, restoredTabs.length - 1);
        this.selectedTabIndex$.next(selectedIndex);
    }

    newTab(): QueryTab {
        const tabs = this.openTabs$.value;
        const newTabNumber = this.getNextTabNumber();
        const newTab: QueryTab = {
            id: crypto.randomUUID(),
            name: `Query ${newTabNumber}`,
            query: "",
        };
        this.openTabs$.next([...tabs, newTab]);
        this.selectedTabIndex$.next(tabs.length);
        return newTab;
    }

    private getNextTabNumber(): number {
        const tabs = this.openTabs$.value;
        const existingNumbers = tabs
            .map(tab => {
                const match = tab.name.match(/^Query (\d+)$/);
                return match ? parseInt(match[1], 10) : 0;
            })
            .filter(n => n > 0);

        if (existingNumbers.length === 0) return 1;
        return Math.max(...existingNumbers) + 1;
    }

    selectTab(index: number) {
        const tabs = this.openTabs$.value;
        if (index >= 0 && index < tabs.length) {
            this.selectedTabIndex$.next(index);
        }
    }

    closeTab(tab: QueryTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        this.tabControls.delete(tab.id);
        const newTabs = tabs.filter(t => t !== tab);

        if (newTabs.length === 0) {
            this.newTab();
            return;
        }

        this.openTabs$.next(newTabs);

        if (this.selectedTabIndex$.value >= newTabs.length && newTabs.length > 0) {
            this.selectedTabIndex$.next(newTabs.length - 1);
        }
    }

    closeOtherTabs(tab: QueryTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        const newTabs = tabs.filter(t => t === tab || t.pinned);

        for (const t of tabs) {
            if (t !== tab && !t.pinned) {
                this.tabControls.delete(t.id);
            }
        }

        this.openTabs$.next(newTabs);
        const newIndex = newTabs.indexOf(tab);
        this.selectedTabIndex$.next(newIndex);
    }

    closeTabsToRight(tab: QueryTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        const newTabs = tabs.filter((t, i) => i <= index || t.pinned);

        for (const t of tabs) {
            if (tabs.indexOf(t) > index && !t.pinned) {
                this.tabControls.delete(t.id);
            }
        }

        this.openTabs$.next(newTabs);

        if (this.selectedTabIndex$.value >= newTabs.length) {
            this.selectedTabIndex$.next(newTabs.length - 1);
        }
    }

    closeAllTabs() {
        const tabs = this.openTabs$.value;
        const newTabs = tabs.filter(t => t.pinned);

        for (const t of tabs) {
            if (!t.pinned) {
                this.tabControls.delete(t.id);
            }
        }

        if (newTabs.length === 0) {
            this.openTabs$.next([]);
            this.newTab();
            return;
        }

        this.openTabs$.next(newTabs);
        this.selectedTabIndex$.next(0);
    }

    togglePinTab(tab: QueryTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        const newTabs = [...tabs];
        const updatedTab = { ...tab, pinned: !tab.pinned };
        newTabs[index] = updatedTab;

        if (updatedTab.pinned) {
            const lastPinnedIndex = newTabs.reduce((acc, t, i) => t.pinned && i !== index ? i : acc, -1);
            if (lastPinnedIndex < index - 1) {
                newTabs.splice(index, 1);
                newTabs.splice(lastPinnedIndex + 1, 0, updatedTab);
                const currentSelected = this.selectedTabIndex$.value;
                if (currentSelected === index) {
                    this.selectedTabIndex$.next(lastPinnedIndex + 1);
                } else if (currentSelected > lastPinnedIndex && currentSelected < index) {
                    this.selectedTabIndex$.next(currentSelected + 1);
                }
            }
        }

        this.openTabs$.next(newTabs);
    }

    renameTab(tab: QueryTab, newName: string) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        const trimmedName = newName.trim();
        if (!trimmedName) return;

        const newTabs = [...tabs];
        newTabs[index] = { ...tab, name: trimmedName };
        this.openTabs$.next(newTabs);
    }

    duplicateTab(tab: QueryTab): QueryTab {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        const insertIndex = index === -1 ? tabs.length : index + 1;

        const newTab: QueryTab = {
            id: crypto.randomUUID(),
            name: `${tab.name} (copy)`,
            query: tab.query,
        };

        const newTabs = [...tabs];
        newTabs.splice(insertIndex, 0, newTab);
        this.openTabs$.next(newTabs);
        this.selectedTabIndex$.next(insertIndex);
        return newTab;
    }

    updateTabQuery(tabId: string, query: string) {
        const tabs = this.openTabs$.value;
        const index = tabs.findIndex(t => t.id === tabId);
        if (index === -1) return;

        const newTabs = [...tabs];
        newTabs[index] = { ...tabs[index], query };
        this.openTabs$.next(newTabs);
    }
}
