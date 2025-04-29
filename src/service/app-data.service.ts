/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { ConnectionConfig, ConnectionJson } from "../concept/connection";
import { SidebarState, Tool } from "../concept/view-state";
import { StorageService, StorageWriteResult } from "./storage.service";

function isObjectWithFields<FIELD extends string>(obj: unknown, fields: FIELD[]): obj is { [K in typeof fields[number]]: unknown } {
    return obj != null && typeof obj === "object" && fields.every(x => x in obj);
}

const VIEW_STATE = "viewState";

interface ViewStateData {
    sidebarState: SidebarState;
    lastUsedTool: Tool;
}

function parseViewStateDataOrNull(obj: Object): ViewStateData | null {
    return isObjectWithFields(obj, ["sidebarState", "lastUsedTool"]) ? obj as ViewStateData : null;
}

const INITIAL_VIEW_STATE_DATA: ViewStateData = {
    sidebarState: "collapsed",
    lastUsedTool: "query",
};

class ViewState {

    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.readStorage() == null) {
            this.writeStorage(INITIAL_VIEW_STATE_DATA);
        }
    }

    private readStorage(): ViewStateData | null {
        if (!this.storage.isAccessible) return null;
        return this.storage.read<ViewStateData>(VIEW_STATE, parseViewStateDataOrNull);
    }

    private writeStorage(prefs: ViewStateData): StorageWriteResult {
        return this.storage.write(VIEW_STATE, prefs);
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

    lastUsedTool(): Tool {
        const viewState = this.readStorage();
        return viewState?.lastUsedTool || INITIAL_VIEW_STATE_DATA.lastUsedTool;
    }

    setLastUsedTool(value: Tool) {
        const viewState = this.readStorage();
        if (!viewState) return;
        viewState.lastUsedTool = value;
        this.writeStorage(viewState);
    }

    lastUsedToolRoute(): string {
        const lastUsedTool = this.lastUsedTool();
        return lastUsedTool === "query" ? `/query` : `/explore`;
    }
}

const CONNECTIONS = "connections";

const INITIAL_CONNECTIONS: ConnectionConfig[] = [];

class Connections {

    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.storage.read(CONNECTIONS, (obj) => obj as ConnectionJson[]) == null) {
            this.storage.write(CONNECTIONS, INITIAL_CONNECTIONS);
        }
    }

    findStartupConnection(): ConnectionConfig | null {
        return this.list().find(x => x.preferences.isStartupConnection) || null;
    }

    list(): ConnectionConfig[] {
        const data = this.storage.read(CONNECTIONS, (obj) => obj as ConnectionJson[]);
        if (!data) return [];
        // TODO: throw / optionally report on illegal JSON
        const connections = data.map(x => ConnectionConfig.fromJSONOrNull(x));
        if (connections.every(x => !!x)) return connections as ConnectionConfig[];
        else return [];
    }

    push(connection: ConnectionConfig): StorageWriteResult {
        const list = this.list();
        const connections = [connection, ...list.filter(x => x.url !== connection.url).slice(0, 9)];
        if (connection.preferences.isStartupConnection) {
            connections.slice(1).forEach(x => x.preferences.isStartupConnection = false);
        }
        return this.storage.write(CONNECTIONS, connections.map(x => x.toJSON()));
    }

    clearStartupConnection(): StorageWriteResult {
        const connections = this.list();
        connections.forEach(conn => conn.preferences.isStartupConnection = false);
        return this.storage.write(CONNECTIONS, connections.map(x => x.toJSON()));
    }
}

const PREFERENCES = "preferences";

interface PreferencesData {
    connections: {
        showAdvancedConfigByDefault: boolean;
        savePasswordsByDefault: boolean;
    };
}

function parsePreferencesDataOrNull(obj: Object): PreferencesData | null {
    return isObjectWithFields(obj, ["connections"]) && isObjectWithFields(obj.connections, ["showAdvancedConfigByDefault", "savePasswordsByDefault"]) ? obj as PreferencesData : null;
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
        return this.storage.read(PREFERENCES, parsePreferencesDataOrNull);
    }

    private writeStorage(prefs: PreferencesData): StorageWriteResult {
        return this.storage.write(PREFERENCES, prefs);
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
export class AppData {

    readonly isAccessible = this.storage.isAccessible;

    readonly viewState = new ViewState(this.storage);
    readonly connections = new Connections(this.storage);
    readonly preferences = new Preferences(this.storage);

    constructor(private storage: StorageService) {
    }
}
