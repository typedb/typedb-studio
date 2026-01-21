/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { SchemaConcept } from "./schema-state.service";

export interface TypeTableTab {
    kind: "type-table";
    type: SchemaConcept;
    totalCount: number;
    selectedInstanceIID: string | null;
    typeqlFilter?: string;
}

export interface InstanceDetailTab {
    kind: "instance-detail";
    type: SchemaConcept;
    instanceIID: string;
}

export type DataTab = TypeTableTab | InstanceDetailTab;

@Injectable({
    providedIn: "root",
})
export class DataEditorState {
    openTabs$ = new BehaviorSubject<DataTab[]>([]);
    selectedTabIndex$ = new BehaviorSubject<number>(0);

    openTypeTab(type: SchemaConcept) {
        const tabs = this.openTabs$.value;
        const existing = tabs.find(t => t.kind === "type-table" && t.type.label === type.label);

        if (existing) {
            this.selectedTabIndex$.next(tabs.indexOf(existing));
        } else {
            const newTab: TypeTableTab = {
                kind: "type-table",
                type,
                totalCount: 0,
                selectedInstanceIID: null,
            };
            this.openTabs$.next([...tabs, newTab]);
            this.selectedTabIndex$.next(tabs.length);
        }
    }

    closeTab(tab: DataTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        const newTabs = tabs.filter(t => t !== tab);
        this.openTabs$.next(newTabs);

        // Adjust selected index if necessary
        if (this.selectedTabIndex$.value >= newTabs.length && newTabs.length > 0) {
            this.selectedTabIndex$.next(newTabs.length - 1);
        }
    }

    openInstanceDetail(type: SchemaConcept, instanceIID: string) {
        const tabs = this.openTabs$.value;
        // Check if a tab for this specific instance already exists
        const existing = tabs.find(t => t.kind === "instance-detail" && t.instanceIID === instanceIID);

        if (existing) {
            this.selectedTabIndex$.next(tabs.indexOf(existing));
        } else {
            const newTab: InstanceDetailTab = {
                kind: "instance-detail",
                type,
                instanceIID,
            };
            this.openTabs$.next([...tabs, newTab]);
            this.selectedTabIndex$.next(tabs.length);
        }
    }

    updateTabCount(tab: TypeTableTab, count: number) {
        const tabs = this.openTabs$.value;
        const index = tabs.findIndex(t => t === tab);
        if (index !== -1) {
            const newTabs = [...tabs];
            const existingTab = tabs[index];
            if (existingTab.kind === "type-table") {
                newTabs[index] = { ...existingTab, totalCount: count };
                this.openTabs$.next(newTabs);
            }
        }
    }

    openFilteredTypeTab(type: SchemaConcept, typeqlFilter: string) {
        const tabs = this.openTabs$.value;
        // Check if a tab with the same type and filter already exists
        const existing = tabs.find(t =>
            t.kind === "type-table" &&
            t.type.label === type.label &&
            t.typeqlFilter === typeqlFilter
        );

        if (existing) {
            this.selectedTabIndex$.next(tabs.indexOf(existing));
        } else {
            const newTab: TypeTableTab = {
                kind: "type-table",
                type,
                totalCount: 0,
                selectedInstanceIID: null,
                typeqlFilter,
            };
            this.openTabs$.next([...tabs, newTab]);
            this.selectedTabIndex$.next(tabs.length);
        }
    }
}
