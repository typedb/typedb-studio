/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { SchemaConcept, SchemaState } from "./schema-state.service";
import { AppData, PersistedDataTab } from "./app-data.service";
import { DriverState } from "./driver-state.service";

export interface TypeTableTab {
    kind: "type-table";
    type: SchemaConcept;
    totalCount: number;
    selectedInstanceIID: string | null;
    typeqlFilter?: string;
    breadcrumbs?: BreadcrumbItem[];
    pinned?: boolean;
}

export interface BreadcrumbItem {
    kind: "type-table" | "instance-detail";
    typeLabel: string;
    instanceIID?: string;
}

export interface InstanceDetailTab {
    kind: "instance-detail";
    type: SchemaConcept;
    instanceIID: string;
    breadcrumbs: BreadcrumbItem[];
    pinned?: boolean;
}

export type DataTab = TypeTableTab | InstanceDetailTab;

@Injectable({
    providedIn: "root",
})
export class DataEditorState {
    openTabs$ = new BehaviorSubject<DataTab[]>([]);
    selectedTabIndex$ = new BehaviorSubject<number>(0);
    /** True while tabs are being restored (waiting for schema) */
    restoringTabs$ = new BehaviorSubject<boolean>(false);

    private currentDatabaseName: string | null = null;

    constructor(
        private schemaState: SchemaState,
        private appData: AppData,
        private driverState: DriverState,
    ) {
        // Subscribe to database changes to save/restore tabs
        this.driverState.database$.subscribe(database => {
            this.onDatabaseChange(database?.name || null);
        });

        // Subscribe to tab changes to persist
        this.openTabs$.subscribe(() => this.persistTabs());
        this.selectedTabIndex$.subscribe(() => this.persistTabs());
    }

    private onDatabaseChange(databaseName: string | null) {
        // Save current tabs before switching (if we had a database)
        if (this.currentDatabaseName && this.currentDatabaseName !== databaseName) {
            this.persistTabs();
        }

        // Clear tabs when disconnecting or switching databases
        this.openTabs$.next([]);
        this.selectedTabIndex$.next(0);
        this.currentDatabaseName = databaseName;

        // Restore tabs for the new database (if any)
        if (databaseName) {
            this.restoreTabsForDatabase(databaseName);
        }
    }

    private persistTabs() {
        if (!this.currentDatabaseName) return;

        const tabs = this.openTabs$.value;
        const persistedTabs: PersistedDataTab[] = tabs.map(tab => {
            if (tab.kind === "type-table") {
                return {
                    kind: "type-table" as const,
                    typeLabel: tab.type.label,
                    typeqlFilter: tab.typeqlFilter,
                    breadcrumbs: tab.breadcrumbs,
                    pinned: tab.pinned,
                };
            } else {
                return {
                    kind: "instance-detail" as const,
                    typeLabel: tab.type.label,
                    instanceIID: tab.instanceIID,
                    breadcrumbs: tab.breadcrumbs,
                    pinned: tab.pinned,
                };
            }
        });

        this.appData.dataExplorerTabs.setTabs(
            this.currentDatabaseName,
            persistedTabs,
            this.selectedTabIndex$.value
        );
    }

    private restoreTabsForDatabase(databaseName: string) {
        const saved = this.appData.dataExplorerTabs.getTabs(databaseName);
        if (!saved || saved.tabs.length === 0) return;

        // Wait for schema to be available before restoring tabs
        const schema = this.schemaState.value$.value;
        if (!schema) {
            // Schema not yet loaded, indicate we're waiting to restore
            this.restoringTabs$.next(true);
            const subscription = this.schemaState.value$.subscribe(loadedSchema => {
                if (loadedSchema && this.currentDatabaseName === databaseName) {
                    subscription.unsubscribe();
                    this.restoreTabsWithSchema(saved, loadedSchema);
                    this.restoringTabs$.next(false);
                }
            });
            return;
        }

        this.restoreTabsWithSchema(saved, schema);
    }

    private restoreTabsWithSchema(
        saved: { tabs: PersistedDataTab[]; selectedTabIndex: number },
        schema: NonNullable<ReturnType<typeof this.schemaState.value$.getValue>>
    ) {
        const restoredTabs: DataTab[] = [];

        for (const persistedTab of saved.tabs) {
            // Look up the type in the schema
            const type = schema.entities[persistedTab.typeLabel] ||
                         schema.relations[persistedTab.typeLabel] ||
                         schema.attributes[persistedTab.typeLabel];

            if (!type) {
                // Type no longer exists in schema, skip this tab
                continue;
            }

            if (persistedTab.kind === "type-table") {
                restoredTabs.push({
                    kind: "type-table",
                    type,
                    totalCount: 0,
                    selectedInstanceIID: null,
                    typeqlFilter: persistedTab.typeqlFilter,
                    breadcrumbs: persistedTab.breadcrumbs,
                    pinned: persistedTab.pinned,
                });
            } else {
                restoredTabs.push({
                    kind: "instance-detail",
                    type,
                    instanceIID: persistedTab.instanceIID,
                    breadcrumbs: persistedTab.breadcrumbs,
                    pinned: persistedTab.pinned,
                });
            }
        }

        if (restoredTabs.length > 0) {
            this.openTabs$.next(restoredTabs);

            // Restore selected index, bounded by the actual tab count
            const selectedIndex = Math.min(saved.selectedTabIndex, restoredTabs.length - 1);
            this.selectedTabIndex$.next(selectedIndex);
        }
    }

    openTypeTab(type: SchemaConcept, breadcrumbs: BreadcrumbItem[] = []) {
        const tabs = this.openTabs$.value;
        // Find existing tab without a filter (filtered tabs are separate)
        const existing = tabs.find(t => t.kind === "type-table" && t.type.label === type.label && !t.typeqlFilter);

        if (existing) {
            // Update breadcrumbs on existing tab if new ones provided
            if (breadcrumbs.length > 0) {
                const index = tabs.indexOf(existing);
                const newTabs = [...tabs];
                newTabs[index] = { ...existing, breadcrumbs };
                this.openTabs$.next(newTabs);
            }
            this.selectedTabIndex$.next(tabs.indexOf(existing));
        } else {
            const newTab: TypeTableTab = {
                kind: "type-table",
                type,
                totalCount: 0,
                selectedInstanceIID: null,
                breadcrumbs: breadcrumbs.length > 0 ? breadcrumbs : undefined,
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

    closeOtherTabs(tab: DataTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        // Keep the target tab and all pinned tabs
        const newTabs = tabs.filter(t => t === tab || t.pinned);
        this.openTabs$.next(newTabs);

        // Update selected index to the target tab's new position
        const newIndex = newTabs.indexOf(tab);
        this.selectedTabIndex$.next(newIndex);
    }

    closeTabsToRight(tab: DataTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        // Keep tabs up to and including the target, plus any pinned tabs after
        const newTabs = tabs.filter((t, i) => i <= index || t.pinned);
        this.openTabs$.next(newTabs);

        // Adjust selected index if it was in the closed range
        if (this.selectedTabIndex$.value >= newTabs.length) {
            this.selectedTabIndex$.next(newTabs.length - 1);
        }
    }

    closeAllTabs() {
        const tabs = this.openTabs$.value;
        // Keep only pinned tabs
        const newTabs = tabs.filter(t => t.pinned);
        this.openTabs$.next(newTabs);

        if (newTabs.length > 0) {
            // Select the first remaining tab
            this.selectedTabIndex$.next(0);
        } else {
            this.selectedTabIndex$.next(0);
        }
    }

    togglePinTab(tab: DataTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;

        const newTabs = [...tabs];
        const updatedTab = { ...tab, pinned: !tab.pinned };
        newTabs[index] = updatedTab;

        // If pinning, move tab to the left (after other pinned tabs)
        // If unpinning, keep in place
        if (updatedTab.pinned) {
            // Find the position after the last pinned tab
            const lastPinnedIndex = newTabs.reduce((acc, t, i) => t.pinned && i !== index ? i : acc, -1);
            if (lastPinnedIndex < index - 1) {
                // Move to after last pinned tab
                newTabs.splice(index, 1);
                newTabs.splice(lastPinnedIndex + 1, 0, updatedTab);
                // Adjust selected index if needed
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

    openInstanceDetail(type: SchemaConcept, instanceIID: string, breadcrumbs: BreadcrumbItem[] = []) {
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
                breadcrumbs,
            };
            this.openTabs$.next([...tabs, newTab]);
            this.selectedTabIndex$.next(tabs.length);
        }
    }

    navigateToBreadcrumb(breadcrumb: BreadcrumbItem, breadcrumbIndex: number, allBreadcrumbs: BreadcrumbItem[]) {
        const tabs = this.openTabs$.value;
        // Keep only breadcrumbs before this one
        const truncatedBreadcrumbs = allBreadcrumbs.slice(0, breadcrumbIndex);

        if (breadcrumb.kind === "type-table") {
            // Find and focus the type table tab (without filter)
            const existing = tabs.find((t): t is TypeTableTab => t.kind === "type-table" && t.type.label === breadcrumb.typeLabel && !t.typeqlFilter);
            if (existing) {
                // Update breadcrumbs on existing tab
                const index = tabs.indexOf(existing);
                const newTabs = [...tabs];
                const updatedTab: TypeTableTab = { ...existing, breadcrumbs: truncatedBreadcrumbs.length > 0 ? truncatedBreadcrumbs : undefined };
                newTabs[index] = updatedTab;
                this.openTabs$.next(newTabs);
                this.selectedTabIndex$.next(index);
            } else {
                // Reopen the type table tab with truncated breadcrumbs
                const schema = this.schemaState.value$.value;
                if (schema) {
                    const type = schema.entities[breadcrumb.typeLabel] || schema.relations[breadcrumb.typeLabel];
                    if (type) {
                        this.openTypeTab(type, truncatedBreadcrumbs);
                    }
                }
            }
        } else if (breadcrumb.kind === "instance-detail" && breadcrumb.instanceIID) {
            // Find and focus the instance detail tab
            const existing = tabs.find(t => t.kind === "instance-detail" && t.instanceIID === breadcrumb.instanceIID);
            if (existing) {
                this.selectedTabIndex$.next(tabs.indexOf(existing));
            } else {
                // Reopen the instance detail tab with truncated breadcrumbs
                const schema = this.schemaState.value$.value;
                if (schema) {
                    const type = schema.entities[breadcrumb.typeLabel] || schema.relations[breadcrumb.typeLabel];
                    if (type) {
                        this.openInstanceDetail(type, breadcrumb.instanceIID, truncatedBreadcrumbs);
                    }
                }
            }
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
