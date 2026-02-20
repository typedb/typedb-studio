/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { ConnectionConfig, ConnectionJson } from "../concept/connection";
import { OperationMode } from "../concept/transaction";
import { SchemaToolWindowState, SidebarState, sidebarStates, Tool, tools } from "../concept/view-state";
import { StorageService, StorageWriteResult } from "./storage.service";

export type RowLimit = 10 | 50 | 100 | 500 | 1000 | 5000 | "none";

function isObjectWithFields<FIELD extends string>(obj: unknown, fields: FIELD[]): obj is { [K in typeof fields[number]]: unknown } {
    return obj != null && typeof obj === "object" && fields.every(x => x in obj);
}

const VIEW_STATE = "viewState";

interface ViewStateData {
    sidebarState: SidebarState;
    lastUsedTool: Tool | null;
    schemaToolWindowState: SchemaToolWindowState;
}

const INITIAL_VIEW_STATE_DATA: ViewStateData = {
    sidebarState: "collapsed",
    lastUsedTool: null,
    schemaToolWindowState: {
        linksVisibility: {
            sub: true,
            owns: true,
            plays: true,
            relates: true,
        },
        viewMode: "hierarchical",
        rootNodesCollapsed: {
            entities: false,
            relations: false,
            attributes: false,
        },
    },
};

function parseViewStateData(obj: Object | null): ViewStateData {
    return Object.assign({}, INITIAL_VIEW_STATE_DATA, obj) as ViewStateData;
}

class ViewState {

    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.readStorage() == null) {
            this.writeStorage(INITIAL_VIEW_STATE_DATA);
        }
    }

    private readStorage(): ViewStateData {
        if (!this.storage.isAccessible) return INITIAL_VIEW_STATE_DATA;
        return this.storage.read<ViewStateData>(VIEW_STATE, parseViewStateData);
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
        viewState.sidebarState = value;
        this.writeStorage(viewState);
    }

    lastUsedTool(): Tool | null {
        const viewState = this.readStorage();
        return viewState.lastUsedTool;
    }

    setLastUsedTool(value: Tool) {
        const viewState = this.readStorage();
        viewState.lastUsedTool = value;
        this.writeStorage(viewState);
    }

    lastUsedToolRoute(): string {
        const lastUsedTool = this.lastUsedTool();
        switch (lastUsedTool) {
            case "query": return "/query";
            case "schema": return "/schema";
            case "data": return "/data";
            case "chat": return "/agent-mode";
            case null: return "/welcome";
        }
    }

    schemaToolWindowState(): SchemaToolWindowState {
        const viewState = this.readStorage();
        return viewState.schemaToolWindowState || INITIAL_VIEW_STATE_DATA.schemaToolWindowState;
    }

    setSchemaToolWindowState(value: SchemaToolWindowState) {
        const viewState = this.readStorage();
        viewState.schemaToolWindowState = value;
        this.writeStorage(viewState);
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
    };
    transactionMode: OperationMode;
    queryRowLimit: RowLimit;
}

const DATA_EXPLORER_TABS = "dataExplorerTabs";

export interface PersistedBreadcrumbItem {
    kind: "type-table" | "instance-detail";
    typeLabel: string;
    typeKind?: "entityType" | "relationType" | "attributeType";
    instanceIID?: string;
}

export interface PersistedTypeTableTab {
    kind: "type-table";
    typeLabel: string;
    typeqlFilter?: string;
    breadcrumbs?: PersistedBreadcrumbItem[];
    pinned?: boolean;
}

export interface PersistedInstanceDetailTab {
    kind: "instance-detail";
    typeLabel: string;
    instanceIID: string;
    breadcrumbs: PersistedBreadcrumbItem[];
    pinned?: boolean;
}

export type PersistedDataTab = PersistedTypeTableTab | PersistedInstanceDetailTab;

export interface PersistedQueryTab {
    id: string;
    name: string;
    query: string;
    pinned?: boolean;
}

interface DataExplorerTabsData {
    /** Maps database name to its persisted tabs */
    databases: {
        [databaseName: string]: {
            tabs: PersistedDataTab[];
            selectedTabIndex: number;
        };
    };
}

const INITIAL_DATA_EXPLORER_TABS: DataExplorerTabsData = {
    databases: {},
};

function parseDataExplorerTabsData(obj: Object | null): DataExplorerTabsData {
    return Object.assign({}, INITIAL_DATA_EXPLORER_TABS, obj) as DataExplorerTabsData;
}

class DataExplorerTabs {
    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.readStorage() == null) {
            this.writeStorage(INITIAL_DATA_EXPLORER_TABS);
        }
    }

    private readStorage(): DataExplorerTabsData {
        if (!this.storage.isAccessible) return INITIAL_DATA_EXPLORER_TABS;
        return this.storage.read<DataExplorerTabsData>(DATA_EXPLORER_TABS, parseDataExplorerTabsData);
    }

    private writeStorage(data: DataExplorerTabsData): StorageWriteResult {
        return this.storage.write(DATA_EXPLORER_TABS, data);
    }

    getTabs(databaseName: string): { tabs: PersistedDataTab[]; selectedTabIndex: number } | null {
        const data = this.readStorage();
        return data.databases[databaseName] || null;
    }

    setTabs(databaseName: string, tabs: PersistedDataTab[], selectedTabIndex: number): StorageWriteResult {
        const data = this.readStorage();
        data.databases[databaseName] = { tabs, selectedTabIndex };
        return this.writeStorage(data);
    }

    clearTabs(databaseName: string): StorageWriteResult {
        const data = this.readStorage();
        delete data.databases[databaseName];
        return this.writeStorage(data);
    }
}

const QUERY_TABS = "queryTabs";

interface QueryTabsData {
    tabs: PersistedQueryTab[];
    selectedTabIndex: number;
}

const INITIAL_QUERY_TABS: QueryTabsData = {
    tabs: [],
    selectedTabIndex: 0,
};

function parseQueryTabsData(obj: Object | null): QueryTabsData {
    return Object.assign({}, INITIAL_QUERY_TABS, obj) as QueryTabsData;
}

class QueryTabs {
    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.readStorage() == null) {
            this.writeStorage(INITIAL_QUERY_TABS);
        }
    }

    private readStorage(): QueryTabsData {
        if (!this.storage.isAccessible) return INITIAL_QUERY_TABS;
        return this.storage.read<QueryTabsData>(QUERY_TABS, parseQueryTabsData);
    }

    private writeStorage(data: QueryTabsData): StorageWriteResult {
        return this.storage.write(QUERY_TABS, data);
    }

    getTabs(): { tabs: PersistedQueryTab[]; selectedTabIndex: number } {
        return this.readStorage();
    }

    setTabs(tabs: PersistedQueryTab[], selectedTabIndex: number): StorageWriteResult {
        return this.writeStorage({ tabs, selectedTabIndex });
    }
}

const CHAT_CONVERSATIONS = "chatConversations";

export interface PersistedChatMessage {
    id: string;
    sender: 'user' | 'ai';
    timestamp: string;
    content: Array<{
        type: 'text' | 'code';
        content?: string;
        language?: string;
    }>;
}

export interface PersistedConversation {
    id: string;
    title: string;
    createdAt: string;
    updatedAt: string;
    messages: PersistedChatMessage[];
}

function truncateTitle(text: string, maxLen = 50): string {
    const firstLine = text.split('\n')[0].trim();
    if (firstLine.length <= maxLen) return firstLine || 'New conversation';
    return firstLine.substring(0, maxLen - 3) + '...';
}

interface ChatConversationsData {
    version: 2;
    databases: {
        [databaseName: string]: {
            conversations: PersistedConversation[];
            selectedConversationId: string | null;
        };
    };
}

const INITIAL_CHAT_CONVERSATIONS: ChatConversationsData = {
    version: 2,
    databases: {},
};

function parseChatConversationsData(obj: Object | null): ChatConversationsData {
    if (obj == null) return INITIAL_CHAT_CONVERSATIONS;

    const data = obj as any;
    if (!data.version) {
        // Migrate V1 -> V2
        const v2: ChatConversationsData = { version: 2, databases: {} };
        const v1Databases = data.databases as { [db: string]: { messages: PersistedChatMessage[] } } | undefined;
        for (const [dbName, dbData] of Object.entries(v1Databases || {})) {
            if (dbData.messages && dbData.messages.length > 0) {
                const convId = crypto.randomUUID();
                const firstUserMsg = dbData.messages.find(m => m.sender === 'user');
                const title = firstUserMsg
                    ? truncateTitle(firstUserMsg.content.find(p => p.type === 'text')?.content || 'Conversation')
                    : 'Conversation';
                v2.databases[dbName] = {
                    conversations: [{
                        id: convId,
                        title,
                        createdAt: dbData.messages[0]?.timestamp || new Date().toISOString(),
                        updatedAt: dbData.messages[dbData.messages.length - 1]?.timestamp || new Date().toISOString(),
                        messages: dbData.messages,
                    }],
                    selectedConversationId: convId,
                };
            }
        }
        return v2;
    }

    return Object.assign({}, INITIAL_CHAT_CONVERSATIONS, data) as ChatConversationsData;
}

class ChatConversations {
    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.readStorage() == null) {
            this.writeStorage(INITIAL_CHAT_CONVERSATIONS);
        }
    }

    private readStorage(): ChatConversationsData {
        if (!this.storage.isAccessible) return INITIAL_CHAT_CONVERSATIONS;
        return this.storage.read<ChatConversationsData>(CHAT_CONVERSATIONS, parseChatConversationsData);
    }

    private writeStorage(data: ChatConversationsData): StorageWriteResult {
        return this.storage.write(CHAT_CONVERSATIONS, data);
    }

    getConversationList(databaseName: string): PersistedConversation[] {
        const data = this.readStorage();
        return data.databases[databaseName]?.conversations || [];
    }

    getSelectedConversationId(databaseName: string): string | null {
        const data = this.readStorage();
        return data.databases[databaseName]?.selectedConversationId || null;
    }

    getConversation(databaseName: string, conversationId: string): PersistedConversation | null {
        const data = this.readStorage();
        const dbData = data.databases[databaseName];
        return dbData?.conversations.find(c => c.id === conversationId) || null;
    }

    setConversation(databaseName: string, conversation: PersistedConversation): StorageWriteResult {
        const data = this.readStorage();
        if (!data.databases[databaseName]) {
            data.databases[databaseName] = { conversations: [], selectedConversationId: null };
        }
        const idx = data.databases[databaseName].conversations.findIndex(c => c.id === conversation.id);
        if (idx >= 0) {
            data.databases[databaseName].conversations[idx] = conversation;
        } else {
            data.databases[databaseName].conversations.unshift(conversation);
        }
        return this.writeStorage(data);
    }

    setSelectedConversationId(databaseName: string, conversationId: string | null): StorageWriteResult {
        const data = this.readStorage();
        if (!data.databases[databaseName]) {
            data.databases[databaseName] = { conversations: [], selectedConversationId: null };
        }
        data.databases[databaseName].selectedConversationId = conversationId;
        return this.writeStorage(data);
    }

    deleteConversation(databaseName: string, conversationId: string): StorageWriteResult {
        const data = this.readStorage();
        const dbData = data.databases[databaseName];
        if (!dbData) return this.writeStorage(data);
        dbData.conversations = dbData.conversations.filter(c => c.id !== conversationId);
        if (dbData.selectedConversationId === conversationId) {
            dbData.selectedConversationId = dbData.conversations[0]?.id || null;
        }
        return this.writeStorage(data);
    }

    renameConversation(databaseName: string, conversationId: string, title: string): StorageWriteResult {
        const data = this.readStorage();
        const conv = data.databases[databaseName]?.conversations.find(c => c.id === conversationId);
        if (conv) conv.title = title;
        return this.writeStorage(data);
    }

    clearDatabase(databaseName: string): StorageWriteResult {
        const data = this.readStorage();
        delete data.databases[databaseName];
        return this.writeStorage(data);
    }
}

function parsePreferencesData(obj: Object | null): PreferencesData {
    return Object.assign({}, INITIAL_PREFERENCES, obj) as PreferencesData;
}

const INITIAL_PREFERENCES: PreferencesData = {
    connections: {
        showAdvancedConfigByDefault: true,
    },
    transactionMode: "auto",
    queryRowLimit: 100,
};

class Preferences {

    constructor(private storage: StorageService) {
        if (this.storage.isAccessible && this.readStorage() == null) {
            this.writeStorage(INITIAL_PREFERENCES);
        }
    }

    private readStorage(): PreferencesData {
        return this.storage.read(PREFERENCES, parsePreferencesData);
    }

    private writeStorage(prefs: PreferencesData): StorageWriteResult {
        return this.storage.write(PREFERENCES, prefs);
    }

    readonly connection = {
        showAdvancedConfigByDefault: () => {
            const prefs = this.readStorage();
            return prefs?.connections.showAdvancedConfigByDefault || INITIAL_PREFERENCES.connections.showAdvancedConfigByDefault;
        },
        setShowAdvancedConfigByDefault: (value: boolean) => {
            const prefs = this.readStorage()!;
            prefs.connections.showAdvancedConfigByDefault = value;
            return this.writeStorage(prefs);
        },
    };

    transactionMode(): OperationMode {
        const prefs = this.readStorage();
        return prefs?.transactionMode ?? INITIAL_PREFERENCES.transactionMode;
    }

    setTransactionMode(value: OperationMode): StorageWriteResult {
        const prefs = this.readStorage()!;
        prefs.transactionMode = value;
        return this.writeStorage(prefs);
    }

    queryRowLimit(): RowLimit {
        const prefs = this.readStorage();
        return prefs?.queryRowLimit ?? INITIAL_PREFERENCES.queryRowLimit;
    }

    setQueryRowLimit(value: RowLimit): StorageWriteResult {
        const prefs = this.readStorage()!;
        prefs.queryRowLimit = value;
        return this.writeStorage(prefs);
    }
}

@Injectable({
    providedIn: "root",
})
export class AppData {

    readonly isAccessible = this.storage.isAccessible;

    readonly viewState = new ViewState(this.storage);
    readonly connections = new Connections(this.storage);
    readonly preferences = new Preferences(this.storage);
    readonly dataExplorerTabs = new DataExplorerTabs(this.storage);
    readonly queryTabs = new QueryTabs(this.storage);
    readonly chatConversations = new ChatConversations(this.storage);

    constructor(private storage: StorageService) {
    }
}
