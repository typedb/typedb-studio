/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

export const sidebarStates = ["expanded", "collapsed"] as const;
export type SidebarState = typeof sidebarStates[number];

export const tools = ["query", "explore", "schema"] as const;
export type Tool = typeof tools[number];
