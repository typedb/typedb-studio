/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Routes } from "@angular/router";

import { _404PageComponent } from "../module/404/404-page.component";
import { ConnectionCreatorComponent } from "../module/connection/create/connection-creator.component";
import { HomeComponent } from "../module/home/home.component";
import { QueryToolComponent } from "../module/query/query-tool.component";

export const routes: Routes = [
    { path: "", component: HomeComponent, title: "Home", pathMatch: "full" },
    { path: "connect", component: ConnectionCreatorComponent, title: "Connect" },
    { path: "query", component: QueryToolComponent, title: "Query" },
    { path: "**", component: _404PageComponent, title: "404" },
];
