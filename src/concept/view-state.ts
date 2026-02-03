/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { SchemaTreeLinkNode } from "../service/schema-tool-window-state.service";

export const sidebarStates = ["expanded", "collapsed"] as const;
export type SidebarState = typeof sidebarStates[number];

export const tools = ["query", "schema", "data", "chat"] as const;
export type Tool = typeof tools[number];

export interface SchemaToolWindowState {
    linksVisibility: Record<SchemaTreeLinkNode["linkKind"], boolean>;
    viewMode: "flat" | "hierarchical";
    rootNodesCollapsed: {
        entities: boolean;
        relations: boolean;
        attributes: boolean;
    };
}
