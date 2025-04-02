/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Injectable } from "@angular/core";
import { Connection, SidebarState } from "../concept";
import { StorageService, StorageWriteResult } from "./storage.service";

const VIEW_STATE = "viewState";

interface ViewStateData {
    sidebarState: SidebarState;
}

const INITIAL_VIEW_STATE_DATA: ViewStateData = {
    sidebarState: "expanded"
};

class ViewState {

    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.readStorage() == null) {
            this.writeStorage(INITIAL_VIEW_STATE_DATA);
        }
    }

    private readStorage(): ViewStateData | null {
        if (!this.storage.isAccessible) return null;
        const data = this.storage.read(VIEW_STATE);
        if (data) return JSON.parse(data) as ViewStateData;
        else return null;
    }

    private writeStorage(prefs: ViewStateData): StorageWriteResult {
        return this.storage.write(VIEW_STATE, JSON.stringify(prefs));
    }

    sidebarState(): SidebarState {
        const viewState = this.readStorage();
        return viewState?.sidebarState || INITIAL_VIEW_STATE_DATA.sidebarState;
    }

    setSidebarState(value: SidebarState) {
        const viewState = this.readStorage();
        if (!viewState) return;
        viewState.sidebarState = value;
        this.writeStorage(viewState);
    }
}

const CONNECTIONS = "connections";

const INITIAL_CONNECTIONS: Connection[] = [];

class Connections {

    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.storage.read(CONNECTIONS) == null) {
            this.storage.write(CONNECTIONS, INITIAL_CONNECTIONS);
        }
    }

    list(): Connection[] {
        const data = this.storage.read(CONNECTIONS);
        if (data) return JSON.parse(data) as Connection[];
        else return [];
    }

    placeAtFront(connection: Connection): StorageWriteResult {
        const connections = [connection, ...this.list().filter(x => x.uri !== connection.uri)];
        return this.storage.write(CONNECTIONS, JSON.stringify(connections));
    }
}

const PREFERENCES = "preferences";

interface PreferencesData {
    connections: {
        showAdvancedConfigByDefault: boolean;
        savePasswordsByDefault: boolean;
    };
}

const INITIAL_PREFERENCES: PreferencesData = {
    connections: {
        showAdvancedConfigByDefault: false,
        savePasswordsByDefault: true,
    }
};

const FALLBACK_PREFERENCES: PreferencesData = {
    connections: {
        showAdvancedConfigByDefault: false,
        savePasswordsByDefault: false,
    }
};

class Preferences {

    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.readStorage() == null) {
            this.writeStorage(INITIAL_PREFERENCES);
        }
    }

    private readStorage(): PreferencesData | null {
        const data = this.storage.read(PREFERENCES);
        if (data) return JSON.parse(data) as PreferencesData;
        else return null;
    }

    private writeStorage(prefs: PreferencesData): StorageWriteResult {
        return this.storage.write(PREFERENCES, JSON.stringify(prefs));
    }

    readonly connection = {
        showAdvancedConfigByDefault: () => {
            const prefs = this.readStorage();
            return prefs?.connections.showAdvancedConfigByDefault || FALLBACK_PREFERENCES.connections.showAdvancedConfigByDefault;
        },
        setShowAdvancedConfigByDefault: (value: boolean) => {
            const prefs = this.readStorage()!;
            prefs.connections.showAdvancedConfigByDefault = value;
            return this.writeStorage(prefs);
        },
        savePasswordsByDefault: () => {
            const prefs = this.readStorage();
            return prefs?.connections.savePasswordsByDefault || FALLBACK_PREFERENCES.connections.savePasswordsByDefault;
        },
        setSavePasswordsByDefault: (value: boolean) => {
            const prefs = this.readStorage()!;
            prefs.connections.savePasswordsByDefault = value;
            return this.writeStorage(prefs);
        },
    };
}

@Injectable({
    providedIn: "root",
})
export class AppDataService {

    readonly isAccessible = this.storage.isAccessible;

    readonly viewState = new ViewState(this.storage);
    readonly connections = new Connections(this.storage);
    readonly preferences = new Preferences(this.storage);

    constructor(private storage: StorageService) {
    }
}
